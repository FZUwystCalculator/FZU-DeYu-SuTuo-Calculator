package com.fzu.wuyu.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fzu.wuyu.data.DeyuRepository
import com.fzu.wuyu.data.ThemePrefs
import com.fzu.wuyu.data.backup.BackupManager
import com.fzu.wuyu.data.backup.ImportResult
import com.fzu.wuyu.data.catalog.SubcategoryCatalog
import com.fzu.wuyu.data.local.DeyuDatabase
import com.fzu.wuyu.domain.CategorySummary
import com.fzu.wuyu.domain.ScoringEngine
import com.fzu.wuyu.domain.ZongceBreakdown
import com.fzu.wuyu.domain.ZongceEngine
import com.fzu.wuyu.domain.model.AssessmentBackup
import com.fzu.wuyu.domain.model.BackupData
import com.fzu.wuyu.domain.model.Category
import com.fzu.wuyu.domain.model.DeyuRecord
import com.fzu.wuyu.domain.model.DeyuSemester
import com.fzu.wuyu.domain.model.SemesterAssessment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 单个学期的发展性德育总分 */
data class SemesterTotal(
    val semester: DeyuSemester,
    val score: Float
)

data class DeyuUiState(
    val loading: Boolean = true,
    val semesters: List<DeyuSemester> = emptyList(),
    val selectedSemesterId: Long? = null,
    val records: List<DeyuRecord> = emptyList(),
    val historyRecords: List<DeyuRecord> = emptyList(),
    val summaries: List<CategorySummary> = emptyList(),
    /** 各学期五育发展性总分（按创建顺序） */
    val semesterTotals: List<SemesterTotal> = emptyList(),
    /** 多学期累计五育发展性总分 */
    val cumulativeScore: Float = 0f,
    /** 各学期综测参数（semesterId → 绩点 + 五育基础性得分） */
    val assessments: Map<Long, SemesterAssessment> = emptyMap(),
    /** 各学期综测成绩拆解（按创建顺序） */
    val zongceBreakdowns: List<ZongceBreakdown> = emptyList(),
    /** 各学期总素拓（加分 − 减分，按创建顺序） */
    val sutuoSemesterTotals: List<SemesterTotal> = emptyList(),
    /** 多学期累计总素拓 */
    val sutuoCumulative: Float = 0f
) {
    val selectedSemester: DeyuSemester?
        get() = semesters.firstOrNull { it.id == selectedSemesterId }

    fun summaryOf(category: Category): CategorySummary? =
        summaries.firstOrNull { it.category == category }

    /** 本学期素拓汇总（S） */
    val sutuo: CategorySummary?
        get() = summaries.firstOrNull { it.category.isSutuo }

    /** 全部记录（当前学期 + 往学期） */
    val allRecords: List<DeyuRecord> get() = records + historyRecords

    /** 本学期五育发展性总分（A-E 合计，上限 150；不含 F 负面评价与 S 素拓） */
    val semesterScore: Float
        get() = summaries.filter { !it.category.isDeduction && !it.category.isSutuo }
            .sumOf { it.developmentScore.toDouble() }.toFloat()

    /** 本学期五育发展性总分上限（A-E 各类 30 分） */
    val semesterCap: Float
        get() = summaries.count { !it.category.isDeduction && !it.category.isSutuo } *
            SubcategoryCatalog.DEVELOPMENT_CAP

    /** 本学期 F 负面评价合计（正数存放） */
    val deductionScore: Float
        get() = summaries.firstOrNull { it.category.isDeduction }?.developmentScore ?: 0f

    /** 当前学期的综测参数（无记录时返回默认值：基础性 70） */
    fun assessmentOf(semesterId: Long?): SemesterAssessment =
        semesterId?.let { assessments[it] } ?: SemesterAssessment(semesterId ?: 0)

    /** 当前学期的综测拆解 */
    val selectedZongce: ZongceBreakdown?
        get() = zongceBreakdowns.firstOrNull { it.semesterId == selectedSemesterId }

    /** 全部学期的未分类记录（subcategoryId = "unknown"） */
    val unknownRecords: List<DeyuRecord>
        get() = allRecords.filter { it.subcategoryId == SubcategoryCatalog.UNKNOWN_SUBCATEGORY_ID }

    /**
     * 五育对综测的加权加分（多学期平均口径）。
     * 与官方"平均综测成绩 = Σ各学期综测成绩 / 学期数"一致：
     * 取各学期 Σ(发展性得分 × 权重) 的平均值。不含基础性测评，
     * 学业成绩（×80%）不在本 App 计算范围。
     */
    val wuyuBonusAverage: Float?
        get() {
            val parts = zongceBreakdowns.map { it.wuyuPart }
            return if (parts.isEmpty()) null else parts.sum() / parts.size
        }
}

