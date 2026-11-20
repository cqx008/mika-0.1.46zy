package com.wangzan101.mika.ui

import com.wangzan101.mika.R
import com.wangzan101.mika.account.MikaAccount
import com.wangzan101.mika.i18n.tr
import com.wangzan101.mika.i18n.trPlural
import com.wangzan101.mika.ui.MikaAccountPanelActions
import com.wangzan101.mika.ui.MikaAccountPanelSheet
import com.wangzan101.mika.ui.MIKA_AVATAR_PRESETS
import com.wangzan101.mika.ui.openSensenovaRegister
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wangzan101.mika.ui.components.GroupCard
import com.wangzan101.mika.ui.components.ListRow
import com.wangzan101.mika.ui.components.RowValue
import com.wangzan101.mika.ui.components.RowChevron
import com.wangzan101.mika.ui.components.ValueTone
import com.wangzan101.mika.ui.components.PageScaffold
import com.wangzan101.mika.ui.components.GroupLabel
import com.wangzan101.mika.ui.components.FooterNote
import com.wangzan101.mika.ui.theme.MikaIcons
import com.wangzan101.mika.ui.theme.MikaType
import com.wangzan101.mika.ui.theme.MikaTheme
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wangzan101.mika.settings.ProviderConnection
import android.app.ActivityManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import com.wangzan101.mika.ui.components.MikaDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wangzan101.mika.BuildConfig
import com.wangzan101.mika.cap.sandbox.SandboxState
import com.wangzan101.mika.capsule.CapsuleMode
import com.wangzan101.mika.capsule.CapsuleSetup
import com.wangzan101.mika.cap.phone.a11y.ScreenAccessibility
import com.wangzan101.mika.core.agent.SpeedFlags
import com.wangzan101.mika.core.cap.sandbox.SandboxStatus
import com.wangzan101.mika.core.model.ModelRole
import com.wangzan101.mika.core.shell.BuiltinShellStatus
import com.wangzan101.mika.record.RunStore
import com.wangzan101.mika.settings.AccessibilityState
import com.wangzan101.mika.settings.FeatureFlags
import com.wangzan101.mika.settings.Prerequisites
import com.wangzan101.mika.settings.Problem
import com.wangzan101.mika.settings.ProviderStore
import com.wangzan101.mika.settings.NetProxyStore
import com.wangzan101.mika.settings.WebSearchConfig
import com.wangzan101.mika.settings.WebSearchSettings
import com.wangzan101.mika.cap.web.WebProxy
import com.wangzan101.mika.settings.SystemStatus
import com.wangzan101.mika.settings.notificationOverview
import com.wangzan101.mika.task.ParallelCapability
import com.wangzan101.mika.ui.components.ButtonKind
import com.wangzan101.mika.ui.components.ButtonSize
import com.wangzan101.mika.ui.components.GroupCard
import com.wangzan101.mika.ui.components.GroupLabel
import com.wangzan101.mika.ui.components.ListRow
import com.wangzan101.mika.ui.components.MikaButton
import com.wangzan101.mika.ui.components.MikaSwitch
import com.wangzan101.mika.ui.components.PageScaffold
import com.wangzan101.mika.ui.components.PillTone
import com.wangzan101.mika.ui.components.RowChevron
import com.wangzan101.mika.ui.components.RowValue
import com.wangzan101.mika.ui.components.StateCard
import com.wangzan101.mika.ui.components.StatusPill
import com.wangzan101.mika.ui.components.TileTone
import com.wangzan101.mika.i18n.AppLanguage
import com.wangzan101.mika.wechat.WechatConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SHIZUKU_SETUP_GUIDE_URL = "https://shizuku.rikka.app/guide/setup/"

/** 设置里能进入的子页面;[Home] 是设置首页,[Advanced] 集中放置连接、维护和工程能力。 */
private enum class SettingsPage { Home, Advanced, AppAccess, SmsLogin, PhoneOps, Calls, Wechat, DuringWork, AppPace, Capsule, Language, Notifications, Recommendations, Checklists, Workflows, Sandbox, Plugins, EnvVars, Recovery, SelfCheck, ComputerAccess, McpServers, Root, Developer, DeviceDirectories, RunStats, Proactive, Backup, MikaAccount }

/**
 * 设置页:总状态卡 + 常用 / 我的数据 / 其他。高级与其他只增加一层,各子页返回来时的页面。
 * 模型页由 [onOpenModels]、记忆与数据由 [onOpenMemory]、关于页由 [onOpenAbout] 交给 Activity。
 * 修复动作由 [onFix] 交给 Activity 去做(请求权限、跳转系统设置)。
 */
