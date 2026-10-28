package com.fzu.wuyu.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fzu.wuyu.data.ThemeMode
import com.fzu.wuyu.domain.model.DeyuSemester
import com.fzu.wuyu.ui.DeyuUiState
import com.fzu.wuyu.ui.DeyuViewModel
import com.fzu.wuyu.ui.components.SectionTitleWithHint
import com.fzu.wuyu.ui.components.SimpleDropdown
import com.fzu.wuyu.util.DateUtils

private const val FEEDBACK_EMAIL = "fzu.wyst.calculator@proton.me"

/**
 * 设置页（由底栏第一层「设置」进入）：
 * ① 学期设置 ② 外观 ③ 数据导出/导入 ④ 建议反馈 ⑤ 关于。
 */
@Composable
fun SettingsScreen(
    viewModel: DeyuViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onExportJson: () -> Unit,
    onExportMarkdown: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showAddSemester by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<DeyuSemester?>(null) }
    var deleteTarget by remember { mutableStateOf<DeyuSemester?>(null) }

    LazyColumn(
        modifier = modifier,
        // 底部留白覆盖双层悬浮底栏（两层 56dp + 间距 + 留边 ≈ 150dp）
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SemesterSettingsCard(
                state = state,
                onSelectSemester = viewModel::selectSemester,
                onCreateSemester = { showAddSemester = true },
                onRename = { renameTarget = it },
                onDelete = { deleteTarget = it }
            )
        }
        item { AppearanceCard(themeMode = themeMode, onThemeModeChange = onThemeModeChange) }
        item {
            BackupCard(
                onExportJson = onExportJson,
                onExportMarkdown = onExportMarkdown,
                onImport = onImport
            )
        }
        item { FeedbackCard() }
        item { AboutCard() }
    }

    if (showAddSemester) {
        AddSemesterDialog(
            onDismiss = { showAddSemester = false },
            onConfirm = { name ->
                viewModel.addSemester(name)
                showAddSemester = false
            }
        )
    }

    renameTarget?.let { semester ->
        RenameSemesterDialog(
            semester = semester,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                viewModel.renameSemester(semester.id, name)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { semester ->
        DeleteSemesterDialog(
            recordCount = state.allRecords.count { it.semesterId == semester.id },
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.deleteSemester(semester.id)
                deleteTarget = null
            }
        )
    }
}

// ---------- ① 学期设置 ----------

@Composable
private fun SemesterSettingsCard(
    state: DeyuUiState,
    onSelectSemester: (Long) -> Unit,
    onCreateSemester: () -> Unit,
    onRename: (DeyuSemester) -> Unit,
    onDelete: (DeyuSemester) -> Unit
) {
    val selected = state.selectedSemester
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitleWithHint(
                title = "学期设置",
                hint = "记录按学期隔离，各页签只显示当前学期的数据；删除学期会同时删除其全部记录。"
            )
            Spacer(Modifier.height(8.dp))
            SimpleDropdown(
                label = "当前学期",
                items = state.semesters.map { it.name } + "＋ 新建学期…",
                selectedIndex = state.semesters.indexOfFirst { it.id == state.selectedSemesterId }
                    .takeIf { it >= 0 } ?: 0,
                onSelect = { index ->
                    if (index >= state.semesters.size) onCreateSemester()
                    else onSelectSemester(state.semesters[index].id)
                }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { selected?.let(onRename) },
                    enabled = selected != null,
                    modifier = Modifier.weight(1f)
                ) { Text("重命名") }
                OutlinedButton(
                    onClick = { selected?.let(onDelete) },
                    enabled = selected != null,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除本学期") }
            }
        }
    }
}

// ---------- ② 外观 ----------

@Composable
private fun AppearanceCard(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitleWithHint(
                title = "外观",
                hint = "「跟随系统」随手机系统的深色模式自动切换；选择结果本地保存。"
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == themeMode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size
                        )
                    ) {
                        Text(mode.displayName)
                    }
                }
            }
        }
    }
}

// ---------- ③ 数据导出 / 导入 ----------

@Composable
private fun BackupCard(
    onExportJson: () -> Unit,
    onExportMarkdown: () -> Unit,
    onImport: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitleWithHint(
                title = "数据导出 / 导入",
                hint = "导出包含全部学期数据；导入会用备份文件覆盖当前全部数据，请谨慎操作。"
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onExportJson, modifier = Modifier.fillMaxWidth()) {
                Text("导出 JSON 备份")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onExportMarkdown, modifier = Modifier.fillMaxWidth()) {
                Text("导出 Markdown 报告")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Text("导入备份…")
            }
        }
    }
}

// ---------- ④ 建议反馈 ----------

@Composable
private fun FeedbackCard() {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitleWithHint(
                title = "建议反馈",
                hint = "使用中遇到问题，或对计分规则、门类设置有改进建议，欢迎发送邮件反馈。" +
                    "点击按钮调起系统邮件应用；本 App 不联网，邮件由你的邮件客户端发送。"
            )
            Spacer(Modifier.height(8.dp))
            Text(
                FEEDBACK_EMAIL,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$FEEDBACK_EMAIL")
                        putExtra(Intent.EXTRA_SUBJECT, "五育得分记录器 · 建议反馈")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            "未找到邮件应用，请手动发送至 $FEEDBACK_EMAIL",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("发送邮件") }
        }
    }
}

// ---------- ⑤ 关于 ----------

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitleWithHint(
                title = "关于",
                hint = "本 App 记录福州大学五育发展性测评得分：每项素质满分 100 = 基础性 70 + 发展性 30，" +
                    "App 只记录发展性部分（A–E 共 29 门类，F 负面评价按 100% 扣减）。" +
                    "综测 = 学业成绩 ×80% + A德育×6% + B智育×5% + C体育×3% + D美育×3% + E劳育×3% − F负面评价分。" +
                    "全部数据仅保存在本机，App 不联网。"
            )
            Spacer(Modifier.height(8.dp))
            InfoRow(label = "应用", value = "FZU五育计算器")
            InfoRow(label = "版本", value = versionName)
            InfoRow(label = "包名", value = "com.fzu.wuyu")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------- 学期对话框 ----------

@Composable
private fun AddSemesterDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(DateUtils.currentSemesterName()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建学期") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("学期名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun RenameSemesterDialog(
    semester: DeyuSemester,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(semester.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名学期") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("学期名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DeleteSemesterDialog(
    recordCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除学期") },
        text = { Text("确定删除该学期吗？将同时删除其中的 $recordCount 条记录，且不可恢复。") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("删除") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
