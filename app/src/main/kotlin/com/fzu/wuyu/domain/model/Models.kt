package com.fzu.wuyu.domain.model

import kotlinx.serialization.Serializable

/**
 * 综合素质评价大类：A-E 五育（加分）+ F 负面评价（减分）+ S 素质拓展分。
 *
 * - F 类得分直接以 100% 扣减综测成绩，不占五育发展性 30 分上限。
 * - S 素拓是**独立体系**：不参与综测计分、不占 30 分上限，按单次分值累计，
 *   用于各类评优评先、推优入党、学生干部选拔等考核。素拓自身也分加分项与减分项
 *   （减分项以 [Subcategory.deduct] 标记，分值与五育 F 完全不同）。
 */
enum class Category(
    val code: String,
    val displayName: String,
    val isDeduction: Boolean = false,
    val isSutuo: Boolean = false
) {
    A("A", "德育"),
    B("B", "智育"),
    C("C", "体育"),
    D("D", "美育"),
    E("E", "劳育"),
    F("F", "负面评价", isDeduction = true),
    S("S", "素拓", isSutuo = true);

    companion object {
        fun fromCode(code: String): Category? = entries.firstOrNull { it.code == code }

        /** 参与五育计分的大类（A–F）；S 素拓走独立体系 */
        val wuyuEntries: List<Category> get() = entries.filter { !it.isSutuo }
    }
}

/**
 * 门类计分方式：
 * - PER_TIME  按次：每次固定分（如思想品德活动 0.4 分/次）
 * - PER_DAY   按天：每天固定分（如寒暑假社会实践 0.4 分/天）
 * - TIERED    分档：下拉选择级别/等第，自动填分（如荣誉 国家级15）
 * - FIXED     固定选项：下拉选择一项，每项固定分（如计算机等级 四级4分）
 * - MANUAL    手动：直接输入分数（如"其他未涵盖项"学院酌情）
 * - INFO_ONLY 仅提示：只展示计算方式，不记录不计分（如基础性测评、社会工作）
 */
enum class ScoringType {
    PER_TIME, PER_DAY, TIERED, FIXED, MANUAL, INFO_ONLY
}

/**
 * 门类上限类型：
 * - PER_SEMESTER 每学期封顶（记录分按学期累计后封顶）
 * - ABSOLUTE     绝对封顶（跨所有学期累计后封顶，如志愿服务星级上限27分）
 * - NONE         无上限（仍受发展性30分总上限约束）
 */
enum class CapType {
    PER_SEMESTER, ABSOLUTE, NONE
}

/** 分档/固定选项的一项 */
data class TierOption(
    val label: String,          // 完整展示名，如 "国家级"
    val score: Float,
    val group: String? = null,  // 分组（级别），非空时 UI 用两级下拉
    val rank: String? = null    // 等第，如 "一等奖"
)

/** 细分门类定义（来自《福州大学本科生综合素质评价参考细则》） */
data class Subcategory(
    val id: String,
    val category: Category,
    val name: String,
    val scoringType: ScoringType,
    val capType: CapType,
    val capValue: Float?,                 // 该项上限（null = 无上限）
    val unitScore: Float? = null,         // PER_TIME / PER_DAY 的每次/每天分值
    val options: List<TierOption> = emptyList(),
    val note: String? = null,             // 细则备注
    /** 素拓减分项：记录时分数仍以正数存放，汇总时从素拓加分中扣减 */
    val deduct: Boolean = false
) {
    val recordable: Boolean get() = scoringType != ScoringType.INFO_ONLY

    /** 该门类记录是否为扣分（五育 F 类或素拓减分项） */
    val isDeducting: Boolean get() = deduct || category.isDeduction
}

/** 基础性测评说明（不记录，仅展示计算方式） */
data class BasicEvaluationInfo(
    val category: Category,
    val formula: String
)

// ---------- 本地数据领域模型 ----------

@Serializable
data class DeyuRecord(
    val id: Long = 0,
    val semesterId: Long,
    val subcategoryId: String,
    val title: String,
    val score: Float,
    val dateEpochDay: Long,
    val note: String = "",
    val createdAt: Long = 0
)

@Serializable
data class DeyuSemester(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = 0
)

/**
 * 某学期的综测参数：学业成绩（平均学分绩点）与五育基础性得分。
 * 基础性得分达标默认 70 分，未达标填 50 分；仅用于综测计算，不影响记录统计。
 */
@Serializable
data class SemesterAssessment(
    val semesterId: Long = 0,
    val gpa: Float? = null,
    val basicA: Float = 70f,
    val basicB: Float = 70f,
    val basicC: Float = 70f,
    val basicD: Float = 70f,
    val basicE: Float = 70f
) {
    // S 素拓没有基础性测评，返回/保持不变
    fun basicOf(category: Category): Float = when (category) {
        Category.A -> basicA
        Category.B -> basicB
        Category.C -> basicC
        Category.D -> basicD
        Category.E -> basicE
        Category.F, Category.S -> 0f
    }

    fun withBasic(category: Category, value: Float): SemesterAssessment = when (category) {
        Category.A -> copy(basicA = value)
        Category.B -> copy(basicB = value)
        Category.C -> copy(basicC = value)
        Category.D -> copy(basicD = value)
        Category.E -> copy(basicE = value)
        Category.F, Category.S -> this
    }
}

/** 导出/导入的备份结构 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val appName: String = "FZU五育计算器",
    val exportedAt: Long = 0,
    val semesters: List<DeyuSemester> = emptyList(),
    val records: List<DeyuRecord> = emptyList(),
    /** 各学期综测参数（按学期名关联，导入时重新映射学期 id） */
    val assessments: List<AssessmentBackup> = emptyList()
)

/** 备份用的综测参数（按学期名关联） */
@Serializable
data class AssessmentBackup(
    val semesterName: String,
    val gpa: Float? = null,
    val basicA: Float = 70f,
    val basicB: Float = 70f,
    val basicC: Float = 70f,
    val basicD: Float = 70f,
    val basicE: Float = 70f
)
