package com.fzu.wuyu.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fzu.wuyu.data.catalog.SubcategoryCatalog
import com.fzu.wuyu.domain.CategorySummary
import com.fzu.wuyu.domain.model.Category
import com.fzu.wuyu.domain.model.DeyuRecord
import com.fzu.wuyu.ui.DeyuUiState
import com.fzu.wuyu.ui.DeyuViewModel
import com.fzu.wuyu.ui.components.SectionTitleWithHint
import com.fzu.wuyu.ui.components.SimpleDropdown
import com.fzu.wuyu.ui.components.SortSwitchRow
import com.fzu.wuyu.ui.components.fmtScore
import com.fzu.wuyu.ui.components.sortedByActivityDate
import com.fzu.wuyu.util.DateUtils

/**
 * 总览页（默认启动页）：
 * ① 本学期总五育 ② 总素拓 ③ 未分类记录 ④ 多学期累计总五育 ⑤ 五育对综测的加分。
 * 学期设置 / 外观 / 导出导入 / 建议反馈已移至底栏第一层「设置」页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    viewModel: DeyuViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val sortDescending by viewModel.sortDescending.collectAsState()
    var classifyTarget by remember { mutableStateOf<DeyuRecord?>(null) }

    LazyColumn(
        modifier = modifier,
        // 底部留白覆盖双层悬浮底栏（两层 56dp + 间距 + 留边 ≈ 150dp）
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            state.loading -> item {
                Text("加载中…", modifier = Modifier.padding(vertical = 24.dp))
            }

            state.semesters.isEmpty() -> item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("还没有学期数据")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "请先到底栏「设置」页创建学期",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                item { SemesterScoreCard(state) }
                item { SutuoCard(state) }
                if (state.unknownRecords.isNotEmpty()) {
                    item {
                        UnknownRecordsCard(
                            records = state.unknownRecords.sortedByActivityDate(sortDescending),
                            semesterNames = state.semesters.associate { it.id to it.name },
                            sortDescending = sortDescending,
                            onSortChange = viewModel::setSortDescending,
                            onClassify = { classifyTarget = it },
                            onDelete = viewModel::deleteRecord
                        )
                    }
                }
                item { CumulativeScoreCard(state) }
                item { WuyuBonusCard(state) }
            }
        }
    }

    classifyTarget?.let { record ->
        ClassifyRecordDialog(
            record = record,
            onDismiss = { classifyTarget = null },
            onConfirm = { subcategoryId ->
                viewModel.updateRecord(record.copy(subcategoryId = subcategoryId))
                classifyTarget = null
            }
        )
    }
}

// ---------- ① 本学期总五育 ----------

@Composable
private fun SemesterScoreCard(state: DeyuUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitleWithHint(
                title = "本学期总五育",
                hint = "基础性测评（每类上限 ${SubcategoryCatalog.BASIC_CAP.toInt()} 分）按班级互评、" +
                    "辅导员评价等规则评定，本 App 不记录不计分；F 负面评价见「减分」页签。"
            )
            Text(
                "${fmtScore(state.semesterScore)} / ${state.semesterCap.toInt()} 分",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "五育发展性测评 · ${state.selectedSemester?.name ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = {
                    if (state.semesterCap > 0) (state.semesterScore / state.semesterCap)
                        .coerceIn(0f, 1f) else 0f
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            // 圆角统计块之间留间距，避免大圆角块零间距紧贴看起来像重叠
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 只列 A–E 五育：F 负面评价单独在「减分」页，S 素拓单独一个卡
                state.summaries
                    .filter { !it.category.isDeduction && !it.category.isSutuo }
                    .forEach { summary -> CategoryScoreRow(summary) }
            }
        }
    }
}

// ---------- ② 本学期总素拓 ----------

/**
 * 总素拓卡：素拓（S）是独立于五育的体系，加分项累加、减分项扣减，不封顶、不计入综测，
 * 主要用于各类评优评先、推优入党、学生干部选拔等考核。
 */
