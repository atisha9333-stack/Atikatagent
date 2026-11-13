package com.atik.aiagent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atik.aiagent.ui.screens.ActivityLogScreen
import com.atik.aiagent.ui.screens.DashboardScreen
import com.atik.aiagent.ui.screens.RulesPrivacyScreen
import com.atik.aiagent.ui.screens.TestBenchScreen
import com.atik.aiagent.ui.screens.XiaomiGuideScreen
import com.atik.aiagent.ui.theme.AtikAiAgentTheme
import com.atik.aiagent.ui.theme.AtikColors
import com.atik.aiagent.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AtikAiAgentTheme {
                val vm: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(application)
                )
                AtikAgentRootScaffold(vm)
            }
        }
    }
}

private enum class AgentTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Rounded.Dashboard),
    TEST_BENCH("Test", Icons.Rounded.Science),
    RULES("Rules", Icons.Rounded.Tune),
    LOGS("Logs", Icons.Rounded.History),
    XIAOMI("Xiaomi", Icons.Rounded.BatteryChargingFull)
}

@Composable
private fun AtikAgentRootScaffold(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableStateOf(AgentTab.DASHBOARD) }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val logs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val pendingDrafts by viewModel.pendingDrafts.collectAsStateWithLifecycle()
    val hasNotificationAccess by viewModel.hasNotificationAccess.collectAsStateWithLifecycle()
    val isListenerConnected by viewModel.isListenerConnected.collectAsStateWithLifecycle()
    val isBatteryUnrestricted by viewModel.isBatteryUnrestricted.collectAsStateWithLifecycle()
    val lastTestObservation by viewModel.lastTestObservation.collectAsStateWithLifecycle()
    val lastEchoReceived by viewModel.lastEchoReceived.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val filterTestResult by viewModel.filterTestResult.collectAsStateWithLifecycle()

    val postNotifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled gracefully */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                postNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDevicePermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    Scaffold(
        containerColor = AtikColors.ObsidianBg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = AtikColors.SurfaceDark) {
                AgentTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = AtikColors.AccentCyan,
                            indicatorColor = AtikColors.AccentPurple,
                            unselectedIconColor = AtikColors.TextSecondary,
                            unselectedTextColor = AtikColors.TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                AgentTab.DASHBOARD -> DashboardScreen(
                    settings = settings,
                    hasNotificationAccess = hasNotificationAccess,
                    isListenerConnected = isListenerConnected,
                    isBatteryUnrestricted = isBatteryUnrestricted,
                    pendingDrafts = pendingDrafts,
                    onToggleAgent = viewModel::setAgentEnabled,
                    onUpdateDraftText = viewModel::updateDraftReply,
                    onApproveDraft = viewModel::approveAndSendDraft,
                    onDismissDraft = viewModel::dismissDraft,
                    onNavigateToTestBench = { currentTab = AgentTab.TEST_BENCH },
                    onNavigateToXiaomiGuide = { currentTab = AgentTab.XIAOMI }
                )

                AgentTab.TEST_BENCH -> TestBenchScreen(
                    hasNotificationAccess = hasNotificationAccess,
                    isListenerConnected = isListenerConnected,
                    testBenchVerified = settings.testBenchVerified,
                    lastObservation = lastTestObservation,
                    lastEchoReceived = lastEchoReceived,
                    filterTestResult = filterTestResult,
                    onRunRemoteInputTest = viewModel::triggerRemoteInputTest,
                    onRunUnavailableTest = viewModel::triggerUnavailableActionTest,
                    onRunSensitiveFilterCheck = viewModel::runSensitiveFilterCheck
                )

                AgentTab.RULES -> RulesPrivacyScreen(
                    settings = settings,
                    onSetOperationMode = viewModel::setOperationMode,
                    onSetMessenger = viewModel::setMessengerEnabled,
                    onSetImo = viewModel::setImoEnabled,
                    onSetLanguage = viewModel::setLanguage,
                    onSetTone = viewModel::setTone,
                    onSetIgnoreGroups = viewModel::setIgnoreGroupChats,
                    onSetCooldownSeconds = viewModel::setCooldownSeconds,
                    onSetStorePreview = viewModel::setStoreMessagePreviewInLogs,
                    onSetAllowlistOnly = viewModel::setAllowlistOnly,
                    onSetAllowedCsv = viewModel::setAllowedSendersCsv,
                    onSetBlockedCsv = viewModel::setBlockedSendersCsv,
                    onClearLogs = viewModel::clearAllLogs
                )

                AgentTab.LOGS -> ActivityLogScreen(logs = logs)

                AgentTab.XIAOMI -> XiaomiGuideScreen(
                    hasNotificationAccess = hasNotificationAccess,
                    isBatteryUnrestricted = isBatteryUnrestricted
                )
            }
        }
    }
}
