package com.fzu.wuyu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fzu.wuyu.data.ThemeMode
import com.fzu.wuyu.data.catalog.SubcategoryCatalog
import com.fzu.wuyu.domain.model.Category
import com.fzu.wuyu.domain.model.DeyuRecord
import com.fzu.wuyu.ui.DeyuViewModel
import com.fzu.wuyu.ui.category.CategoryDetailScreen
import com.fzu.wuyu.ui.components.fmtScore
import com.fzu.wuyu.ui.overview.OverviewScreen
import com.fzu.wuyu.ui.record.RecordEditorSheet
import com.fzu.wuyu.ui.records.RecordsScreen
import com.fzu.wuyu.ui.settings.SettingsScreen
import kotlin.math.abs

/**
 * 底部导航页签。
 * 双层悬浮底栏：第一层 = 添加（圆形按钮）/ 总览 / 记录 / 设置，
 * 第二层 = A–F 六个五育分类 + S 素拓。
 */
private enum class MainTab(val label: String) {
    OVERVIEW("总览"),
    RECORDS("记录"),
    SETTINGS("设置"),
    A("德育"),
    B("智育"),
    C("体育"),
    D("美育"),
    E("劳育"),
    F("减分"),
    S("素拓")
}

/** 左右划切换序列：总览与六个分类 + 素拓按底栏方位排列；记录/设置不参与划动 */
private val swipeTabs = listOf(
    MainTab.OVERVIEW, MainTab.A, MainTab.B, MainTab.C, MainTab.D, MainTab.E, MainTab.F, MainTab.S
)

/** 第二层底栏的分类页签（A–F 五育 + S 素拓） */
private val bottomTabs = listOf(
    MainTab.A, MainTab.B, MainTab.C, MainTab.D, MainTab.E, MainTab.F, MainTab.S
)

/**
 * 记录编辑器请求（由导航层持有，分类页的 + 按钮/记录编辑/底栏添加圆钮都通过它打开）。
 * [category] 为 null 表示不预设大类（用户在添加页自选）；
 * presetSubcategoryId = "unknown" 表示默认选未分类。
 */
private data class EditorRequest(
    val category: Category?,
    val presetSubcategoryId: String?,
    val existingRecord: DeyuRecord?
)