@Composable
fun SettingsScreen(
    store: ProviderStore,
    status: SystemStatus,
    androidRelease: String,
    onFix: (Fix) -> Unit,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenMemory: (() -> Unit)? = null,
    onOpenBrowser: () -> Unit = {},
    onOpenFiles: () -> Unit = {},
    onOpenPersonal: () -> Unit = {},
    onOpenPlans: () -> Unit = {},
    /** 任务运行里的「自主模式」:当前值和换它的办法。 */
    autonomous: Boolean = false,
    onAutonomous: (Boolean) -> Unit = {},
    /** 查到的新版本号;有它时「关于 Mika」一行写「x 可更新」带红点。 */
    updateVersion: String? = null,
    onOpenAbout: () -> Unit = {},
    /** 外部 MCP server 设置(outbound);为空时不显示入口。 */
    mcpServers: com.wangzan101.mika.settings.McpServerStore? = null,
    /** 人格设置;为空时不显示入口。 */
    onOpenPersonas: (() -> Unit)? = null,
    openPhoneOps: Boolean = false,
    onPhoneOpsOpened: () -> Unit = {},
    /** 自有实现:报错工单/反馈:直达官方反馈面板(发工单:机型 + 任务日志 + 诊断, 后台「报错工单」管理)。 */
    onOpenFeedback: () -> Unit = {},
    /** 自有实现:反馈与建议:发一条纯文字的意见建议/咨询/其他(不带日志诊断), 后台「反馈建议」管理。 */
    onOpenSuggestion: () -> Unit = {},
) {
    val configs by store.configs.collectAsState()
    val roles by store.roles.collectAsState()
    // 开发者选项里的「看图定位已验证」跟着看图模型(看图用主模型时是主模型那条配置)
    val visionConfig = remember(configs, roles) { store.configFor(ModelRole.Vision) }
    var page by rememberSaveable { mutableStateOf(if (openPhoneOps) SettingsPage.PhoneOps else SettingsPage.Home) }
    var explaining by remember { mutableStateOf<Problem?>(null) }
    // 「完全退出」先问一句:任务、胶囊和通知都会停
    var confirmingQuit by remember { mutableStateOf(false) }

    LaunchedEffect(openPhoneOps) {
        if (openPhoneOps) {
            page = SettingsPage.PhoneOps
            onPhoneOpsOpened()
        }
    }

    fun fix(f: Fix) = when (f) {
        Fix.AddModel -> onOpenModels()
        Fix.OpenPhoneOps -> page = SettingsPage.PhoneOps
        else -> onFix(f)
    }
    val onProblem: (Problem) -> Unit = { p -> StatusCopy.directFix(p)?.let(::fix) ?: run { explaining = p } }

    // 并行任务的上限只在实验开关 parallelTasks 打开时出现;设备能力按此刻算,第二块屏建失败过就按失败算
    val context = LocalContext.current
    val recommendedTasks = remember { (context.applicationContext as com.wangzan101.mika.MikaApp).recommendedTasks }
    val savedChecklists = remember { (context.applicationContext as com.wangzan101.mika.MikaApp).savedChecklists }
    val workflows = remember { (context.applicationContext as com.wangzan101.mika.MikaApp).workflows }
    val shellManager = remember { com.wangzan101.mika.cap.phone.PhoneServices.of(context).shellManager }
    val root = remember { shellManager.root }
    val rootState by root.state.collectAsState()
    LaunchedEffect(Unit) { root.refresh() }
    val wechat = remember { (context.applicationContext as com.wangzan101.mika.MikaApp).wechat }
    val wechatState by wechat.state.collectAsState()
    val flags = remember { FeatureFlags.get(context) }
    var personalStart by remember { mutableStateOf(flags.personalStart()) }
    var parallelMax by remember { mutableIntStateOf(flags.parallelMax()) }
    var keepScreenOn by remember { mutableStateOf(flags.keepScreenOn()) }
    val pocketPrefs = remember { context.getSharedPreferences(com.wangzan101.mika.pocket.PocketGuardManager.PREFS, 0) }
    var pocketGuard by remember { mutableStateOf(pocketPrefs.getBoolean(com.wangzan101.mika.pocket.PocketGuardManager.ENABLED, false)) }
    var pocketCompletionLock by remember { mutableStateOf(pocketPrefs.getBoolean(com.wangzan101.mika.pocket.PocketGuardManager.LOCK_ON_COMPLETION, false)) }
    var showTaskCost by remember { mutableStateOf(flags.showTaskCost()) }
    var muteTarget by remember { mutableStateOf(flags.muteTarget()) }
    var operatingPace by remember { mutableStateOf(flags.operatingPace()) }
    var stepLimit by remember { mutableStateOf(flags.taskStepLimit()) }
    var triggerRunMinutes by remember { mutableStateOf(flags.triggerRunMinutes()) }
    var tokenBudget by remember { mutableStateOf(flags.taskTokenBudget()) }
    var selfConfig by remember { mutableStateOf(flags.selfConfig()) }
    var selfOperate by remember { mutableStateOf(flags.selfOperate()) }
    var deviceFiles by remember { mutableStateOf(flags.deviceFiles()) }
    var pipOperate by remember { mutableStateOf(flags.pipOperate()) }
    var externalSchedule by remember { mutableStateOf(flags.externalSchedule()) }
    var autoArchiveDays by remember { mutableStateOf(flags.autoArchiveDays()) }
    var editingAutoArchive by remember { mutableStateOf(false) }
    val capsuleMode by remember(flags) { flags.capsuleModeChanges() }.collectAsState(flags.capsuleMode())
    var capsuleIdleOpacity by remember { mutableStateOf(flags.capsuleIdleOpacity()) }
    var capsuleCardScale by remember { mutableStateOf(flags.capsuleCardScale()) }
    // 换语言时界面会重建,这里每次进来读一次即可
    val language = remember { AppLanguage.current(context) }
    val runtimeLimit by (context.applicationContext as com.wangzan101.mika.MikaApp).taskState.parallelLimit.collectAsState()
    var speed by remember { mutableStateOf(flags.speedFlags()) }
    val scope = rememberCoroutineScope()
    val sandboxStatus by remember { SandboxState.get(context) }.status.collectAsState()
    // 自有实现:mika 自有账号(商汤预设服务商)的会话;不依赖商汤预设服务商存在
    val mikaAccountVm = remember(scope) { ModelSetupViewModel(store, scope, mikaContextArg = context.applicationContext) }
    var mikaAccountSession by remember { mutableStateOf(mikaAccountVm.mikaSession()) }
    // 网络代理:只在这里改;读设置要解密、看浏览器支不支持要加载 WebView,都放到后台
    val proxies = remember { NetProxyStore.get(context) }
    var proxy by remember { mutableStateOf<String?>(null) }
    var browserProxy by remember { mutableStateOf<WebProxy.Mode?>(null) }
    var editingProxy by remember { mutableStateOf(false) }
    LaunchedEffect(proxy) {
        if (proxy == null) proxy = withContext(Dispatchers.IO) { proxies.get() }
        browserProxy = withContext(Dispatchers.IO) { WebProxy.mode() }
    }
    val searchSettings = remember { WebSearchSettings.get(context) }
    var webSearch by remember { mutableStateOf<WebSearchConfig?>(null) }
    var editingSearch by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { webSearch = withContext(Dispatchers.IO) { searchSettings.get() } }
    val mcp = remember { (context.applicationContext as com.wangzan101.mika.MikaApp).mcp }
    val mcpNetworkOn by mcp.settings.enabled.collectAsState()
    val mcpUsbOn by mcp.usb.enabled.collectAsState()
    val mcpOn = mcpNetworkOn || mcpUsbOn
    val pluginRegistry = remember { (context.applicationContext as com.wangzan101.mika.MikaApp).plugins }
    val pluginRecords by pluginRegistry.plugins.collectAsState()
    val parallel = if (!speed.parallelTasks) null else ParallelSetting(
        parallelMax,
        ParallelCapability.detect(
            status.sdkInt, context.getSystemService(ActivityManager::class.java).isLowRamDevice, parallelMax,
            secondDisplayFailed = runtimeLimit.reason == ParallelCapability.SECOND_DISPLAY_FAILED,
        ),
    )
    // 开发者选项只在调试版和性能测试版里列出:发布版的各开关按评测结果统一定默认值。
    // 主动能力不在里面:入口在设置首页,每个版本都有,默认关着
    val showDeveloper = com.wangzan101.mika.record.DetailedTraces.supportedBuild
    val toHome = { page = SettingsPage.Home }
    val toAdvanced = { page = SettingsPage.Advanced }
    val back = {
        page = when (page) {
            SettingsPage.RunStats, SettingsPage.DeviceDirectories -> SettingsPage.Developer
            SettingsPage.SelfCheck -> SettingsPage.Recovery
            SettingsPage.Sandbox, SettingsPage.Recovery, SettingsPage.ComputerAccess,
            SettingsPage.McpServers, SettingsPage.Root, SettingsPage.Developer, SettingsPage.EnvVars -> SettingsPage.Advanced
            SettingsPage.AppPace -> SettingsPage.DuringWork
            else -> SettingsPage.Home
        }
    }

    if (page != SettingsPage.Home) BackHandler { back() }

    val visiblePage = when {
        !showDeveloper && (page == SettingsPage.Developer || page == SettingsPage.DeviceDirectories || page == SettingsPage.RunStats) -> SettingsPage.Home
        page == SettingsPage.Root && !rootState.suPresent -> SettingsPage.Advanced
        else -> page
    }
    when (visiblePage) {
        SettingsPage.Home -> SettingsContent(
            status = status, androidRelease = androidRelease,
            mainModel = roles.main?.let { id -> configs.firstOrNull { it.id == id } }?.let(ProviderConnection::displayName),
            onBack = onBack, onProblem = onProblem, onOpenModels = onOpenModels,
            onOpenPhoneOps = { page = SettingsPage.PhoneOps },
            onOpenCalls = { page = SettingsPage.Calls },
            wechatState = wechatState, wechatBound = wechat.store.account() != null,
            onOpenWechat = { page = SettingsPage.Wechat },
            onOpenProactive = { page = SettingsPage.Proactive },
            onOpenAppAccess = { page = SettingsPage.AppAccess },
            onOpenSmsLogin = { page = SettingsPage.SmsLogin },
            onOpenDuringWork = { page = SettingsPage.DuringWork },
            capsuleMode = capsuleMode, onOpenCapsule = { page = SettingsPage.Capsule },
            onOpenNotifications = { page = SettingsPage.Notifications },
            language = language, onOpenLanguage = { page = SettingsPage.Language },
            personalStart = personalStart,
            onPersonalStart = { flags.setPersonalStart(it); personalStart = it },
            autoArchiveDays = autoArchiveDays,
            onOpenAutoArchive = { editingAutoArchive = true },
            onOpenMemory = onOpenMemory,
            onOpenBackup = { page = SettingsPage.Backup },
            onOpenRecommendations = { page = SettingsPage.Recommendations },
            onOpenPersonas = onOpenPersonas,
            onOpenAdvanced = { page = SettingsPage.Advanced },
            onOpenBrowser = onOpenBrowser,
            onOpenFiles = onOpenFiles,
            onOpenPersonal = onOpenPersonal,
            onOpenPlans = onOpenPlans,
            mikaAccount = mikaAccountSession,
            onOpenMikaAccount = { page = SettingsPage.MikaAccount },
            versionName = BuildConfig.VERSION_NAME, updateVersion = updateVersion, onOpenAbout = onOpenAbout,
        )
        SettingsPage.Advanced -> AdvancedSettingsContent(
            proxy = proxy, proxyNote = proxy?.let { NetProxyCopy.subtitle(it, browserProxy) }, onOpenProxy = { editingProxy = true },
            webSearch = webSearch, onOpenWebSearch = { editingSearch = true },
            sandbox = sandboxStatus, onOpenSandbox = { page = SettingsPage.Sandbox },
            pluginsCount = pluginRecords.size, onOpenPlugins = { page = SettingsPage.Plugins },
            onOpenEnvVars = { page = SettingsPage.EnvVars },
            showRecovery = status.sdkInt >= Prerequisites.MIN_SDK, onOpenRecovery = { page = SettingsPage.Recovery },
            computerAccess = mcpOn, onOpenComputerAccess = { page = SettingsPage.ComputerAccess },
            mcpServersCount = mcpServers?.servers?.value?.size, onOpenMcpServers = { page = SettingsPage.McpServers },
            showRoot = rootState.suPresent, rootCommands = rootState.commands, onOpenRoot = { page = SettingsPage.Root },
            showDeveloper = showDeveloper, onOpenDeveloper = { page = SettingsPage.Developer },
            onBack = toHome,
            onQuit = { confirmingQuit = true },
        )
        SettingsPage.AppAccess -> AppAccessScreen(onBack = toHome)
        SettingsPage.SmsLogin -> SmsLoginSettingsScreen(onBack = toHome)
        SettingsPage.Wechat -> WechatSettingsScreen(wechat, onBack = toHome)
        SettingsPage.PhoneOps -> PhoneOpsScreen(
            status, androidRelease, onProblem,
            onRepair = { onFix(Fix.RepairPhoneHelper) }, onUnpair = { onFix(Fix.UnpairPhoneHelper) }, onBack = toHome,
            onSelectTransport = { shellManager.engine.selectTransport(it) },
            onGrantShizuku = {
                if (!shellManager.shizuku.openGrantPermissionOrApp(context)) {
                    openInBrowser(context, SHIZUKU_SETUP_GUIDE_URL)
                }
            },
            onOpenShizukuGuide = { openInBrowser(context, SHIZUKU_SETUP_GUIDE_URL) },
            onRefreshShizuku = { shellManager.refreshShizukuAsync() },
        )
        SettingsPage.Calls -> CallSettingsScreen((context.applicationContext as com.wangzan101.mika.MikaApp).calls, onBack = toHome)
        SettingsPage.Notifications -> NotificationsScreen(status, onFix = ::fix, onBack = back)
        SettingsPage.DuringWork -> DuringWorkScreen(
            keepScreenOn, onKeepScreenOn = { flags.setKeepScreenOn(it); keepScreenOn = it },
            muteTarget, onMuteTarget = { flags.setMuteTarget(it); muteTarget = it },
            parallel, onParallelMax = { flags.setParallelMax(it); parallelMax = it }, onBack = toHome,
            autonomous = autonomous, onAutonomous = onAutonomous,
            operatingPace = operatingPace, onOperatingPace = { flags.setOperatingPace(it); operatingPace = it },
            stepLimit = stepLimit, onStepLimit = { flags.setTaskStepLimit(it); stepLimit = it },
            triggerRunMinutes = triggerRunMinutes, onTriggerRunMinutes = { flags.setTriggerRunMinutes(it); triggerRunMinutes = it },
            tokenBudget = tokenBudget, onTokenBudget = { flags.setTaskTokenBudget(it); tokenBudget = it },
            showTaskCost = showTaskCost, onShowTaskCost = { flags.setShowTaskCost(it); showTaskCost = it },
            pocketGuard = pocketGuard, onPocketGuard = { pocketPrefs.edit().putBoolean(com.wangzan101.mika.pocket.PocketGuardManager.ENABLED, it).apply(); pocketGuard = it },
            pocketCompletionLock = pocketCompletionLock, onPocketCompletionLock = { pocketPrefs.edit().putBoolean(com.wangzan101.mika.pocket.PocketGuardManager.LOCK_ON_COMPLETION, it).apply(); pocketCompletionLock = it },
            selfConfig = selfConfig, onSelfConfig = { flags.setSelfConfig(it); selfConfig = it },
            selfOperate = selfOperate, onSelfOperate = { flags.setSelfOperate(it); selfOperate = it },
            pipOperate = pipOperate, onPipOperate = { flags.setPipOperate(it); pipOperate = it },
            externalSchedule = externalSchedule, onExternalSchedule = { flags.setExternalSchedule(it); externalSchedule = it },
            onOpenAppPace = { page = SettingsPage.AppPace },
        )
        SettingsPage.AppPace -> AppPaceScreen(operatingPace, flags, onBack = { page = SettingsPage.DuringWork })
        SettingsPage.Language -> LanguageScreen(language, onLanguage = { AppLanguage.set(context, it) }, onBack = toHome)
        SettingsPage.Recommendations -> RecommendedTasksScreen(recommendedTasks, onBack = toHome,
            onOpenChecklists = { page = SettingsPage.Checklists },
            onOpenWorkflows = { page = SettingsPage.Workflows })
        SettingsPage.Checklists -> SavedChecklistsScreen(savedChecklists, onBack = toHome)
        SettingsPage.Workflows -> WorkflowsScreen(workflows, onBack = toHome)
        SettingsPage.Capsule -> CapsuleScreen(
            capsuleMode, onMode = {
                flags.setCapsuleMode(it)
                if (CapsuleSetup.needsAccessibility(it, ScreenAccessibility.instance.value != null)) onFix(Fix.OpenAccessibility)
            }, onBack = toHome,
            idleOpacity = capsuleIdleOpacity,
            onIdleOpacity = { flags.setCapsuleIdleOpacity(it); capsuleIdleOpacity = it },
            cardScale = capsuleCardScale,
            onCardScale = { flags.setCapsuleCardScale(it); capsuleCardScale = it },
        )
        SettingsPage.Plugins -> PluginsScreen(pluginRegistry, onBack = toHome)
        SettingsPage.EnvVars -> EnvVarSettingsScreen(remember { com.wangzan101.mika.settings.EnvVarStore.get(context) }, onBack = toAdvanced)
        SettingsPage.Sandbox -> SandboxScreen(onBack = toAdvanced)
        SettingsPage.Recovery -> RecoveryScreen(onBack = toAdvanced, onOpenSelfCheck = { page = SettingsPage.SelfCheck })
        SettingsPage.SelfCheck -> SelfCheckScreen(status, onBack = back)
        SettingsPage.ComputerAccess -> McpScreen(mcp, onBack = toAdvanced)
        SettingsPage.McpServers -> mcpServers?.let { McpServersScreen(it, onBack = toAdvanced) }
        SettingsPage.Root -> RootScreen(helperRow = status.sdkInt >= Prerequisites.MIN_SDK, onBack = toAdvanced)
        SettingsPage.Developer -> DeveloperOptionsScreen(
            speed, onSpeed = { flags.setSpeed(it); speed = it }, onBack = toAdvanced,
            deviceFiles = deviceFiles, onDeviceFiles = { flags.setDeviceFiles(it); deviceFiles = it },
            grounding = visionConfig?.let { GroundingSetting(it.model, it.capabilities.groundingVerified) },
            onGrounding = { verified -> scope.launch { withContext(Dispatchers.IO) { store.setVisionGroundingVerified(verified) } } },
            onOpenRunStats = { page = SettingsPage.RunStats },
            onDeviceDirectories = { page = SettingsPage.DeviceDirectories },
        )
        SettingsPage.DeviceDirectories -> DeviceDirectoriesScreen(
            remember { com.wangzan101.mika.settings.DeviceDirectoryStore.get(context) },
            remember { com.wangzan101.mika.cap.phone.PhoneServices.of(context).hub },
            onBack = { page = SettingsPage.Developer },
        )
        SettingsPage.RunStats -> RunStatsScreen(remember { RunStore(context).metrics }, onBack = back)
        SettingsPage.Proactive -> com.wangzan101.mika.proactive.ProactiveScreen(onBack = back)
        SettingsPage.Backup -> BackupScreen(onBack = toHome)
        SettingsPage.MikaAccount -> {
            // 退出/改资料/填 key 后重新读会话,让账号页立即反映最新状态
            fun refreshSession() { mikaAccountSession = mikaAccountVm.mikaSession() }
            fun saveSensenovaKey(k: String): Boolean {
                val ok = mikaAccountVm.mikaSetSensenovaKey(k)
                refreshSession()
                return ok
            }
            var editingSensenovaKey by remember { mutableStateOf(false) }
            var showMikaLoginPanel by remember { mutableStateOf(false) }
            val mikaUi by mikaAccountVm.state.collectAsState()
            val mikaPanelActions = remember(mikaAccountVm) { MikaAccountPanelActions(
                login = { email, pwd -> mikaAccountVm.mikaLogin(email, pwd) },
                register = { email, pwd, nick, avatar -> mikaAccountVm.mikaRegister(email, pwd, nick, avatar) },
                dismiss = { showMikaLoginPanel = false },
                openSensenova = { openSensenovaRegister(context) },
            ) }
            MikaAccountSettingsContent(
                session = mikaAccountSession,
                hasSensenovaKey = store.mikaProvider()?.id?.let { store.hasKey(it) } ?: false,
                onBack = toHome,
                onSaveProfile = { nick, avatar, avatarPreset ->
                    mikaAccountVm.mikaSaveProfile(nick, avatar, avatarPreset)
                    refreshSession()
                },
                onSensenovaKey = { key -> saveSensenovaKey(key) },
                onSignOut = {
                    mikaAccountVm.mikaSignOut()
                    refreshSession()
                },
                onOpenSensenova = { openSensenovaRegister(context) },
                onEditSensenovaKey = { editingSensenovaKey = true },
                onSignIn = { showMikaLoginPanel = true },
            )
            // 模型页「填写/修改商汤 key」弹框(和设置页账号页同一个 KeyEditDialog,复用 onSensenovaKey 保存路径)
            if (editingSensenovaKey) {
                var key by remember { mutableStateOf(mikaAccountSession?.sensenovaKey.orEmpty()) }
                val keyFilled = mikaAccountSession?.sensenovaKey?.isNotBlank() == true || store.mikaProvider()?.id?.let { store.hasKey(it) } ?: false
                KeyEditDialog(
                    title = stringResource(R.string.mika_sensenova_key),
                    hint = stringResource(R.string.mika_sensenova_key_fill_hint),
                    value = key, onChange = { key = it },
                    secret = true,
                    allowClear = true,
                    clearText = stringResource(R.string.model_key_clear),
                    confirmText = stringResource(R.string.save),
                    enabled = key.isNotBlank() || (key.isEmpty() && keyFilled),
                    onConfirm = {
                        if (saveSensenovaKey(key.trim())) editingSensenovaKey = false
                    },
                    onDismiss = { editingSensenovaKey = false },
                )
            }
            // 未登录时点「登录/注册」弹的登录面板(复用模型页/欢迎页同一个 MikaAccountPanelSheet)
            if (showMikaLoginPanel && mikaAccountSession == null) {
                MikaAccountPanelSheet(mikaUi, initialTab = false, mikaPanelActions, onOpenSensenova = { openSensenovaRegister(context) })
                LaunchedEffect(Unit) { mikaAccountVm.state.collect { if (it.mikaSession != null || it.accountSignIn.running) showMikaLoginPanel = false; refreshSession() } }
            }
        }
    }

    if (editingProxy) NetProxyDialog(
        initial = proxy.orEmpty(),
        onDismiss = { editingProxy = false },
        onSave = { v ->
            scope.launch {
                withContext(Dispatchers.IO) { proxies.set(v) }
                proxy = v.trim()
                editingProxy = false
            }
        },
    )

    if (editingSearch) WebSearchDialog(
        initial = webSearch ?: WebSearchConfig(),
        onDismiss = { editingSearch = false },
        onSave = { config ->
            scope.launch {
                withContext(Dispatchers.IO) { searchSettings.set(config) }
                webSearch = searchSettings.get()
                editingSearch = false
            }
        },
    )

    explaining?.let { p ->
        val e = StatusCopy.explain(p, androidRelease)
        MikaDialog(
            onDismissRequest = { explaining = null },
            title = { Text(e.title) },
            text = {
                Column {
                    Text(e.body)
                    e.secondaryAction?.let { (label, destination) ->
                        TextButton(onClick = {
                            explaining = null
                            fix(destination)
                        }) { Text(label) }
                    }
                }
            },
            confirmButton = {
                if (e.action != null && e.fix != null) {
                    TextButton({ explaining = null; fix(e.fix) }) { Text(e.action) }
                } else {
                    TextButton({ explaining = null }) { Text(stringResource(R.string.ok_got_it)) }
                }
            },
            dismissButton = if (e.fix != null) ({ TextButton({ explaining = null }) { Text(stringResource(R.string.cancel)) } }) else null,
        )
    }

    if (confirmingQuit) {
        AlertDialog(
            onDismissRequest = { confirmingQuit = false },
            title = { Text(stringResource(R.string.quit_title)) },
            text = { Text(stringResource(R.string.quit_confirm_body)) },
            confirmButton = {
                TextButton({ confirmingQuit = false; com.wangzan101.mika.quit.MikaQuit.quit(context) }) {
                    Text(stringResource(R.string.quit_confirm_action), color = MikaTheme.colors.bad)
                }
            },
            dismissButton = { TextButton({ confirmingQuit = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (editingAutoArchive) {
        AutoArchiveDialog(
            current = autoArchiveDays,
            onPick = { days ->
                flags.setAutoArchiveDays(days)
                autoArchiveDays = days
                editingAutoArchive = false
                // 选完马上按新设置跑一次,老对话立刻进归档
                (context.applicationContext as com.wangzan101.mika.MikaApp).autoArchiveNow()
            },
            onDismiss = { editingAutoArchive = false },
        )
    }
}

/** 不带状态的设置首页,便于测试。首页只列常用入口,高级能力统一从 [onOpenAdvanced] 进入。 */
@Composable
fun SettingsContent(
    status: SystemStatus,
    androidRelease: String,
    /** 当前主模型的名字;没有可用的主模型时为空。 */
    mainModel: String?,
    onBack: () -> Unit,
    onProblem: (Problem) -> Unit,
    onOpenModels: () -> Unit,
    onOpenPhoneOps: () -> Unit = {},
    onOpenCalls: () -> Unit = {},
    wechatState: WechatConnectionState = WechatConnectionState.NeedsBinding,
    wechatBound: Boolean = false,
    onOpenWechat: () -> Unit = {},
    /** 「主动能力」的入口,在「微信」和「任务运行」之间。 */
    onOpenProactive: () -> Unit = {},
    onOpenAppAccess: () -> Unit = {},
    onOpenSmsLogin: () -> Unit = {},
    onOpenDuringWork: () -> Unit = {},
    /** 悬浮胶囊此刻的选择,写在「悬浮胶囊」一行的右边。 */
    capsuleMode: CapsuleMode = CapsuleMode.DEFAULT,
    onOpenCapsule: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    /** 此刻选的界面语言,写在「语言与外观」一行的右边。 */
    language: AppLanguage = AppLanguage.System,
    onOpenLanguage: () -> Unit = {},
    onOpenMemory: (() -> Unit)? = null,
    onOpenBackup: (() -> Unit)? = null,
    /** 自有实现:mika 自有账号(注册/带昵称头像那个)的本地会话;没登录时为 null。 */
    mikaAccount: com.wangzan101.mika.account.MikaAccount.Session? = null,
    /** 「账号」入口:进独立的 mika 账号页(不依赖商汤预设服务商存不存在)。 */
    onOpenMikaAccount: () -> Unit = {},
    onOpenRecommendations: () -> Unit = {},
    onOpenPersonas: (() -> Unit)? = null,
    onOpenAdvanced: () -> Unit = {},
    onOpenBrowser: () -> Unit = {},
    onOpenFiles: () -> Unit = {},
    onOpenPersonal: () -> Unit = {},
    onOpenPlans: () -> Unit = {},
    versionName: String = "",
    updateVersion: String? = null,
    onOpenAbout: () -> Unit = {},
    personalStart: Boolean = false,
    onPersonalStart: ((Boolean) -> Unit)? = null,
    /** 自动归档:多久没有新消息的对话自动归档;0 表示关闭。 */
    autoArchiveDays: Int = 0,
    onOpenAutoArchive: () -> Unit = {},
) {
    val check = Prerequisites.check(status, hasMainModel = mainModel != null)
    PageScaffold(stringResource(R.string.settings_title), onBack) {
        StatusSummary(check, onProblem)

        GroupLabel(stringResource(R.string.settings_model))
        GroupCard {
            ListRow(MikaIcons.Spark, stringResource(R.string.settings_model), stringResource(R.string.settings_model_sub), first = true, onClick = onOpenModels) {
                RowValue(mainModel ?: stringResource(R.string.settings_not_set))
                RowChevron()
            }
        }

        // 「账号」入口:mika 自有账号(昵称/头像/已登录);没登录也显示(未登录),点进去能登录/管理/退出,
        // 不依赖商汤预设服务商存不存在
        GroupLabel(stringResource(R.string.settings_account))
        GroupCard {
            val mikaSignedIn = mikaAccount != null
            val mikaName = mikaAccount?.nick?.takeIf { it.isNotBlank() } ?: mikaAccount?.email.orEmpty()
            ListRow(
                MikaIcons.Key,
                stringResource(R.string.settings_account),
                stringResource(R.string.settings_account_sub),
                first = true,
                onClick = onOpenMikaAccount,
            ) {
                RowValue(
                    if (mikaName.isNotEmpty()) mikaName else stringResource(R.string.settings_account_not_logged_in),
                    if (mikaSignedIn) ValueTone.Ok else ValueTone.Plain,
                )
                RowChevron()
            }
        }

        GroupLabel(stringResource(R.string.settings_capabilities))
        GroupCard {
            ListRow(MikaIcons.Phone, stringResource(R.string.settings_phone_ops), first = true, onClick = onOpenPhoneOps) {
                PhoneOpsPill(status)
                RowChevron()
            }
            ListRow(MikaIcons.Clock, stringResource(R.string.drawer_plans), onClick = onOpenPlans) { RowChevron() }
            ListRow(MikaIcons.Globe, stringResource(R.string.drawer_browser), onClick = onOpenBrowser) { RowChevron() }
            ListRow(MikaIcons.Paperclip, stringResource(R.string.file_library_title), onClick = onOpenFiles) { RowChevron() }
            ListRow(MikaIcons.Phone, stringResource(R.string.calls_title), stringResource(R.string.calls_entry_sub), onClick = onOpenCalls) { RowChevron() }
            ListRow(MikaIcons.Chats, stringResource(R.string.wechat_title), stringResource(R.string.wechat_settings_sub), onClick = onOpenWechat) {
                WechatEntryStatus(wechatState, wechatBound)
                RowChevron()
            }
            ListRow(MikaIcons.Ripple, stringResource(R.string.proactive_title), stringResource(R.string.proactive_entry_sub), onClick = onOpenProactive) { RowChevron() }
            if (personalStart) {
                ListRow(MikaIcons.Message, stringResource(R.string.drawer_personal), onClick = onOpenPersonal) { RowChevron() }
            }
        }

        GroupLabel(stringResource(R.string.settings_preferences))
        GroupCard {
            ListRow(MikaIcons.Moon, stringResource(R.string.settings_during_work), stringResource(R.string.settings_during_work_sub), first = true, onClick = onOpenDuringWork) { RowChevron() }
            ListRow(MikaIcons.Message, CapsuleSettingsCopy.TITLE, CapsuleSettingsCopy.SUBTITLE, onClick = onOpenCapsule) {
                RowValue(CapsuleSettingsCopy.label(capsuleMode))
                RowChevron()
            }
            ListRow(MikaIcons.Globe, stringResource(R.string.settings_language_appearance), onClick = onOpenLanguage) {
                RowValue(languageLabel(language))
                RowChevron()
            }
            val (notifText, notifTone) = StatusCopy.notificationSummary(status.notificationOverview().summary)
            ListRow(MikaIcons.Bell, StatusCopy.NOTIFICATIONS, stringResource(R.string.settings_notif_sub), onClick = onOpenNotifications) {
                RowValue(notifText, notifTone)
                RowChevron()
            }
            onPersonalStart?.let { change ->
                val label = stringResource(R.string.settings_personal_start)
                ListRow(MikaIcons.Spark, label, stringResource(R.string.settings_personal_start_sub)) {
                    MikaSwitch(personalStart, change, Modifier.semantics { contentDescription = label })
                }
            }
        }

        GroupLabel(stringResource(R.string.settings_data))
        GroupCard {
            onOpenMemory?.let {
                ListRow(MikaIcons.Heart, stringResource(R.string.memory_and_skills), stringResource(R.string.settings_memory_sub), first = true, onClick = it) { RowChevron() }
            }
            onOpenPersonas?.let {
                ListRow(MikaIcons.Spark, stringResource(R.string.persona_title), stringResource(R.string.persona_sub), first = onOpenMemory == null, onClick = it) { RowChevron() }
            }
            onOpenBackup?.let {
                ListRow(MikaIcons.Archive, stringResource(R.string.backup_title), stringResource(R.string.backup_sub), first = onOpenMemory == null && onOpenPersonas == null, onClick = it) { RowChevron() }
            }
            ListRow(
                MikaIcons.Archive,
                stringResource(R.string.settings_auto_archive),
                stringResource(R.string.settings_auto_archive_sub),
                first = onOpenMemory == null && onOpenPersonas == null && onOpenBackup == null,
                onClick = onOpenAutoArchive,
            ) {
                RowValue(
                    if (autoArchiveDays <= 0) stringResource(R.string.state_off)
                    else pluralStringResource(R.plurals.settings_auto_archive_days, autoArchiveDays, autoArchiveDays),
                )
                RowChevron()
            }
            ListRow(
                MikaIcons.Message,
                stringResource(R.string.recommendations_title),
                stringResource(R.string.recommendations_sub),
                onClick = onOpenRecommendations,
            ) { RowChevron() }
            ListRow(MikaIcons.Shield, stringResource(R.string.appaccess_title), stringResource(R.string.appaccess_row_sub), onClick = onOpenAppAccess) { RowChevron() }
            ListRow(MikaIcons.Shield, stringResource(R.string.sms_login_title), stringResource(R.string.sms_login_default_off), onClick = onOpenSmsLogin) { RowChevron() }
        }

        GroupLabel(stringResource(R.string.settings_advanced))
        GroupCard {
            ListRow(MikaIcons.Wrench, stringResource(R.string.settings_advanced), stringResource(R.string.settings_advanced_sub), first = true, onClick = onOpenAdvanced) { RowChevron() }
        }
        GroupCard {
            ListRow(MikaIcons.Info, stringResource(R.string.settings_about), first = true, onClick = onOpenAbout) {
                TrailingValue(AboutCopy.settingsValue(updateVersion, versionName))
                RowChevron()
            }
        }
    }
}

/** 高级与其他:网络与连接、维护、开发者三组;各能力仍使用原来的完整子页。 */
@Composable
fun AdvancedSettingsContent(
    onBack: () -> Unit,
    proxy: String? = null,
    proxyNote: String? = null,
    onOpenProxy: () -> Unit = {},
    webSearch: WebSearchConfig? = null,
    onOpenWebSearch: () -> Unit = {},
    sandbox: SandboxStatus? = null,
    onOpenSandbox: () -> Unit = {},
    pluginsCount: Int? = null,
    onOpenPlugins: () -> Unit = {},
    onOpenEnvVars: () -> Unit = {},
    showRecovery: Boolean = true,
    onOpenRecovery: () -> Unit = {},
    computerAccess: Boolean? = null,
    onOpenComputerAccess: () -> Unit = {},
    mcpServersCount: Int? = null,
    onOpenMcpServers: () -> Unit = {},
    showRoot: Boolean = false,
    rootCommands: Boolean = false,
    onOpenRoot: () -> Unit = {},
    showDeveloper: Boolean = false,
    onOpenDeveloper: () -> Unit = {},
    onQuit: () -> Unit = {},
) {
    PageScaffold(stringResource(R.string.settings_advanced), onBack) {
        val sandboxRow = sandbox?.takeIf { it !is SandboxStatus.Unsupported }
        if (computerAccess != null || mcpServersCount != null || pluginsCount != null || showRoot) {
            GroupLabel(stringResource(R.string.settings_extensions))
            GroupCard {
                var first = true
                fun take(): Boolean = first.also { first = false }
                computerAccess?.let { on ->
                    ListRow(MikaIcons.Desktop, stringResource(R.string.mcp_title), first = take(), onClick = onOpenComputerAccess) {
                        RowValue(stringResource(if (on) R.string.state_on else R.string.state_off)); RowChevron()
                    }
                }
                mcpServersCount?.let { count ->
                    ListRow(MikaIcons.Link, stringResource(R.string.mcp_servers_title), first = take(), onClick = onOpenMcpServers) {
                        RowValue(stringResource(R.string.mcp_servers_count, count)); RowChevron()
                    }
                }
                pluginsCount?.let {
                    ListRow(MikaIcons.Spark, stringResource(R.string.plugins_title), first = take(), onClick = onOpenPlugins) {
                        RowValue(trPlural(R.plurals.plugins_count, it)); RowChevron()
                    }
                }
                if (showRoot) {
                    ListRow(MikaIcons.Key, PhoneSetupCopy.ROOT_GROUP, PhoneSetupCopy.rootEntryLine(rootCommands), first = take(), onClick = onOpenRoot) {
                        RowChevron()
                    }
                }
            }
        }
        GroupCard {
            ListRow(MikaIcons.Code, stringResource(R.string.env_vars_title), first = true, onClick = onOpenEnvVars) { RowChevron() }
        }
        if (proxy != null || webSearch != null) {
            GroupLabel(stringResource(R.string.settings_network))
            GroupCard {
                var first = true
                fun take(): Boolean = first.also { first = false }
                proxy?.let {
                    ListRow(MikaIcons.Link, NetProxyCopy.TITLE, proxyNote, first = take(), onClick = onOpenProxy) {
                        RowValue(NetProxyCopy.value(it))
                        RowChevron()
                    }
                }
                webSearch?.let { config ->
                    ListRow(MikaIcons.Globe, stringResource(R.string.web_search_title), stringResource(R.string.web_search_row_sub), first = take(), onClick = onOpenWebSearch) {
                        RowValue(WebSearchCopy.value(config)); RowChevron()
                    }
                }
            }
        }
        if (sandboxRow != null) {
            GroupLabel(stringResource(R.string.settings_runtime))
            GroupCard {
                ListRow(MikaIcons.Terminal, stringResource(R.string.settings_sandbox), first = true, onClick = onOpenSandbox) {
                    RowValue(sandboxValue(sandboxRow)); RowChevron()
                }
            }
        }
        if (showRecovery) {
            GroupLabel(stringResource(R.string.settings_maintenance))
            GroupCard {
                ListRow(MikaIcons.Wrench, stringResource(R.string.settings_recovery), first = true, onClick = onOpenRecovery) { RowChevron() }
            }
        }
        if (showDeveloper) {
            GroupLabel(stringResource(R.string.settings_developer_group))
            GroupCard {
                ListRow(MikaIcons.Code, stringResource(R.string.settings_developer), first = true, onClick = onOpenDeveloper) { RowChevron() }
            }
        }

        // 完全退出:和无障碍服务不同,它只是让 Mika 安静下来;下次打开 App 就回来
        GroupCard {
            ListRow(
                MikaIcons.Power, stringResource(R.string.quit_title), stringResource(R.string.quit_row_sub),
                first = true, titleColor = MikaTheme.colors.bad, onClick = onQuit,
            ) { RowChevron() }
        }
    }
}

/** 「手机操作」一行尾部的状态胶囊:无障碍和内置助手都就绪才是「可用」。 */
@Composable
private fun PhoneOpsPill(status: SystemStatus) {
    when {
        status.sdkInt < Prerequisites.MIN_SDK -> StatusPill(stringResource(R.string.pill_unsupported), PillTone.Neutral)
        status.builtinShell == BuiltinShellStatus.Running && status.accessibility == AccessibilityState.On -> StatusPill(stringResource(R.string.pill_available), PillTone.Ok)
        else -> StatusPill(stringResource(R.string.pill_needs_attention), PillTone.Warn)
    }
}

private fun sandboxValue(s: SandboxStatus): String = when (s) {
    is SandboxStatus.Ready -> tr(R.string.sandbox_ready)
    is SandboxStatus.Installing -> tr(R.string.sandbox_preparing)
    is SandboxStatus.Broken -> tr(R.string.pill_needs_attention)
    else -> tr(R.string.sandbox_not_prepared)
}

/**
 * 页面顶上的总状态:一切正常是「一切就绪」;否则变成第一个问题的提示,带修复按钮。
 * 焦点提醒改不了,只在手机操作页的「系统版本」一行里提示,不占这里。
 */
@Composable
private fun StatusSummary(check: Prerequisites.Check, onProblem: (Problem) -> Unit) {
    val problem = check.problems.firstOrNull { it != Problem.FocusMayLeak }
    if (problem == null) {
        StateCard(MikaIcons.Check, stringResource(R.string.all_ready), stringResource(R.string.settings_all_ready_detail), TileTone.Ok, Modifier.testTag("settings-status"))
    } else {
        val copy = StatusCopy.banner(problem)
        StateCard(
            MikaIcons.Exclaim, copy.title, copy.detail,
            if (problem in check.blockers) TileTone.Bad else TileTone.Warn,
            Modifier.testTag("settings-status"),
        ) {
            MikaButton(copy.link, { onProblem(problem) }, kind = ButtonKind.Tonal, size = ButtonSize.Small)
        }
    }
}

/**
 * 单行/多行输入弹框:本地账号页、商汤 key、昵称/头像编辑共用。
 */
@Composable
fun KeyEditDialog(
    title: String,
    hint: String,
    value: String,
    onChange: (String) -> Unit,
    body: (@Composable () -> Unit)? = null,
    secret: Boolean = false,
    allowClear: Boolean = false,
    clearText: String = "",
    confirmText: String,
    enabled: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MikaTheme.colors
    val keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text)
    val visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None
    MikaDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(hint, style = MikaType.Caption, color = c.ink3)
                BasicTextField(
                    value, onChange,
                    Modifier.fillMaxWidth().padding(top = 8.dp).testTag("settings-account-edit"),
                    singleLine = true,
                    textStyle = MikaType.Body.copy(color = c.ink),
                    keyboardOptions = keyboardOptions,
                    cursorBrush = SolidColor(c.accent),
                    visualTransformation = visualTransformation,
                )
                body?.invoke()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled) { Text(confirmText) }
        },
        dismissButton = {
            if (allowClear) {
                TextButton({ onChange(""); onConfirm() }) { Text(clearText.ifEmpty { stringResource(R.string.cancel) }, color = c.ink2) }
            } else {
                TextButton(onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/**
 * mika 账号(注册/带昵称头像那个)的独立管理页:
 * 显示昵称/邮箱/头像/登录状态,改昵称和头像,商汤 key 的状态,以及「退出登录」。
 */
@Composable
fun MikaAccountSettingsContent(
    session: com.wangzan101.mika.account.MikaAccount.Session?,
    hasSensenovaKey: Boolean,
    onBack: () -> Unit,
    onSaveProfile: (String, String, String) -> Unit = { _, _, _ -> },
    onSensenovaKey: (String) -> Boolean = { true },
    onSignOut: () -> Unit = {},
    onOpenSensenova: () -> Unit = {},
    onEditSensenovaKey: () -> Unit = {},
    onSignIn: () -> Unit = {},
) {
    val c = MikaTheme.colors
    val signedIn = session != null
    var editingNick by remember { mutableStateOf(false) }
    var editingAvatar by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf(false) }
    var confirmingSignOut by remember { mutableStateOf(false) }
    val keyFilled = hasSensenovaKey || session?.sensenovaKey?.isNotBlank() == true

    PageScaffold(stringResource(R.string.settings_account), onBack) {
        GroupLabel(stringResource(R.string.mika_account_title))
        GroupCard {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val preset = MIKA_AVATAR_PRESETS.firstOrNull {
                    it.id == session?.avatarPreset || (it.id == session?.avatar && session?.avatarPreset.isNullOrEmpty())
                }
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(22.dp))
                        .background(preset?.color ?: c.neutralSoft).testTag("settings-account-avatar"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        preset?.glyph ?: "M",
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        color = if (preset != null) Color.White else c.ink2,
                    )
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        session?.nick?.takeIf { it.isNotBlank() } ?: session?.email.orEmpty(),
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = if (signedIn) c.ink else c.ink3,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (signedIn) stringResource(R.string.settings_account_signed_in) else stringResource(R.string.settings_account_not_logged_in),
                        Modifier.padding(top = 2.dp), style = MikaType.Caption,
                        color = if (signedIn) c.ok else c.ink3,
                    )
                }
                if (!signedIn) {
                    Text(
                        stringResource(R.string.settings_account_sign_in),
                        Modifier.padding(start = 8.dp, bottom = 2.dp).clickable { onSignIn() },
                        color = c.accent, style = MikaType.Body,
                    )
                }
            }
            session?.email?.takeIf { it.isNotEmpty() }?.let { email ->
                ListRow(null, stringResource(R.string.mika_account_email), first = true) {
                    RowValue(email)
                }
            }
        }

        if (signedIn) {
            GroupLabel(stringResource(R.string.settings_account_profile))
            GroupCard {
                ListRow(MikaIcons.Edit, stringResource(R.string.mika_account_nick), first = true, onClick = { editingNick = true }) {
                    RowValue(session?.nick?.takeIf { it.isNotBlank() } ?: stringResource(R.string.settings_account_profile_none))
                    RowChevron()
                }
                ListRow(MikaIcons.Edit, stringResource(R.string.mika_account_avatar), onClick = { editingAvatar = true }) {
                    val shown = MIKA_AVATAR_PRESETS.firstOrNull {
                        it.id == session?.avatarPreset || (it.id == session?.avatar && session?.avatarPreset.isNullOrEmpty())
                    }
                    RowValue(shown?.let { stringResource(R.string.settings_account_avatar_selected, it.glyph) } ?: stringResource(R.string.settings_account_profile_none))
                    RowChevron()
                }
            }

            GroupLabel(stringResource(R.string.mika_sensenova_key))
            GroupCard {
                if (keyFilled) {
                    ListRow(null, stringResource(R.string.mika_sensenova_key), first = true, onClick = onEditSensenovaKey) {
                        RowValue(stringResource(R.string.models_key_saved))
                        RowChevron()
                    }
                } else {
                    ListRow(null, stringResource(R.string.mika_sensenova_key), stringResource(R.string.mika_sensenova_key_fill_hint), first = true, onClick = onEditSensenovaKey) {
                        RowValue(stringResource(R.string.mika_sensenova_key_open), ValueTone.Link)
                        RowChevron()
                    }
                }
            }

            GroupCard(Modifier.padding(top = 8.dp)) {
                ListRow(
                    null, stringResource(R.string.chatgpt_sign_out), stringResource(R.string.settings_account_sign_out_note),
                    titleColor = c.bad, first = true, onClick = { confirmingSignOut = true },
                ) { RowChevron() }
            }
        }
    }

    // 改昵称
    if (editingNick) {
        var nick by remember { mutableStateOf(session?.nick.orEmpty()) }
        KeyEditDialog(
            title = stringResource(R.string.mika_account_nick),
            hint = stringResource(R.string.mika_account_nick_ph),
            value = nick, onChange = { nick = it },
            allowClear = true,
            confirmText = stringResource(R.string.save),
            onConfirm = {
                editingNick = false
                onSaveProfile(nick.take(32), session?.avatar ?: "", session?.avatarPreset ?: "")
            },
            onDismiss = { editingNick = false },
        )
    }

    // 改头像
    var pickedAvatar by remember { mutableStateOf(
        session?.avatarPreset?.takeIf { it.isNotBlank() } ?: session?.avatar?.takeIf { it.matches(Regex("a[1-8]")) } ?: ""
    ) }
    if (editingAvatar) {
        KeyEditDialog(
            title = stringResource(R.string.mika_account_avatar),
            hint = stringResource(R.string.mika_account_avatar_hint),
            value = "", onChange = {},
            body = {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MIKA_AVATAR_PRESETS.forEach { p ->
                        val on = pickedAvatar == p.id
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (on) p.color else c.fill)
                                .clickable(onClick = { pickedAvatar = if (on) "" else p.id })
                                .testTag("settings-account-avatar-${p.id}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                p.glyph,
                                fontSize = if (p.glyph.length > 1) 11.sp else 16.sp,
                                color = if (on) Color.White else c.ink2,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            },
            confirmText = stringResource(R.string.save),
            onConfirm = {
                editingAvatar = false
                onSaveProfile(
                    session?.nick.orEmpty(),
                    if (pickedAvatar.isEmpty()) "" else pickedAvatar,
                    if (pickedAvatar.isEmpty()) "" else pickedAvatar,
                )
            },
            onDismiss = { editingAvatar = false },
        )
    }

    // 填/清商汤 key
    if (editingKey) {
        var key by remember { mutableStateOf("") }
        KeyEditDialog(
            title = stringResource(R.string.mika_sensenova_key),
            hint = if (keyFilled) stringResource(R.string.models_new_key_hint) else stringResource(R.string.mika_sensenova_key_fill_hint),
            value = key, onChange = { key = it },
            secret = true,
            allowClear = true,
            clearText = stringResource(R.string.model_key_clear),
            confirmText = stringResource(R.string.save),
            enabled = key.isNotBlank() || (key.isEmpty() && keyFilled),
            onConfirm = {
                val saved = onSensenovaKey(key.trim())
                if (saved) editingKey = false
            },
            onDismiss = { editingKey = false },
        )
    }

    // 退出前确认
    if (confirmingSignOut) {
        AlertDialog(
            onDismissRequest = { confirmingSignOut = false },
            title = { Text(stringResource(R.string.settings_account_sign_out_title)) },
            text = { Text(stringResource(R.string.settings_account_sign_out_body)) },
            confirmButton = {
                TextButton({ confirmingSignOut = false; onSignOut() }) {
                    Text(stringResource(R.string.chatgpt_sign_out), color = c.bad)
                }
            },
            dismissButton = { TextButton({ confirmingSignOut = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (!signedIn) {
        GroupCard(Modifier.padding(top = 8.dp)) {
            ListRow(null, stringResource(R.string.mika_guide_button), stringResource(R.string.mika_guide_note), first = true, onClick = onOpenSensenova) {
                RowChevron()
            }
        }
    }
}
