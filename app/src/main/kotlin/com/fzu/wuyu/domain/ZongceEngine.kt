package com.fzu.wuyu.domain

import com.fzu.wuyu.domain.model.Category

/**
 * 一个学期的「五育对综测的加分」拆解。
 * 公式（依据《大一：五育、素拓全解析PPT》第 9/40 页，仅五育部分）：
 *   计入分 = 发展性得分 × 权重：A 德育 ×6% + B 智育 ×5% + C 体育 ×3% + D 美育 ×3% + E 劳育 ×3%
 * 多学期累计口径与官方"平均综测成绩 = Σ各学期综测成绩 / 学期数"一致，取各学期加分的平均值。
 * 注意：基础性测评（70 分）不参与计算；学业成绩（×80%）不在本 App 计算范围。
 */
data class ZongceBreakdown(
    val semesterId: Long,
    /** A-E 发展性得分（已由 ScoringEngine 按规则封顶；不含基础性测评） */
    val categoryScores: Map<Category, Float>,
    /** F 负面评价合计（正数存放，仅作展示，不参与本卡计算） */
    val deduction: Float
) {
    /** 五育加权加分合计 */
    val wuyuPart: Float
        get() = categoryScores.entries
            .sumOf { (category, score) -> (score * ZongceEngine.weightOf(category)).toDouble() }
            .toFloat()

    /** 某类的加权加分 */
    fun categoryPart(category: Category): Float =
        (categoryScores[category] ?: 0f) * ZongceEngine.weightOf(category)
}

object ZongceEngine {

    /** 五育权重：德育 6%、智育 5%、体育/美育/劳育各 3% */
    private val CATEGORY_WEIGHTS: Map<Category, Float> = mapOf(
        Category.A to 0.06f,
        Category.B to 0.05f,
        Category.C to 0.03f,
        Category.D to 0.03f,
        Category.E to 0.03f
    )

    fun weightOf(category: Category): Float = CATEGORY_WEIGHTS[category] ?: 0f

    /**
     * 计算某学期的五育对综测加分。
     * @param devScores A-E 发展性得分（已由 ScoringEngine 按规则封顶）
     * @param deduction F 类负面评价合计（正数）
     */
    fun compute(
        semesterId: Long,
        devScores: Map<Category, Float>,
        deduction: Float
    ): ZongceBreakdown = ZongceBreakdown(
        semesterId = semesterId,
        categoryScores = devScores,
        deduction = deduction
    )
}