@Composable
private fun SutuoCard(state: DeyuUiState) {
    val sutuo = state.sutuo
    val add = sutuo?.sutuoAdd ?: 0f
    val deduct = sutuo?.sutuoDeduct ?: 0f
    val net = sutuo?.sutuoNet ?: 0f
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitleWithHint(
                title = "总素拓",
                hint = "素质拓展分（S）独立于五育，不计入综测成绩，也不受每类 30 分上限约束；" +
                    "用于各类评优评先、推优入党、学生干部选拔等考核。" +
                    "加分项：文化/艺术/科技/实践类活动、体育竞赛类活动、招募观众类、场务类、" +
                    "现场展示类、志愿服务（年度满25小时6分）、广播站、特别贡献；" +
                    "减分项：违纪处理处分、课堂秩序、日常秩序（分值与五育 F 类不同）。"
            )
            Text(
                "${fmtScore(net)} 分",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (net < 0f) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            Text(
                "素拓加分 ${fmtScore(add)} − 减分 ${fmtScore(deduct)} · " +
                    "${state.selectedSemester?.name ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            Text(
                "多学期累计 ${fmtScore(state.sutuoCumulative)} 分",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            if (state.sutuoSemesterTotals.isEmpty()) {
                Text("暂无学期数据")
            } else {
                state.sutuoSemesterTotals.forEach { total ->
                    val current = total.semester.id == state.selectedSemesterId
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            total.semester.name + if (current) "（当前）" else "",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            "${fmtScore(total.score)} 分",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (total.score < 0f) MaterialTheme.colorScheme.error
                            else Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryScoreRow(summary: CategorySummary) {
    val fraction = if (summary.developmentCap > 0) {
        (summary.developmentScore / summary.developmentCap).coerceIn(0f, 1f)
    } else 0f
    // KernelSU 式圆角统计块：字母 + 分数 + 细进度条
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${summary.category.code} ${summary.category.displayName}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${fmtScore(summary.developmentScore)} / ${summary.developmentCap.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------- ④ 多学期累计总五育 ----------

@Composable
private fun CumulativeScoreCard(state: DeyuUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitleWithHint(
                title = "多学期累计总德育",
                hint = "各学期的五育发展性得分直接相加（各学期内仍按每类 30 分封顶）。"
            )
            Text(
                "${fmtScore(state.cumulativeScore)} 分",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "共 ${state.semesterTotals.size} 个学期合计",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            state.semesterTotals.forEach { total ->
                val current = total.semester.id == state.selectedSemesterId
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        total.semester.name + if (current) "（当前）" else "",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
                    )
                    Text("${fmtScore(total.score)} 分", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ---------- ⑤ 五育对综测的加分 ----------

@Composable
private fun WuyuBonusCard(state: DeyuUiState) {
    val semesterNames = remember(state.semesters) {
        state.semesters.associate { it.id to it.name }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitleWithHint(
                title = "五育对综测的加分",
                hint = "五育（A–E）发展性得分按权重换算后计入综测成绩的加分，多学期按平均计入。" +
                    "计入分 = 发展性得分 × 权重：A 德育 ×6% + B 智育 ×5% + C 体育 ×3% + " +
                    "D 美育 ×3% + E 劳育 ×3%。不含基础性测评；学业成绩与 F 负面评价不参与本卡计算。"
            )
            Text(
                "+${fmtScore(state.wuyuBonusAverage ?: 0f)} 分",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "共 ${state.zongceBreakdowns.size} 个学期 · 按平均计入",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            if (state.zongceBreakdowns.isEmpty()) {
                Text("暂无学期数据")
            } else {
                state.zongceBreakdowns.forEach { breakdown ->
                    val name = semesterNames[breakdown.semesterId] ?: "（已删除学期）"
                    val current = breakdown.semesterId == state.selectedSemesterId
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            name + if (current) "（当前）" else "",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            "+${fmtScore(breakdown.wuyuPart)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// ---------- 未分类记录 ----------

/**
 * 未分类记录卡：添加时选“?”的记录会汇总在这里，不计入五育，可随时归类或删除。
 * 展示全部学期的未分类记录（换学期也不会漏），每条标注所属学期。
 */
@Composable
private fun UnknownRecordsCard(
    records: List<DeyuRecord>,
    semesterNames: Map<Long, String>,
    sortDescending: Boolean,
    onSortChange: (Boolean) -> Unit,
    onClassify: (DeyuRecord) -> Unit,
    onDelete: (DeyuRecord) -> Unit
) {
    val total = records.sumOf { it.score.toDouble() }.toFloat()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitleWithHint(
                title = "未分类记录",
                hint = "未分类记录暂不计入五育发展性得分，归类后立即参与计分。"
            )
            Text(
                "共 ${records.size} 条，合计 ${fmtScore(total)} 分",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            SortSwitchRow(sortDescending = sortDescending, onChange = onSortChange)
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            records.forEach { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(record.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${DateUtils.format(record.dateEpochDay)} · +${fmtScore(record.score)} 分 · " +
                                (semesterNames[record.semesterId] ?: "（已删除学期）") +
                                if (record.note.isNotBlank()) " · ${record.note}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onClassify(record) }) { Text("归类") }
                    IconButton(onClick = { onDelete(record) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/** 归类对话框：选大类（A–F + S 七个 chip）+ 选门类，确定后把记录移入该门类 */
@Composable
private fun ClassifyRecordDialog(
    record: DeyuRecord,
    onDismiss: () -> Unit,
    onConfirm: (subcategoryId: String) -> Unit
) {
    var category by remember { mutableStateOf<Category?>(null) }
    var subcategoryId by remember { mutableStateOf("") }
    val recordable = category?.let { c ->
        SubcategoryCatalog.byCategory(c).filter { it.recordable }
    } ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("归类记录") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "「${record.title}」（${DateUtils.format(record.dateEpochDay)} +${fmtScore(record.score)} 分）",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "选择要归入的大类与门类：",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Category.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                subcategoryId = SubcategoryCatalog.byCategory(cat)
                                    .filter { it.recordable }.firstOrNull()?.id ?: ""
                            },
                            label = { Text(cat.code, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                if (category == null) {
                    Text(
                        "请先选择大类（A–F 五育 / S 素拓）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    SimpleDropdown(
                        label = "门类",
                        items = recordable.map { it.name },
                        selectedIndex = recordable.indexOfFirst { it.id == subcategoryId }
                            .coerceAtLeast(0),
                        onSelect = { index -> subcategoryId = recordable[index].id }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(subcategoryId) },
                enabled = subcategoryId.isNotBlank()
            ) { Text("归类") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