class DeyuViewModel(
    private val repo: DeyuRepository,
    private val prefs: ThemePrefs
) : ViewModel() {

    private val _selectedSemesterId = MutableStateFlow<Long?>(null)

    /** 记录排序方向：true = 按活动时间倒序（新→旧） */
    private val _sortDescending = MutableStateFlow(prefs.recordSortDescending)
    val sortDescending: StateFlow<Boolean> = _sortDescending.asStateFlow()

    /** 记录页是否按类别分层展示（A–F、S） */
    private val _groupByCategory = MutableStateFlow(prefs.recordsGroupByCategory)
    val groupByCategory: StateFlow<Boolean> = _groupByCategory.asStateFlow()

    val uiState: StateFlow<DeyuUiState> = combine(
        repo.observeSemesters(),
        repo.observeAllRecords(),
        repo.observeAssessments(),
        _selectedSemesterId
    ) { semesters, allRecords, assessments, selectedId ->
        val id = selectedId ?: semesters.firstOrNull()?.id
        if (id == null) {
            DeyuUiState(loading = false)
        } else {
            val semesterRecords = allRecords.filter { it.semesterId == id }
            val history = allRecords.filter { it.semesterId != id }
            val assessmentMap = assessments.associateBy { it.semesterId }

            // 多学期累计：按创建顺序逐学期结算，
            // 保证 ABSOLUTE 上限门类（如志愿服务星级 27 分）跨学期正确封顶
            val totals = ArrayList<SemesterTotal>()
            val breakdowns = ArrayList<ZongceBreakdown>()
            val sutuoTotals = ArrayList<SemesterTotal>()
            val seen = ArrayList<DeyuRecord>()
            var cumulative = 0f
            var sutuoCumulative = 0f
            semesters.sortedBy { it.createdAt }.forEach { s ->
                val recs = allRecords.filter { it.semesterId == s.id }
                val allSummaries = ScoringEngine.overview(recs, seen)
                val wuyu = allSummaries.filter { !it.category.isDeduction && !it.category.isSutuo }
                val score = wuyu.sumOf { it.developmentScore.toDouble() }.toFloat()
                totals += SemesterTotal(s, score)
                cumulative += score
                val deduction = allSummaries
                    .firstOrNull { it.category.isDeduction }?.developmentScore ?: 0f
                breakdowns += ZongceEngine.compute(
                    semesterId = s.id,
                    devScores = wuyu.associate { it.category to it.developmentScore },
                    deduction = deduction
                )
                // 素拓净分逐学期累加（含 ABSOLUTE 上限门类的跨学期扣减）
                val sutuoNet = allSummaries
                    .firstOrNull { it.category.isSutuo }?.sutuoNet ?: 0f
                sutuoTotals += SemesterTotal(s, sutuoNet)
                sutuoCumulative += sutuoNet
                seen += recs
            }

            DeyuUiState(
                loading = false,
                semesters = semesters,
                selectedSemesterId = id,
                records = semesterRecords,
                historyRecords = history,
                summaries = ScoringEngine.overview(semesterRecords, history),
                semesterTotals = totals,
                cumulativeScore = cumulative,
                assessments = assessmentMap,
                zongceBreakdowns = breakdowns,
                sutuoSemesterTotals = sutuoTotals,
                sutuoCumulative = sutuoCumulative
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeyuUiState())

    init {
        viewModelScope.launch { repo.ensureDefaultSemester() }
    }

    // ---------- 学期 ----------

    fun selectSemester(id: Long) {
        _selectedSemesterId.value = id
    }

    fun addSemester(name: String, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.addSemester(name)
            _selectedSemesterId.value = id
            onDone(id)
        }
    }

    fun renameSemester(id: Long, name: String) {
        viewModelScope.launch { repo.renameSemester(id, name) }
    }

    fun deleteSemester(id: Long) {
        viewModelScope.launch {
            repo.deleteSemester(id)
            // 删的是当前学期时重置选中，让 combine 自动落到剩余第一个学期
            if (_selectedSemesterId.value == id) _selectedSemesterId.value = null
        }
    }

    // ---------- 记录 ----------

    fun addRecord(record: DeyuRecord) {
        viewModelScope.launch { repo.addRecord(record) }
    }

    fun updateRecord(record: DeyuRecord) {
        viewModelScope.launch { repo.updateRecord(record) }
    }

    fun deleteRecord(record: DeyuRecord) {
        viewModelScope.launch { repo.deleteRecord(record) }
    }

    /** 切换记录排序方向（按活动时间正序/倒序），本地持久化 */
    fun setSortDescending(descending: Boolean) {
        prefs.recordSortDescending = descending
        _sortDescending.value = descending
    }

    /** 切换记录页「按类别」分组，本地持久化 */
    fun setGroupByCategory(grouped: Boolean) {
        prefs.recordsGroupByCategory = grouped
        _groupByCategory.value = grouped
    }

    // ---------- 综测参数 ----------

    /** 保存某学期的综测参数（绩点 + 五育基础性得分） */
    fun setAssessment(semesterId: Long, gpa: Float?, basics: Map<Category, Float>) {
        viewModelScope.launch {
            var assessment = uiState.value.assessmentOf(semesterId).copy(
                semesterId = semesterId,
                gpa = gpa
            )
            basics.forEach { (category, value) -> assessment = assessment.withBasic(category, value) }
            repo.saveAssessment(assessment)
        }
    }

    // ---------- 导入导出 ----------

    fun buildJsonBackup(): BackupData {
        val state = uiState.value
        val nameById = state.semesters.associate { it.id to it.name }
        return BackupData(
            exportedAt = System.currentTimeMillis(),
            semesters = state.semesters,
            records = state.allRecords,
            assessments = state.assessments.values.map { a ->
                AssessmentBackup(
                    semesterName = nameById[a.semesterId] ?: "",
                    gpa = a.gpa,
                    basicA = a.basicA,
                    basicB = a.basicB,
                    basicC = a.basicC,
                    basicD = a.basicD,
                    basicE = a.basicE
                )
            }
        )
    }

    fun exportJsonText(): String = BackupManager.exportJson(buildJsonBackup())

    fun exportMarkdownText(): String = BackupManager.exportMarkdown(
        semesters = uiState.value.semesters,
        allRecords = uiState.value.allRecords
    )

    suspend fun importBackup(text: String): ImportResult =
        when (val result = BackupManager.parseJson(text)) {
            is ImportResult.Failure -> result
            is ImportResult.Success -> {
                repo.replaceAll(result.data.semesters, result.data.records, result.data.assessments)
                _selectedSemesterId.value = result.data.semesters.firstOrNull()?.id
                result
            }
        }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DeyuViewModel(
                    DeyuRepository(DeyuDatabase.get(context)),
                    ThemePrefs(context)
                )
            }
        }
    }
}