package com.fzu.wuyu.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fzu.wuyu.data.catalog.SubcategoryCatalog
import com.fzu.wuyu.domain.model.Category
import com.fzu.wuyu.domain.model.DeyuRecord
import com.fzu.wuyu.domain.model.ScoringType
import com.fzu.wuyu.domain.model.Subcategory
import com.fzu.wuyu.domain.model.TierOption
import com.fzu.wuyu.ui.components.DateField
import com.fzu.wuyu.ui.components.InfoHintButton
import com.fzu.wuyu.ui.components.SimpleDropdown
import com.fzu.wuyu.ui.components.fmtScore
import com.fzu.wuyu.util.DateUtils

/**
 * 添加/编辑记录的底部弹窗。
 * 表单按门类计分方式动态变化：
 * - PER_TIME：次数 → 自动分
 * - PER_DAY：起止日期 → 天数 → 自动分
 * - TIERED：级别下拉 + 等第下拉（末项"自定义…"）→ 自动分
 * - FIXED：选项下拉（末项"自定义…"）→ 自动分
 * - MANUAL：直接输分
 * 除 MANUAL 外均可开"自定义分数"覆盖自动值。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditorSheet(
    category: Category?,
    presetSubcategoryId: String?,
    existingRecord: DeyuRecord?,
    semesterId: Long?,
    onDismiss: () -> Unit,
    onSave: (DeyuRecord) -> Unit
) {
    // 大类可随时切换（顶部两行 chip 网格）：分类页打开时预设对应类，总览页打开时为 null（无默认）
    var selectedCategory by remember { mutableStateOf(category) }
    // 未分类记录：暂不归入 A-F，不计入五育，之后可在总览页归类
    var unknownSelected by remember {
        mutableStateOf(
            existingRecord?.subcategoryId == SubcategoryCatalog.UNKNOWN_SUBCATEGORY_ID ||
                presetSubcategoryId == SubcategoryCatalog.UNKNOWN_SUBCATEGORY_ID
        )
    }
    val recordable = remember(selectedCategory) {
        selectedCategory?.let { c -> SubcategoryCatalog.byCategory(c).filter { it.recordable } }
            ?: emptyList()
    }
    var subcategoryId by remember {
        mutableStateOf(
            existingRecord?.subcategoryId
                ?: presetSubcategoryId
                ?: category?.let { c ->
                    SubcategoryCatalog.byCategory(c).filter { it.recordable }.firstOrNull()?.id
                } ?: ""
        )
    }
    val subcategory = SubcategoryCatalog.byId(subcategoryId)
    // 扣分口径：五育 F 类 或 素拓减分项
    val deducting = subcategory?.isDeducting == true

    var title by remember { mutableStateOf(existingRecord?.title ?: "") }
    var note by remember { mutableStateOf(existingRecord?.note ?: "") }
    var dateEpochDay by remember {
        mutableStateOf(existingRecord?.dateEpochDay ?: DateUtils.todayEpochDay())
    }
    var startDay by remember {
        mutableStateOf(existingRecord?.dateEpochDay ?: DateUtils.todayEpochDay())
    }
    var endDay by remember {
        mutableStateOf(existingRecord?.dateEpochDay ?: DateUtils.todayEpochDay())
    }
    var selectedOption by remember { mutableStateOf(existingRecord?.let { matchOption(it) }) }

    // 初始自定义模式：编辑时分数与任何选项都对不上 → 自定义
    var customMode by remember {
        mutableStateOf(
            existingRecord != null && SubcategoryCatalog.byId(existingRecord.subcategoryId)
                ?.scoringType == ScoringType.MANUAL ||
                (existingRecord != null && matchOption(existingRecord)?.score != existingRecord.score &&
                    SubcategoryCatalog.byId(existingRecord.subcategoryId)?.scoringType in
                    setOf(ScoringType.TIERED, ScoringType.FIXED))
        )
    }
    var customScoreText by remember {
        mutableStateOf(existingRecord?.let { fmtScore(it.score) } ?: "")
    }
    var countText by remember {
        mutableStateOf(
            existingRecord?.let { r ->
                val unit = SubcategoryCatalog.byId(r.subcategoryId)?.unitScore
                if (unit != null && unit > 0 && r.score % unit == 0f) (r.score / unit).toLong().toString() else "1"
            } ?: "1"
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val autoScore: Float = when (subcategory?.scoringType) {
        ScoringType.PER_TIME -> (subcategory.unitScore ?: 0f) * (countText.toLongOrNull() ?: 0)
        ScoringType.PER_DAY -> (subcategory.unitScore ?: 0f) * (endDay - startDay + 1).coerceAtLeast(0)
        ScoringType.TIERED, ScoringType.FIXED -> selectedOption?.score ?: 0f
        ScoringType.MANUAL -> customScoreText.toFloatOrNull() ?: 0f
        else -> 0f
    }
    val finalScore = if (customMode) (customScoreText.toFloatOrNull() ?: 0f) else autoScore

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (existingRecord != null) "编辑记录" else "添加记录",
                style = MaterialTheme.typography.titleLarge
            )

            // 记录大类：A–F 五育 + S 素拓 + ? 未分类，共 8 个 chip，不用下拉，可随时切换
            Text(
                "记录大类",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // 大类选择不再横向滚动：A–F + S + ? 共 8 个 chip，固定两行、每行 4 个，
            // 每个 chip 占等宽槽位并铺满（间距 8dp 均匀），选中态切换也不会顶开邻居
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (Category.entries.map { it to false } + (null to true)).chunked(4)
                    .forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { (cat, isUnknown) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    if (isUnknown) {
                                        FilterChip(
                                            selected = unknownSelected,
                                            onClick = {
                                                if (!unknownSelected) {
                                                    unknownSelected = true
                                                    selectedCategory = null
                                                    subcategoryId =
                                                        SubcategoryCatalog.UNKNOWN_SUBCATEGORY_ID
                                                    selectedOption = null
                                                    customMode = true
                                                    customScoreText = ""
                                                    countText = "1"
                                                }
                                            },
                                            label = {
                                                ChipLabelText("? 未知")
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        FilterChip(
                                            selected = selectedCategory == cat && !unknownSelected,
                                            onClick = {
                                                if (unknownSelected || selectedCategory != cat) {
                                                    unknownSelected = false
                                                    selectedCategory = cat
                                                    subcategoryId = SubcategoryCatalog
                                                        .byCategory(cat!!)
                                                        .filter { it.recordable }
                                                        .firstOrNull()?.id ?: ""
                                                    selectedOption = null
                                                    customMode = false
                                                    customScoreText = ""
                                                    countText = "1"
                                                }
                                            },
                                            label = {
                                                ChipLabelText(categoryChipLabel(cat))
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            // 末行不满 4 个时补空槽，保证与上一行的列对齐
                            repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
            }
            InfoHintButton(
                text = "“?” = 未分类：暂不归入任何大类，不计入五育得分，之后可在总览页随时归类",
                modifier = Modifier.padding(top = 4.dp)
            )

            if (unknownSelected) {
                InfoHintButton(
                    text = "未分类记录：请填写活动名称、日期与分数，保存后可在总览页「未分类记录」中归类。",
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (recordable.isEmpty()) {
                Text(
                    "请先在上方选择记录大类（A–F 五育 / S 素拓 / ? 未分类）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SimpleDropdown(
                    label = "门类",
                    items = recordable.map { it.name },
                    selectedIndex = recordable.indexOfFirst { it.id == subcategoryId }.coerceAtLeast(0),
                    onSelect = { index ->
                        subcategoryId = recordable[index].id
                        selectedOption = null
                        customMode = false
                        customScoreText = ""
                        countText = "1"
                    }
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (deducting) "事项名称" else "活动名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // key(subcategoryId) 让计分输入区的内部状态随门类切换重置
            key(subcategoryId) {
                when (subcategory?.scoringType) {
                    ScoringType.PER_DAY -> {
                        DateField("开始日期", startDay) { startDay = it }
                        DateField("结束日期", endDay) { endDay = it }
                        Text(
                            "共 ${(endDay - startDay + 1).coerceAtLeast(0)} 天 × ${fmtScore(subcategory.unitScore ?: 0f)} 分/天",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ScoringType.PER_TIME -> {
                        OutlinedTextField(
                            value = countText,
                            onValueChange = { countText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("次数") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${countText.toLongOrNull() ?: 0} 次 × ${fmtScore(subcategory.unitScore ?: 0f)} 分/次",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ScoringType.TIERED -> TieredSelector(
                        subcategory = subcategory,
                        selectedOption = selectedOption,
                        onSelect = { selectedOption = it; customMode = false },
                        onCustom = {
                            customMode = true
                            if (customScoreText.isBlank()) customScoreText = fmtScore(selectedOption?.score ?: 0f)
                        }
                    )
                    ScoringType.FIXED -> FixedSelector(
                        subcategory = subcategory,
                        selectedOption = selectedOption,
                        onSelect = { selectedOption = it; customMode = false },
                        onCustom = {
                            customMode = true
                            if (customScoreText.isBlank()) customScoreText = fmtScore(selectedOption?.score ?: 0f)
                        }
                    )
                    else -> Unit
                }
            }

            if (subcategory?.scoringType != ScoringType.PER_DAY) {
                DateField("活动日期", dateEpochDay) { dateEpochDay = it }
            }

            if (subcategory?.scoringType != ScoringType.MANUAL) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = customMode, onCheckedChange = { customMode = it })
                    Spacer(Modifier.width(8.dp))
                    Text("自定义分数")
                }
            }
            if (customMode) {
                OutlinedTextField(
                    value = customScoreText,
                    onValueChange = { customScoreText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("分数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（选填）") },
                modifier = Modifier.fillMaxWidth()
            )

            if (unknownSelected) {
                Text(
                    "未分类记录：暂不计入五育，归类后开始计分",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (selectedCategory?.isDeduction == true) {
                Text(
                    "本条扣分：−${fmtScore(finalScore)} 分（按 100% 扣减综测成绩）",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (subcategory?.deduct == true) {
                Text(
                    "本条素拓扣分：−${fmtScore(finalScore)} 分（从总素拓中扣减）",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (selectedCategory?.isSutuo == true) {
                Text(
                    "本条计入素拓：+${fmtScore(finalScore)} 分",
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                Text("本条计入：+${fmtScore(finalScore)} 分", style = MaterialTheme.typography.titleMedium)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            DeyuRecord(
                                id = existingRecord?.id ?: 0,
                                semesterId = existingRecord?.semesterId ?: semesterId ?: 0,
                                subcategoryId = subcategoryId,
                                title = title.trim(),
                                score = finalScore,
                                dateEpochDay = if (subcategory?.scoringType == ScoringType.PER_DAY) startDay else dateEpochDay,
                                note = note.trim(),
                                createdAt = existingRecord?.createdAt ?: 0
                            )
                        )
                    },
                    enabled = title.isNotBlank() && subcategoryId.isNotBlank() &&
                        finalScore > 0f && semesterId != null
                ) { Text("保存") }
            }
        }
    }
}

/** 级别下拉（有分组时）+ 等第下拉；等第末项为"自定义…" */
@Composable
private fun TieredSelector(
    subcategory: Subcategory,
    selectedOption: TierOption?,
    onSelect: (TierOption?) -> Unit,
    onCustom: () -> Unit
) {
    val groups = remember(subcategory) { subcategory.options.mapNotNull { it.group }.distinct() }
    var group by remember(subcategory) {
        mutableStateOf(selectedOption?.group ?: groups.firstOrNull())
    }
    val optionsInGroup = subcategory.options.filter { it.group == group }

    if (groups.isNotEmpty()) {
        SimpleDropdown(
            label = "级别",
            items = groups,
            selectedIndex = groups.indexOf(group).coerceAtLeast(0),
            onSelect = { index ->
                group = groups[index]
                onSelect(optionsInGroup.firstOrNull()) // 切级别时默认选中该级第一档
            }
        )
    }
    // 未选时展示「请选择…」而不是假装选中首项（否则显示值与实际分数不一致）
    SimpleDropdown(
        label = "等第 / 档次",
        items = listOf("请选择…") + optionsInGroup.map { it.label } + "自定义…",
        selectedIndex = when {
            selectedOption == null -> 0
            else -> optionsInGroup.indexOfFirst { it.label == selectedOption.label } + 1
        }.coerceAtLeast(0),
        onSelect = { index ->
            when {
                index == 0 -> onSelect(null)
                index > optionsInGroup.size -> onCustom()
                else -> onSelect(optionsInGroup[index - 1])
            }
        }
    )
    if (selectedOption != null) {
        Text(
            "${selectedOption.label} = ${fmtScore(selectedOption.score)} 分",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FixedSelector(
    subcategory: Subcategory,
    selectedOption: TierOption?,
    onSelect: (TierOption?) -> Unit,
    onCustom: () -> Unit
) {
    // 未选时展示「请选择…」而不是假装选中首项
    SimpleDropdown(
        label = "选项",
        items = listOf("请选择…") + subcategory.options.map { it.label } + "自定义…",
        selectedIndex = when {
            selectedOption == null -> 0
            else -> subcategory.options.indexOfFirst { it.label == selectedOption.label } + 1
        }.coerceAtLeast(0),
        onSelect = { index ->
            when {
                index == 0 -> onSelect(null)
                index > subcategory.options.size -> onCustom()
                else -> onSelect(subcategory.options[index - 1])
            }
        }
    )
    if (selectedOption != null) {
        Text(
            "${selectedOption.label} = ${fmtScore(selectedOption.score)} 分",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 大类 chip 的文案：字母 + 具体名称（如「A 德育」「S 素拓」「? 未知」），
 * 让等宽槽位不至于显得空荡；F 用「减分」这个短名，避免 4 字名撑爆一行 4 个。
 */
private fun categoryChipLabel(category: Category?): String = when (category) {
    null -> "? 未知"
    Category.F -> "F 减分"
    else -> "${category.code} ${category.displayName}"
}

/** chip 内文案：铺满 chip 并居中，避免字母偏左 */
@Composable
private fun ChipLabelText(text: String) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        maxLines = 1,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
    )
}

/** 编辑记录时反查它对应的选项 */
private fun matchOption(record: DeyuRecord): TierOption? {
    val sub = SubcategoryCatalog.byId(record.subcategoryId) ?: return null
    return sub.options.firstOrNull { it.score == record.score }
}
