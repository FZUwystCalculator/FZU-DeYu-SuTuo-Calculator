package com.fzu.wuyu

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fzu.wuyu.data.ThemeMode
import com.fzu.wuyu.data.ThemePrefs
import com.fzu.wuyu.data.backup.ImportResult
import com.fzu.wuyu.ui.DeyuViewModel
import com.fzu.wuyu.ui.theme.DeyuCalculatorTheme
import com.fzu.wuyu.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 主题模式：本地持久化，设置页「外观」卡片中切换
            val themePrefs = remember { ThemePrefs(applicationContext) }
            var themeMode by remember { mutableStateOf(themePrefs.themeMode) }
            DeyuCalculatorTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            themePrefs.themeMode = mode
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val viewModel: DeyuViewModel = viewModel(factory = DeyuViewModel.factory(context))
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 导出：JSON 全量备份
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val ok = writeTextToUri(context, it, viewModel.exportJsonText())
            scope.launch {
                snackbarHostState.showSnackbar(if (ok) "导出成功" else "导出失败，请重试")
            }
        }
    }
    // 导出：Markdown 可读报告
    val exportMdLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        uri?.let {
            val ok = writeTextToUri(context, it, viewModel.exportMarkdownText())
            scope.launch {
                snackbarHostState.showSnackbar(if (ok) "导出成功" else "导出失败，请重试")
            }
        }
    }
    // 导入：JSON 备份
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } ?: ""
                }
                val result = viewModel.importBackup(text)
                val message = when (result) {
                    is ImportResult.Failure -> result.message
                    is ImportResult.Success ->
                        if (result.warnings.isEmpty()) "导入成功"
                        else "导入成功，但有提醒：" + result.warnings.joinToString("；")
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    MainNavigation(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
        onExportJson = {
            exportJsonLauncher.launch("五育备份-${DateUtils.format(DateUtils.todayEpochDay())}.json")
        },
        onExportMarkdown = {
            exportMdLauncher.launch("五育报告-${DateUtils.format(DateUtils.todayEpochDay())}.md")
        },
        onImport = { importLauncher.launch(arrayOf("application/json")) }
    )
}

/** 把文本写入用户选择的 URI，返回是否成功 */
private fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean =
    try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
        } != null
    } catch (_: Exception) {
        false
    }
