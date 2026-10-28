package com.fzu.wuyu.domain

import com.fzu.wuyu.data.catalog.SubcategoryCatalog
import com.fzu.wuyu.domain.model.BasicEvaluationInfo
import com.fzu.wuyu.domain.model.CapType
import com.fzu.wuyu.domain.model.Category
import com.fzu.wuyu.domain.model.DeyuRecord
import com.fzu.wuyu.domain.model.Subcategory

/** 单个门类的进度 */
data class SubcategoryProgress(
    val subcategory: Subcategory,
    val raw: Float,              // 本学期原始累计
    val counted: Float,          // 本学期实际计入分（封顶后）
    val cap: Float?,             // 规则上限（null = 无上限）
    val effectiveCap: Float?,    // 本学期实际上限（绝对上限门类会扣除往学期已得分）
    val remaining: Float?,       // 距离 effectiveCap 还差多少（null = 无上限）
    val capped: Boolean          // 是否已触顶
)

/** 一个大类的汇总 */
data class CategorySummary(
    val category: Category,
    val subcategories: List<SubcategoryProgress>,
    val developmentScore: Float,   // 发展性得分 = min(各门类计入分之和, 30)；S 素拓为「加分 − 减分」净分
    val developmentRaw: Float,     // 封顶前的合计；S 素拓为加分合计
    val developmentCap: Float,     // 30；S 素拓为 0（不封顶、不参与综测）
    val basicInfo: BasicEvaluationInfo?,
    /** 仅 S 素拓有意义：加分项合计 */
    val sutuoAdd: Float = 0f,
    /** 仅 S 素拓有意义：减分项合计（正数存放） */
    val sutuoDeduct: Float = 0f
) {
    /** S 素拓净分 = 加分 − 减分 */
    val sutuoNet: Float get() = sutuoAdd - sutuoDeduct
}

object ScoringEngine {

    /**
     * 计算某门类在某学期的进度。
     * @param semesterRecords 本学期记录
     * @param historyRecords  往学期记录（仅 ABSOLUTE 上限门类需要，用于跨学期累计封顶）
     */
    fun subcategoryProgress(
        subcategory: Subcategory,
        semesterRecords: List<DeyuRecord>,
        historyRecords: List<DeyuRecord> = emptyList()
    ): SubcategoryProgress {
        val raw = semesterRecords
            .filter { it.subcategoryId == subcategory.id }
            .sumOf { it.score.toDouble() }.toFloat()

        val cap = subcategory.capValue
        if (cap == null) {
            return SubcategoryProgress(subcategory, raw, raw, null, null, null, false)
        }

        // 绝对上限（如志愿服务星级27分）：往学期已得分要扣除
        val previousCounted = if (subcategory.capType == CapType.ABSOLUTE) {
            historyRecords
                .filter { it.subcategoryId == subcategory.id }
                .sumOf { it.score.toDouble() }.toFloat()
                .coerceAtMost(cap)
        } else 0f

        val effectiveCap = (cap - previousCounted).coerceAtLeast(0f)
        val counted = minOf(raw, effectiveCap)
        return SubcategoryProgress(
            subcategory = subcategory,
            raw = raw,
            counted = counted,
            cap = cap,
            effectiveCap = effectiveCap,
            remaining = (effectiveCap - counted).coerceAtLeast(0f),
            capped = counted >= effectiveCap
        )
    }

    /** 某大类在某学期的汇总 */
    fun categorySummary(
        category: Category,
        semesterRecords: List<DeyuRecord>,
        historyRecords: List<DeyuRecord> = emptyList()
    ): CategorySummary {
        val subs = SubcategoryCatalog.byCategory(category)
        val progresses = subs.map { subcategoryProgress(it, semesterRecords, historyRecords) }

        // S 素拓：独立体系，加分项累加、减分项扣减，不受发展性 30 分上限约束
        if (category.isSutuo) {
            val add = progresses.filter { !it.subcategory.deduct }
                .sumOf { it.counted.toDouble() }.toFloat()
            val deduct = progresses.filter { it.subcategory.deduct }
                .sumOf { it.counted.toDouble() }.toFloat()
            return CategorySummary(
                category = category,
                subcategories = progresses,
                developmentScore = add - deduct,
                developmentRaw = add,
                developmentCap = 0f,
                basicInfo = null,
                sutuoAdd = add,
                sutuoDeduct = deduct
            )
        }

        val raw = progresses.sumOf { it.counted.toDouble() }.toFloat()
        // F 负面评价：纯减分，不占发展性 30 分上限，developmentCap = 0 表示无上限
        val capped = if (category.isDeduction) raw else minOf(raw, SubcategoryCatalog.DEVELOPMENT_CAP)
        return CategorySummary(
            category = category,
            subcategories = progresses,
            developmentScore = capped,
            developmentRaw = raw,
            developmentCap = if (category.isDeduction) 0f else SubcategoryCatalog.DEVELOPMENT_CAP,
            basicInfo = SubcategoryCatalog.basicInfoOf(category)
        )
    }

    /** 所有大类的汇总（总览页用），含 S 素拓 */
    fun overview(
        semesterRecords: List<DeyuRecord>,
        historyRecords: List<DeyuRecord> = emptyList()
    ): List<CategorySummary> =
        Category.entries.map { categorySummary(it, semesterRecords, historyRecords) }

    /** 五育大类汇总（A–F，不含 S 素拓） */
    fun wuyuOverview(
        semesterRecords: List<DeyuRecord>,
        historyRecords: List<DeyuRecord> = emptyList()
    ): List<CategorySummary> =
        Category.wuyuEntries.map { categorySummary(it, semesterRecords, historyRecords) }
}
