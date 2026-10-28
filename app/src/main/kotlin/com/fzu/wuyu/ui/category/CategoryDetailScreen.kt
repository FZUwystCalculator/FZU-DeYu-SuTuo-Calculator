package com.fzu.wuyu.ui.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.fzu.wuyu.domain.SubcategoryProgress
import com.fzu.wuyu.domain.model.Category
import com.fzu.wuyu.domain.model.DeyuRecord
import com.fzu.wuyu.ui.DeyuViewModel
import com.fzu.wuyu.ui.components.HintDot
import com.fzu.wuyu.ui.components.SectionTitleWithHint
import com.fzu.wuyu.ui.components.SortSwitchRow
import com.fzu.wuyu.ui.components.fmtScore
import com.fzu.wuyu.ui.components.sortedByActivityDate
import com.fzu.wuyu.util.DateUtils

/**
 * 大类页（底部页签 A–F / S）：发展性总分 + 门类进度 + 记录明细。
 * 顶栏/底栏/记录编辑器由外层 MainNavigation 统一提供，
 * 这里只负责内容；新增/编辑记录通过回调上移。
 */
@Composable
fun CategoryDetailScreen(
    category: Category,
    viewModel: DeyuViewModel,
    onAddRecord: (presetSubcategoryId: String?) -> Unit,
    onEditRecord: (DeyuRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val summary = state.summaryOf(category)
    val sortDescending by viewModel.sortDescending.collectAsState()
    // 各门类下的记录统一按活动时间排序（正序/倒序可切换，偏好全局保存）
    val sortedRecords = remember(state.records, sortDescending) {
        state.records.sortedByActivityDate(sortDescending)
    }
    var expandedSubId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<DeyuRecord?>(null) }

    LazyColumn(
        modifier = modifier,
        // 底部留白覆盖双层悬浮底栏（两层 56dp + 间距 + 留边 ≈ 150dp）
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (summary != null) {
            // 总分卡片（F 负面评价为扣分，无上限、不占发展性 30 分）
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        if (category.isSutuo) {
                            SectionTitleWithHint(
                                title = "总素拓（S 类）",
                                hint = "素质拓展分独立于五育：加分项累加、减分项扣减，不封顶、" +
                                    "不计入综测成绩，主要用于评优评先、推优入党、学生干部选拔等考核。" +
                                    "减分项分值自成一套，与五育 F 类不同。"
                            )
                            Text(
                                "${fmtScore(summary.sutuoNet)} 分",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.sutuoNet < 0f) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "加分 ${fmtScore(summary.sutuoAdd)} 分 · " +
                                    "减分 ${fmtScore(summary.sutuoDeduct)} 分",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (category.isDeduction) {
                            SectionTitleWithHint(
                                title = "负面评价合计（F 类）",
                                hint = "按 100% 直接扣减综测成绩，不计入五育发展性总分。"
                            )
                            Text(
                                "−${fmtScore(summary.developmentScore)} 分",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                "发展性测评总分",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${fmtScore(summary.developmentScore)} / ${summary.developmentCap.toInt()} 分" +
                                    if (summary.developmentRaw > summary.developmentScore)
                                        "（原始累计 ${fmtScore(summary.developmentRaw)}，超出部分不计）" else "",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.padding(4.dp))
                            if (summary.developmentCap > 0) {
                                LinearProgressIndicator(
                                    progress = {
                                        (summary.developmentScore / summary.developmentCap)
                                            .coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            // 门类列表
            items(summary.subcategories.filter { it.subcategory.recordable }) { p ->
                SubcategoryCard(
                    progress = p,
                    records = sortedRecords.filter { it.subcategoryId == p.subcategory.id },
                    expanded = expandedSubId == p.subcategory.id,
                    onToggleExpand = {
                        expandedSubId = if (expandedSubId == p.subcategory.id) null else p.subcategory.id
                    },
                    sortDescending = sortDescending,
                    onSortChange = viewModel::setSortDescending,
                    onAddRecord = { onAddRecord(p.subcategory.id) },
                    onEditRecord = onEditRecord,
                    onDeleteRecord = { pendingDelete = it }
                )
            }
            // 基础性测评说明（默认收起，点击标题右侧小圆圈展开）
            item {
                summary.basicInfo?.let { info ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitleWithHint(
                                title = "基础性测评说明（上限 " +
                                    "${SubcategoryCatalog.BASIC_CAP.toInt()} 分，不记录）",
                                hint = info.formula
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { record ->
        // 正负号按门类口径判断（素拓减分项也是扣分）
        val sign = if (SubcategoryCatalog.byId(record.subcategoryId)?.isDeducting == true) "−" else "+"
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除记录") },
            text = {
                Text(
                    "确定删除「${record.title}」" +
                        "（${DateUtils.format(record.dateEpochDay)} $sign${fmtScore(record.score)} 分）吗？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(record)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun SubcategoryCard(
    progress: SubcategoryProgress,
    records: List<DeyuRecord>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    sortDescending: Boolean,
    onSortChange: (Boolean) -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (DeyuRecord) -> Unit,
    onDeleteRecord: (DeyuRecord) -> Unit
) {
    // 素拓的减分项也是扣分口径（与五育 F 类一样红色显示、带负号）
    val deduction = progress.subcategory.isDeducting
    val fraction = progress.effectiveCap?.let { cap ->
        if (cap > 0) (progress.counted / cap).coerceIn(0f, 1f) else 0f
    }
    // 状态行：有上限才显示"上限/还差/已满"；无上限（含 F 类）只显示当前分
    val statusLine = buildString {
        append("当前 ${fmtScore(progress.counted)}")
        if (progress.cap != null) {
            val capText = if (progress.effectiveCap != null && progress.effectiveCap < progress.cap) {
                "${fmtScore(progress.effectiveCap)}（规则 ${fmtScore(progress.cap)}，往学期已扣减）"
            } else {
                fmtScore(progress.cap)
            }
            append(" | 上限 $capText")
            progress.remaining?.let { remaining ->
                append(if (remaining > 0f) " | 还差 ${fmtScore(remaining)}" else " | 已满")
            }
        } else {
            append(" | 无上限")
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            // 大号当前总分：3.6/4（F 类为扣分，带负号、红色）
            Text(
                text = buildString {
                    if (deduction) append("−")
                    append(fmtScore(progress.counted))
                    progress.effectiveCap?.let { append("/${fmtScore(it)}") }
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (deduction) MaterialTheme.colorScheme.error else Color.Unspecified
            )
            Text(
                progress.subcategory.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                statusLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (fraction != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(4.dp))
            // 计分说明收进小圆圈（默认隐藏，点击展开）+ 操作按钮
            val note = progress.subcategory.note
            var noteExpanded by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note != null) {
                    HintDot(onClick = { noteExpanded = !noteExpanded })
                    Spacer(Modifier.width(8.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onAddRecord) {
                    Icon(Icons.Default.Add, contentDescription = "添加${progress.subcategory.name}记录")
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "收起" else "展开记录"
                    )
                }
            }
            AnimatedVisibility(
                visible = noteExpanded && note != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = note ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
            }
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                if (records.isEmpty()) {
                    Text(
                        "暂无记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    // 活动排序（正序/倒序）：仅在展开该门类、且有记录时出现，
                    // 位于全部活动记录上方、说明圆圈/加号/展开箭头的下方
                    SortSwitchRow(
                        sortDescending = sortDescending,
                        onChange = onSortChange,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    records.forEach { record ->
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(record.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${DateUtils.format(record.dateEpochDay)} · " +
                                        "${if (deduction) "−" else "+"}${fmtScore(record.score)} 分" +
                                        if (record.note.isNotBlank()) " · ${record.note}" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (deduction) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onEditRecord(record) }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = { onDeleteRecord(record) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
            }
        }
    }
}