/**
 * 应用主框架：单 Scaffold 持有全部 Chrome（顶栏/双层悬浮底栏/Snackbar），
 * 内容区按页签切换。默认落在总览页。
 *
 * 底栏为双层悬浮圆角条（KernelSU 风格）：
 * - 第一层：添加圆钮（深色圆形）+ 总览 + 记录 + 设置；第二层：A–F 六个分类 + S 素拓。
 * - 第二层宽度 = 第一层展开宽度 + 32dp（封顶容器宽），页签等宽均分，恒定比第一层宽一圈。
 * - 两层统一 28dp 圆角（条高 56dp 的胶囊形），第一层左侧即整圆。
 * - 页面向下滑：第一层「总览/设置」向左收起、条宽收缩为正圆，
 *   第一层随第二层一同下沉、第二层额外多沉一截完全滑出屏幕，
 *   最终屏幕只剩左下角的添加圆钮；上滑反向恢复。
 * - 内容区左右划切换页签：从屏幕右侧向中间划（手指右→左）切向当前页右边的页签。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    viewModel: DeyuViewModel,
    snackbarHostState: SnackbarHostState,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onExportJson: () -> Unit,
    onExportMarkdown: () -> Unit,
    onImport: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.OVERVIEW) }
    var editorRequest by remember { mutableStateOf<EditorRequest?>(null) }

    // 系统返回键：先回总览，再退出 App
    BackHandler(enabled = selectedTab != MainTab.OVERVIEW) {
        selectedTab = MainTab.OVERVIEW
    }

    // ---------- 双层悬浮底栏：滚动时折叠/展开 ----------
    var barExpanded by remember { mutableStateOf(true) }
    var layer2HeightPx by remember { mutableIntStateOf(0) }
    // 第二层底栏宽度基准：以第一层「展开态」实测宽度 + 32dp 为准（素拓页签加入后第一层变宽，
    // 这里保证第二层恒定比第一层宽，视觉上更稳），同时不超过容器可用宽度。
    var containerWidthPx by remember { mutableIntStateOf(0) }
    var layer1WidthPx by remember { mutableIntStateOf(0) }
    val layer2Width = with(LocalDensity.current) {
        val minPx = if (layer1WidthPx > 0) layer1WidthPx + 32.dp.roundToPx()
        else 300.dp.roundToPx()
        val capPx = if (containerWidthPx > 0) containerWidthPx else minPx
        minPx.coerceAtMost(capPx).toDp()
    }
    val gapPx = with(LocalDensity.current) { 8.dp.roundToPx() }
    val bottomPadPx = with(LocalDensity.current) { 12.dp.roundToPx() }
    // 多滑的一截把 8dp 投影一并送出屏幕，避免屏幕底边残留阴影线看起来"没完全隐藏"
    val hideExtraPx = with(LocalDensity.current) { 24.dp.roundToPx() }
    val navBarsBottomPx = with(LocalDensity.current) {
        WindowInsets.navigationBars
            .asPaddingValues(this)
            .calculateBottomPadding()
            .roundToPx()
    }
    // 第一层（含添加圆钮）整体下沉：恰好落到原第二层的位置（屏幕左下角），
    // 与第二层同向同时运动，相对位置不变
    val barOffsetPx by animateIntAsState(
        targetValue = if (barExpanded) 0 else layer2HeightPx + gapPx,
        animationSpec = tween(durationMillis = 250),
        label = "bottomBarOffset"
    )
    // 第二层在第一层下沉之外再额外多沉一截，把整层（含投影）送出屏幕外
    val layer2ExtraPx by animateIntAsState(
        targetValue = if (barExpanded) 0 else bottomPadPx + navBarsBottomPx + hideExtraPx,
        animationSpec = tween(durationMillis = 250),
        label = "layer2Extra"
    )
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 用户口径的"页面下滑" = 页面内容向下滚动、往下浏览（手指上滑）→ available.y < 0 → 折叠；
                // "页面上滑" = 回滚到顶部（手指下滑）→ available.y > 0 → 展开。
                // available.y 符号与手指位移同向（手指上滑为负）。
                when {
                    available.y < -6f -> barExpanded = false
                    available.y > 6f -> barExpanded = true
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        // 顶栏/底栏自行处理系统栏 insets，内容区不再叠加一层
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // 第一行：当前页
                        Text(
                            text = when (selectedTab) {
                                MainTab.OVERVIEW -> "五育得分记录器"
                                MainTab.RECORDS -> "全部记录"
                                MainTab.SETTINGS -> "设置"
                                else -> Category.fromCode(selectedTab.name)
                                    ?.let { "${it.code} · ${it.displayName}" } ?: "五育得分记录器"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        // 第二行：当前学期
                        Text(
                            text = "当前学期：${state.selectedSemester?.name ?: "未创建"}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        // 第三行：本学期总五育 + 对综测的加分（多学期平均）
                        Text(
                            text = "总五育 ${fmtScore(state.semesterScore)}/${state.semesterCap.toInt()}" +
                                " · 综测加分 +${fmtScore(state.wuyuBonusAverage ?: 0f)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            // 双层悬浮圆角底栏（KernelSU 风格）：
            // 外层 Box 只负责留边（左右 16dp / 底部 12dp + 系统手势区），自身透明无背景，
            // 内容延伸到屏幕底，底栏浮于内容之上，圆角区外能看到下方滚动的页面内容。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { size ->
                            // 容器宽度变化（如转屏）时重置第一层基准宽度，避免第二层按旧值撑满
                            if (size.width != containerWidthPx) {
                                containerWidthPx = size.width
                                layer1WidthPx = 0
                            }
                        }
                ) {
                    // 圆钮尺寸：展开 48dp（贴左缘留呼吸感），折叠 56dp 填满整圆
                    val addButtonSize by animateDpAsState(
                        targetValue = if (barExpanded) 48.dp else 56.dp,
                        animationSpec = tween(durationMillis = 250),
                        label = "addButtonSize"
                    )
                    // 行内左右留边：展开 4dp，折叠 0dp（圆钮紧贴 Surface 左缘）
                    val rowPadH by animateDpAsState(
                        targetValue = if (barExpanded) 4.dp else 0.dp,
                        animationSpec = tween(durationMillis = 250),
                        label = "rowPadH"
                    )
                    Column {
                        // ---------- 第一层：添加圆钮 + 总览 + 设置（紧凑左对齐） ----------
                        // Surface 宽度包裹内容（不占满整宽，右侧不留空）。
                        // 折叠时总览/设置滑出后 Surface 宽度瞬收到圆钮尺寸，
                        // 圆钮 48→56dp 的尺寸动画再驱动条宽平滑张成正圆。
                        // （不能用 animateContentSize：它首次测量会吃满父约束，wrap 失效）
                        // 颜色用 surfaceContainerLow：比背景深一档，wrap 后的边界清晰可辨
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(56.dp)
                                .onSizeChanged { size ->
                                    // 只取展开态宽度（折叠态是 56dp 正圆，不能作为基准）
                                    if (barExpanded) {
                                        layer1WidthPx = maxOf(layer1WidthPx, size.width)
                                    }
                                }
                                .graphicsLayer { translationY = barOffsetPx.toFloat() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = rowPadH),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 添加按钮：深色圆形，视觉上突出于整条底栏
                                Box(
                                    modifier = Modifier
                                        .size(addButtonSize)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable {
                                            // 分类页打开时预设该大类；总览/设置页不预设
                                            editorRequest = EditorRequest(
                                                Category.fromCode(selectedTab.name),
                                                null,
                                                null
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "添加记录",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                // 总览 / 设置：折叠时向左收进圆钮。
                                // 用 shrinkHorizontally 而非 slideOut：slideOut 只做绘制位移、
                                // exit 期间内容仍占布局，条宽要等 250ms 动画结束才收缩（观感"延迟"）；
                                // shrink 让占位宽度同步收窄，Surface 的 wrap 宽度跟着平滑收缩，
                                // 与圆钮放大、两层下沉完全同一时刻开始、同一时长结束。
                                AnimatedVisibility(
                                    visible = barExpanded,
                                    enter = expandHorizontally(tween(250)) +
                                        fadeIn(tween(120)),
                                    exit = shrinkHorizontally(tween(250)) +
                                        fadeOut(tween(120))
                                ) {
                                    TopTabItem(
                                        tab = MainTab.OVERVIEW,
                                        selected = selectedTab == MainTab.OVERVIEW,
                                        onClick = { selectedTab = MainTab.OVERVIEW }
                                    )
                                }
                                AnimatedVisibility(
                                    visible = barExpanded,
                                    enter = expandHorizontally(tween(250)) +
                                        fadeIn(tween(120)),
                                    exit = shrinkHorizontally(tween(250)) +
                                        fadeOut(tween(120))
                                ) {
                                    TopTabItem(
                                        tab = MainTab.RECORDS,
                                        selected = selectedTab == MainTab.RECORDS,
                                        onClick = { selectedTab = MainTab.RECORDS }
                                    )
                                }
                                AnimatedVisibility(
                                    visible = barExpanded,
                                    enter = expandHorizontally(tween(250)) +
                                        fadeIn(tween(120)),
                                    exit = shrinkHorizontally(tween(250)) +
                                        fadeOut(tween(120))
                                ) {
                                    TopTabItem(
                                        tab = MainTab.SETTINGS,
                                        selected = selectedTab == MainTab.SETTINGS,
                                        onClick = { selectedTab = MainTab.SETTINGS }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // ---------- 第二层：A–F + S 七个分类（比第一层宽，页签等宽均匀分布） ----------
                        // 宽度取「第一层展开宽度 + 32dp」（不超过容器），页签用 weight(1f) 等分，
                        // 这样第二层恒定比第一层宽一圈，且 A–S 之间的间距完全均匀。
                        // 下沉量 = 第一层同款位移（落到屏幕外）+ 额外一截（连 8dp 投影一起送出屏幕）。
                        // 少了第一份位移就会只沉出半个条高，"没完全收起"
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .width(layer2Width)
                                .height(56.dp)
                                .onSizeChanged { layer2HeightPx = it.height }
                                .graphicsLayer {
                                    translationY = (barOffsetPx + layer2ExtraPx).toFloat()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    // 页签不贴 Surface 边缘：留 6dp 内边距，
                                    // 否则选中 A/S 时药丸会压到/超出第二层圆角边框
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                bottomTabs.forEach { tab ->
                                    BottomTabItem(
                                        tab = tab,
                                        selected = tab == selectedTab,
                                        onClick = { selectedTab = tab },
                                        // 等分宽度：每个页签占同样的槽位，间距天然均匀
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // 内容区只避让顶栏，底部直接延伸到屏幕底：
        // 悬浮底栏浮于内容之上，圆角区外是透明的、能看到下方滚动的页面内容
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .pointerInput(Unit) {
                    // 左右划切换页签：与 ViewPager 一致——
                    // 手指从屏幕右侧向中间划（totalX < 0）切向当前页右边的页签，
                    // 手指从屏幕左侧向中间划（totalX > 0）切向当前页左边的页签。
                    // 纵向滑动仍归 LazyColumn，这里只在水平位移占优且超过阈值时才出手。
                    val threshold = 60.dp.toPx()
                    var totalX = 0f
                    var totalY = 0f
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            totalX = 0f
                            totalY = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    val idx = swipeTabs.indexOf(selectedTab)
                                    if (idx >= 0 && abs(totalX) > threshold && abs(totalX) > abs(totalY)) {
                                        val nextIdx = if (totalX < 0) idx + 1 else idx - 1
                                        if (nextIdx in swipeTabs.indices) {
                                            selectedTab = swipeTabs[nextIdx]
                                        }
                                    }
                                    break
                                }
                                totalX += change.positionChange().x
                                totalY += change.positionChange().y
                            }
                        }
                    }
                }
        ) {
            // 左右划切换：按页签顺序做翻页动画（新页从对应方向滑入），
            // 滑动过程中旧页同步淡出，clipToBounds 防止滑动的页面溢出盖住顶栏
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    // 新页从对应方向滑入：切向右边页签时从右侧进、旧页往左退，反之亦然。
                    // SlideDirection 是 AnimatedContentTransitionScope 的嵌套类（Compose 1.10+
                    // 起不再是顶级枚举），隐式接收者作用域里引用不到，需写全限定名
                    val forward = targetState.ordinal > initialState.ordinal
                    val dir = if (forward) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    (
                        slideIntoContainer(dir, animationSpec = tween(240)) +
                            fadeIn(animationSpec = tween(240))
                        ) togetherWith (
                        slideOutOfContainer(dir, animationSpec = tween(240)) +
                            fadeOut(animationSpec = tween(240))
                        )
                },
                modifier = Modifier.fillMaxSize().clipToBounds(),
                label = "pageSwitch"
            ) { tab ->
                when (tab) {
                    MainTab.OVERVIEW -> OverviewScreen(viewModel = viewModel)

                    MainTab.RECORDS -> RecordsScreen(
                        viewModel = viewModel,
                        onEditRecord = { record ->
                            // 按记录所属门类反查大类，编辑时大类 chip 默认选中它
                            val recordCategory =
                                SubcategoryCatalog.byId(record.subcategoryId)?.category
                            editorRequest = EditorRequest(
                                recordCategory,
                                record.subcategoryId,
                                record
                            )
                        }
                    )

                    MainTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        onExportJson = onExportJson,
                        onExportMarkdown = onExportMarkdown,
                        onImport = onImport
                    )

                    else -> {
                        val category = Category.fromCode(tab.name) ?: Category.A
                        CategoryDetailScreen(
                            category = category,
                            viewModel = viewModel,
                            onAddRecord = { presetSubcategoryId ->
                                editorRequest = EditorRequest(category, presetSubcategoryId, null)
                            },
                            onEditRecord = { record ->
                                // 按记录所属门类反查大类，编辑时大类 chip 默认选中它
                                val recordCategory =
                                    SubcategoryCatalog.byId(record.subcategoryId)?.category
                                        ?: category
                                editorRequest =
                                    EditorRequest(recordCategory, record.subcategoryId, record)
                            }
                        )
                    }
                }
            }
        }
    }

    editorRequest?.let { request ->
        RecordEditorSheet(
            category = request.category,
            presetSubcategoryId = request.presetSubcategoryId,
            existingRecord = request.existingRecord,
            semesterId = state.selectedSemesterId,
            onDismiss = { editorRequest = null },
            onSave = { record ->
                if (request.existingRecord != null) {
                    viewModel.updateRecord(record)
                } else {
                    viewModel.addRecord(record)
                }
                editorRequest = null
            }
        )
    }
}

/**
 * 第一层页签（总览/设置）：图标 + 文字横排，选中态垫 secondaryContainer 浅色药丸。
 */
@Composable
private fun TopTabItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = when (tab) {
                MainTab.OVERVIEW -> Icons.Default.Home
                MainTab.RECORDS -> Icons.Default.List
                else -> Icons.Default.Settings
            },
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = tab.label,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}

/**
 * 第二层单个分类页签：字母槽是固定的 40×28dp 药丸（选中时垫 secondaryContainer 浅色底），
 * 字母用主色（浅色主题为深青、深色主题为亮蓝），与浅色药丸对比清晰，不会被盖住。
 */
@Composable
private fun BottomTabItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                // 素拓页签加入后第二层有 7 个槽位，药丸收窄到 36dp 才能让页签之间留出均匀间距
                .width(36.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tab.name,
                // 矮胖粗体：Black 字重 + 大字号 + 略宽字距，字母更浑厚醒目
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = contentColor
            )
        }
        Text(
            text = tab.label,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}
