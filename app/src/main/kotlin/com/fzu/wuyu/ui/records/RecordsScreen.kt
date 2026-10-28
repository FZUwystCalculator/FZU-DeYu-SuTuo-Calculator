package com.fzu.wuyu.ui.records

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.fzu.wuyu.domain.model.Category
import com.fzu.wuyu.domain.model.DeyuRecord
import com.fzu.wuyu.ui.DeyuViewModel
import com.fzu.wuyu.ui.components.fmtScore
import com.fzu.wuyu.ui.components.sortedByActivityDate
import com.fzu.wuyu.util.DateUtils
import kotlin.math.abs

/**
 * 记录页：汇总查看所有大类（A–F 五育 + S 素拓）与未分类的全部记录。
 *
 * - 「按类别」点亮：按 A→F→S 分层，每层内部按活动时间正序/倒序；
 * - 「按类别」熄灭：全部记录平铺，只按活动时间正序/倒序。
 *
 * 与分类页共用同一份排序偏好（[DeyuViewModel.sortDescending]），
 * 分组开关单独持久化（[DeyuViewModel.groupByCategory]）。
 */
@Composable
fun RecordsScreen(
    viewModel: DeyuViewModel,
    onEditRecord: (DeyuRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val sortDescending by viewModel.sortDescending.collectAsState()
    val grouped by viewModel.groupByCategory.collectAsState()
    var pendingDelete by remember { mutableStateOf<DeyuRecord?>(null) }

    val semesterNames = remember(state.semesters) {
        state.semesters.associate { it.id to it.name }
    }
    val groups = remember(state.allRecords, sortDescending) {
        buildGroups(state.allRecords, sortDescending)
    }
    val flat = remember(state.allRecords, sortDescending) {
        state.allRecords.sortedByActivityDate(sortDescending)
    }

    LazyColumn(
        modifier = modifier,
        // 底部留白覆盖双层悬浮底栏（两层 56dp + 间距 + 留边 ≈ 150dp）
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "全部记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "共 ${state.allRecords.size} 条（五育 + 素拓 + 未分类）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = grouped,
                            onClick = { viewModel.setGroupByCategory(!grouped) },
                            label = { Text("按类别") }
                        )
                        Spacer(Modifier.weight(1f))
                        FilterChip(
                            selected = !sortDescending,
                            onClick = { viewModel.setSortDescending(false) },
                            label = { Text("正序") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = sortDescending,
                            onClick = { viewModel.setSortDescending(true) },
                            label = { Text("倒序") }
                        )
                    }
                    Text(
                        if (grouped) "按 A→F→S 分层，每层内按活动时间排序"
                        else "全部记录按活动时间排序",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (state.allRecords.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "还没有任何记录，点左下角「+」开始添加",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (grouped) {
            groups.forEach { group ->
                item(key = "header_${group.key}") { GroupHeader(group) }
                items(group.records, key = { "rec_${it.id}" }) { record ->
                    RecordCard(
                        record = record,
                        semesterName = semesterNames[record.semesterId],
                        onEdit = { onEditRecord(record) },
                        onDelete = { pendingDelete = record }
                    )
                }
            }
        } else {
            items(flat, key = { "rec_${it.id}" }) { record ->
                RecordCard(
                    record = record,
                    semesterName = semesterNames[record.semesterId],
                    onEdit = { onEditRecord(record) },
                    onDelete = { pendingDelete = record }
                )
            }
        }
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除记录") },
            text = {
                Text(
                    "确定删除「${record.title}」（${DateUtils.format(record.dateEpochDay)}）吗？"
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

// ---------- 分组模型 ----------

/** 记录分组：A–F 五育、S 素拓，以及未分类（key = "unknown"） */
private data class RecordGroup(
    val key: String,
    val title: String,
    val records: List<DeyuRecord>,
    /** 该组是否为扣分性质（F 负面评价），用于分数正负号 */
    val deduct: Boolean
)

/** 按 A→B→C→D→E→F→S 顺序分组，未分类排最后；组内有记录才出现 */
private fun buildGroups(
    records: List<DeyuRecord>,
    sortDescending: Boolean
): List<RecordGroup> {
    val byCategory = records.groupBy { record ->
        if (record.subcategoryId == SubcategoryCatalog.UNKNOWN_SUBCATEGORY_ID) null
        else SubcategoryCatalog.byId(record.subcategoryId)?.category
    }
    val result = ArrayList<RecordGroup>()
    Category.entries.forEach { category ->
        val list = byCategory[category]?.takeIf { it.isNotEmpty() } ?: return@forEach
        result += RecordGroup(
            key = category.code,
            title = "${category.code} · ${category.displayName}",
            records = list.sortedByActivityDate(sortDescending),
            deduct = category.isDeduction
        )
    }
    byCategory[null]?.takeIf { it.isNotEmpty() }?.let { list ->
        result += RecordGroup(
            key = "unknown",
            title = "? · 未分类",
            records = list.sortedByActivityDate(sortDescending),
            deduct = false
        )
    }
    return result
}

/** 组内合计（素拓组内按加分/减分逐条带符号累计） */
private fun RecordGroup.total(): Float = records.sumOf { record ->
    val sub = SubcategoryCatalog.byId(record.subcategoryId)
    val sign = when {
        sub?.isDeducting == true -> -1.0
        else -> 1.0
    }
    record.score * sign
}.toFloat()

// ---------- 组头 ----------

@Composable
private fun GroupHeader(group: RecordGroup) {
    val total = group.total()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            group.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${group.records.size} 条 · ${if (total < 0f) "−" else "+"}${fmtScore(abs(total))}",
            style = MaterialTheme.typography.labelMedium,
            color = if (total < 0f) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- 单条记录 ----------

@Composable
private fun RecordCard(
    record: DeyuRecord,
    semesterName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sub = SubcategoryCatalog.byId(record.subcategoryId)
    val deduct = sub?.isDeducting == true
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    record.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    buildString {
                        append(sub?.let { "${it.category.code} · ${it.name}" } ?: "未分类")
                        append(" · ${DateUtils.format(record.dateEpochDay)}")
                        append(" · ${semesterName ?: "（已删除学期）"}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.note.isNotBlank()) {
                    Text(
                        record.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "${if (deduct) "−" else "+"}${fmtScore(record.score)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (deduct) MaterialTheme.colorScheme.error else Color.Unspecified
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "编辑")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
