package com.fzu.wuyu.data.backup

import com.fzu.wuyu.data.catalog.SubcategoryCatalog
import com.fzu.wuyu.domain.ScoringEngine
import com.fzu.wuyu.domain.ZongceEngine
import com.fzu.wuyu.domain.model.BackupData
import com.fzu.wuyu.domain.model.DeyuRecord
import com.fzu.wuyu.domain.model.DeyuSemester
import com.fzu.wuyu.util.DateUtils
import kotlinx.serialization.json.Json

/** 导入结果 */
sealed interface ImportResult {
    data class Success(val data: BackupData, val warnings: List<String>) : ImportResult
    data class Failure(val message: String) : ImportResult
}

/**
 * 备份导入导出：
 * - JSON：全量备份，可原样导入（导入导出闭环）
 * - Markdown：可读报告，供查看/分享
 */
object BackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun exportJson(data: BackupData): String = json.encodeToString(data)

    fun parseJson(text: String): ImportResult = try {
        val data = json.decodeFromString<BackupData>(text)
        val warnings = mutableListOf<String>()
        if (data.semesters.isEmpty()) warnings += "备份中没有学期数据"
        val knownIds = SubcategoryCatalog.subcategories.map { it.id }.toSet()
        val unknown = data.records.map { it.subcategoryId }.distinct().filter { it !in knownIds }
        if (unknown.isNotEmpty()) {
            warnings += "有 ${unknown.size} 条记录的门类已不在当前规则目录中，导入时将被保留但无法归类：" +
                unknown.joinToString("、")
        }
        val semesterIds = data.semesters.map { it.id }.toSet()
        val orphan = data.records.count { it.semesterId !in semesterIds }
        if (orphan > 0) warnings += "有 $orphan 条记录找不到对应学期，导入时将被丢弃"
        ImportResult.Success(data, warnings)
    } catch (e: Exception) {
        ImportResult.Failure("备份文件解析失败：${e.message ?: "格式不正确"}")
    }

    /** 生成 Markdown 报告（含各学期五育明细与五育对综测的加分；不含基础性测评） */
    fun exportMarkdown(
        semesters: List<DeyuSemester>,
        allRecords: List<DeyuRecord>
    ): String = buildString {
        appendLine("# FZU五育计算器 · 五育测评报告")
        appendLine()
        appendLine("> 导出于 ${DateUtils.format(DateUtils.todayEpochDay())}；" +
            "数据来源：App 本地记录（依据《福州大学本科生综合素质评价参考细则》）")
        appendLine()
        appendLine("**综合素质评价成绩 = 学业测评成绩 × 80% + 德育(A) × 6% + 智育(B) × 5% + " +
            "体育(C) × 3% + 美育(D) × 3% + 劳育(E) × 3% − 负面评价(F)**；" +
            "学业测评成绩 =（平均学分绩点 + 5）× 10。本报告统计五育发展性部分（不含基础性测评），学业成绩以教务系统为准。")
        appendLine()

        val bonusParts = ArrayList<Float>()
        val seen = ArrayList<DeyuRecord>()
        semesters.sortedBy { it.createdAt }.forEach { semester ->
            val records = allRecords.filter { it.semesterId == semester.id }
            val allSummaries = ScoringEngine.overview(records, seen)
            // 五育 A–E 参与综测；F 负面评价单独扣减；S 素拓独立体系，不进综测表
            val wuyu = allSummaries.filter { !it.category.isDeduction && !it.category.isSutuo }
            val deduction = allSummaries.firstOrNull { it.category.isDeduction }?.developmentScore ?: 0f
            val sutuo = allSummaries.firstOrNull { it.category.isSutuo }
            appendLine("## ${semester.name}")
            appendLine()

            // 五育对综测的加分（仅发展性得分 × 权重）
            val zongce = ZongceEngine.compute(
                semesterId = semester.id,
                devScores = wuyu.associate { it.category to it.developmentScore },
                deduction = deduction
            )
            appendLine("**五育对综测的加分：+${fmt(zongce.wuyuPart)}**（不含基础性测评）")
            appendLine()
            appendLine("| 大类 | 发展性得分 | 权重 | 计入分 |")
            appendLine("|:--|--:|--:|--:|")
            wuyu.forEach { s ->
                appendLine(
                    "| ${s.category.code} ${s.category.displayName} " +
                        "| ${fmt(s.developmentScore)} " +
                        "| ${fmtPct(ZongceEngine.weightOf(s.category))} " +
                        "| +${fmt(zongce.categoryPart(s.category))} |"
                )
            }
            if (deduction > 0f) {
                appendLine("| F 负面评价 | -${fmt(deduction)} | 100% | -${fmt(deduction)} |")
            }
            appendLine()
            bonusParts += zongce.wuyuPart

            // 五育总览表
            appendLine("| 大类 | 发展性得分 | 上限 |")
            appendLine("|:--|--:|--:|")
            wuyu.forEach { s ->
                appendLine("| ${s.category.code} ${s.category.displayName} | ${fmt(s.developmentScore)} | ${fmt(s.developmentCap)} |")
            }
            if (deduction > 0f) {
                appendLine("| F 负面评价 | -${fmt(deduction)} | 无上限 |")
            }
            appendLine()
            sutuo?.let { s ->
                appendLine("**总素拓：${fmt(s.sutuoNet)} 分**（加分 ${fmt(s.sutuoAdd)} − " +
                    "减分 ${fmt(s.sutuoDeduct)}，独立于五育、不计入综测）")
                appendLine()
            }

            // 各门类明细
            allSummaries.forEach { summary ->
                appendLine("### ${summary.category.code} ${summary.category.displayName}")
                appendLine()
                summary.basicInfo?.let {
                    appendLine("**基础性测评说明**（上限70分，本 App 不记录）：${it.formula}")
                    appendLine()
                }
                if (summary.category.isDeduction && summary.developmentScore <= 0f) {
                    appendLine("（本学期无负面评价记录）")
                    appendLine()
                }
                summary.subcategories.filter { it.subcategory.recordable }.forEach { p ->
                    val capText = when {
                        p.cap == null -> "无上限"
                        else -> "上限 ${fmt(p.effectiveCap ?: p.cap)}"
                    }
                    val sign = if (p.subcategory.isDeducting) "-" else "+"
                    appendLine("#### ${p.subcategory.name}（${sign}${fmt(p.counted)} / $capText" +
                        "${if (p.remaining != null && p.remaining > 0) "，还差 ${fmt(p.remaining)}" else ""}）")
                    appendLine()
                    val recs = records.filter { it.subcategoryId == p.subcategory.id }
                        .sortedBy { it.dateEpochDay }
                    if (recs.isEmpty()) {
                        appendLine("- （暂无记录）")
                    } else {
                        recs.forEach { r ->
                            val rSign = if (p.subcategory.isDeducting) "-" else "+"
                            appendLine("- ${DateUtils.format(r.dateEpochDay)} ${r.title} $rSign${fmt(r.score)} 分" +
                                if (r.note.isNotBlank()) "（${r.note}）" else "")
                        }
                    }
                    appendLine()
                }
            }
            // 未分类记录（不计入五育）
            val unknownRecs = records
                .filter { it.subcategoryId == SubcategoryCatalog.UNKNOWN_SUBCATEGORY_ID }
                .sortedBy { it.dateEpochDay }
            if (unknownRecs.isNotEmpty()) {
                appendLine("### 未分类记录（不计入五育）")
                appendLine()
                unknownRecs.forEach { r ->
                    appendLine(
                        "- ${DateUtils.format(r.dateEpochDay)} ${r.title} +${fmt(r.score)} 分" +
                            if (r.note.isNotBlank()) "（${r.note}）" else ""
                    )
                }
                appendLine()
            }
            seen += records
        }

        if (bonusParts.isNotEmpty()) {
            appendLine("---")
            appendLine()
            appendLine(
                "**五育对综测的加分（多学期平均）= ${fmt(bonusParts.sum())} / ${bonusParts.size} = " +
                    "${fmt(bonusParts.sum() / bonusParts.size)}**"
            )
            appendLine()
        }
    }

    /** 权重格式化：6% / 5% / 3% */
    private fun fmtPct(v: Float): String =
        if (v == v.toLong().toFloat()) "${v.toLong()}%" else "${(v * 100).toInt()}%"

    private fun fmt(v: Float): String =
        if (v == v.toLong().toFloat()) v.toLong().toString() else "%.2f".format(v)
}
