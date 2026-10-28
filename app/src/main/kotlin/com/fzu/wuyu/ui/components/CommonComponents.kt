package com.fzu.wuyu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fzu.wuyu.domain.model.DeyuRecord
import com.fzu.wuyu.util.DateUtils

/** 稳定的下拉框：Box + DropdownMenu + 透明点击层，避开 ExposedDropdownMenu 的 API 漂移 */
@Composable
fun SimpleDropdown(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = items.getOrElse(selectedIndex) { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** 日期选择字段：显示 "2026-10-26"，点击弹 DatePicker */
@Composable
fun DateField(label: String, epochDay: Long, onPick: (Long) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = DateUtils.format(epochDay),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }
    if (showDialog) {
        val state = rememberDatePickerState(initialSelectedDateMillis = epochDay * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(it / 86_400_000L) }
                    showDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/**
 * 分数格式化：统一保留至小数点后两位——
 * 第二位为 0 则不显示（3.60→"3.6"），整数不显示小数点（4.00→"4"），
 * 有第二位非零的完整保留（0.15→"0.15"，不做四舍五入到一位）。
 */
fun fmtScore(v: Float): String =
    "%.2f".format(v).trimEnd('0').trimEnd('.')

/** 记录按活动时间排序（正序/倒序，时间相同按录入时间稳定排序） */
fun List<DeyuRecord>.sortedByActivityDate(
    descending: Boolean
): List<DeyuRecord> =
    if (descending) {
        sortedWith(
            compareByDescending<DeyuRecord> { it.dateEpochDay }
                .thenByDescending { it.createdAt }
        )
    } else {
        sortedWith(
            compareBy<DeyuRecord> { it.dateEpochDay }
                .thenBy { it.createdAt }
        )
    }

/** 记录排序切换（按活动时间正序/倒序），全局一份，分类页与总览页共用 */
@Composable
fun SortSwitchRow(
    sortDescending: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "活动排序",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = !sortDescending,
            onClick = { onChange(false) },
            label = { Text("正序") }
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            selected = sortDescending,
            onClick = { onChange(true) },
            label = { Text("倒序") }
        )
    }
}

// ---------- 说明文字折叠（小圆圈 + 感叹号） ----------

/**
 * 说明小圆圈本体：22dp 圆底 + 感叹号。
 * 配合 [InfoHintButton] / [SectionTitleWithHint] 把大段说明文字收进 UI，
 * 默认只显示圆圈，点击后才展开文字，避免界面被文字填满显得杂乱。
 */
@Composable
fun HintDot(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.size(22.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "!",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 独立的说明按钮：小圆圈 + 展开后出现在圆圈下方的说明文字（默认隐藏） */
@Composable
fun InfoHintButton(
    text: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        HintDot(onClick = { expanded = !expanded })
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * 卡片标题 + 右侧说明小圆圈：说明文字默认隐藏，
 * 点击圆圈后在标题下方平滑展开，再次点击收起。
 */
@Composable
fun SectionTitleWithHint(
    title: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            HintDot(onClick = { expanded = !expanded })
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
