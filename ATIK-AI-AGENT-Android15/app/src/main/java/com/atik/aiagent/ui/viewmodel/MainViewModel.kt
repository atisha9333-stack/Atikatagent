package com.atik.aiagent.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.atik.aiagent.AtikAiAgentApp
import com.atik.aiagent.data.local.ActivityLogEntity
import com.atik.aiagent.data.model.AgentSettings
import com.atik.aiagent.data.model.LogOutcome
import com.atik.aiagent.data.model.OperationMode
import com.atik.aiagent.data.model.PendingDraft
import com.atik.aiagent.data.model.ReplyLanguage
import com.atik.aiagent.data.model.ReplyTone
import com.atik.aiagent.security.SensitiveContentFilter
import com.atik.aiagent.service.AtikNotificationListenerService
import com.atik.aiagent.service.RemoteInputDispatcher
import com.atik.aiagent.util.TestNotificationHelper
import com.atik.aiagent.util.XiaomiDeviceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AtikAiAgentApp).container

    val settings: StateFlow<AgentSettings> = container.preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AgentSettings())

    val activityLogs: StateFlow<List<ActivityLogEntity>> = container.activityLogDao.observeRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingDrafts: StateFlow<List<PendingDraft>> = AtikNotificationListenerService.pendingDrafts
    val isListenerConnected: StateFlow<Boolean> = AtikNotificationListenerService.isConnected
    val lastTestObservation = AtikNotificationListenerService.lastTestObservation
    val lastEchoReceived = TestNotificationHelper.lastEchoReceived

    private val _hasNotificationAccess = MutableStateFlow(false)
    val hasNotificationAccess: StateFlow<Boolean> = _hasNotificationAccess.asStateFlow()

    private val _isBatteryUnrestricted = MutableStateFlow(false)
    val isBatteryUnrestricted: StateFlow<Boolean> = _isBatteryUnrestricted.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _filterTestResult = MutableStateFlow<String?>(null)
    val filterTestResult: StateFlow<String?> = _filterTestResult.asStateFlow()

    init {
        TestNotificationHelper.registerReceiverIfNeeded(application)
        refreshDevicePermissions()
    }

    fun refreshDevicePermissions() {
        val ctx = getApplication<Application>()
        _hasNotificationAccess.value = AtikNotificationListenerService.hasNotificationAccess(ctx)
        _isBatteryUnrestricted.value = XiaomiDeviceHelper.isIgnoringBatteryOptimizations(ctx)
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun updateDraftReply(draftId: String, newText: String) {
        AtikNotificationListenerService.updateDraftText(draftId, newText)
    }

    fun approveAndSendDraft(draft: PendingDraft) {
        viewModelScope.launch {
            val cached = container.remoteInputDispatcher.getCached(draft.id)
            if (cached == null) {
                AtikNotificationListenerService.removePendingDraft(draft.id)
                _statusBanner.value = "Notification expired on ${draft.app.displayName}; cannot send inline reply."
                return@launch
            }

            when (val result = container.remoteInputDispatcher.sendReply(cached, draft.draftedReply)) {
                is RemoteInputDispatcher.DispatchResult.Sent -> {
                    container.deduplicationGuard.recordReplySent(
                        packageName = cached.packageName,
                        senderName = cached.senderName,
                        replyText = draft.draftedReply
                    )
                    container.remoteInputDispatcher.removeCached(draft.id)
                    AtikNotificationListenerService.removePendingDraft(draft.id)
                    container.activityLogDao.insertLog(
                        ActivityLogEntity(
                            timestampMillis = System.currentTimeMillis(),
                            appName = draft.app.displayName,
                            senderDisplay = draft.senderName,
                            outcome = LogOutcome.SENT_MANUAL.label,
                            detailNote = "Approved & sent (${draft.detectedLanguage})",
                            previewSnippet = if (settings.value.storeMessagePreviewInLogs) draft.draftedReply.take(80) else null
                        )
                    )
                    _statusBanner.value = "Reply sent to ${draft.senderName} via ${draft.app.displayName}."
                }

                is RemoteInputDispatcher.DispatchResult.Unavailable -> {
                    container.remoteInputDispatcher.removeCached(draft.id)
                    AtikNotificationListenerService.removePendingDraft(draft.id)
                    container.activityLogDao.insertLog(
                        ActivityLogEntity(
                            timestampMillis = System.currentTimeMillis(),
                            appName = draft.app.displayName,
                            senderDisplay = draft.senderName,
                            outcome = LogOutcome.UNAVAILABLE_ACTION.label,
                            detailNote = result.reason,
                            previewSnippet = null
                        )
                    )
                    _statusBanner.value = result.reason
                }

                is RemoteInputDispatcher.DispatchResult.Failed -> {
                    _statusBanner.value = result.reason
                }
            }
        }
    }

    fun dismissDraft(draft: PendingDraft) {
        viewModelScope.launch {
            container.remoteInputDispatcher.removeCached(draft.id)
            AtikNotificationListenerService.removePendingDraft(draft.id)
            container.activityLogDao.insertLog(
                ActivityLogEntity(
                    timestampMillis = System.currentTimeMillis(),
                    appName = draft.app.displayName,
                    senderDisplay = draft.senderName,
                    outcome = LogOutcome.DISMISSED.label,
                    detailNote = "Draft dismissed by ATIK",
                    previewSnippet = null
                )
            )
        }
    }

    fun setAgentEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.preferencesRepository.updateSettings { it.copy(agentEnabled = enabled) }
        }
    }

    fun setOperationMode(mode: OperationMode) {
        viewModelScope.launch {
            if (mode == OperationMode.AUTO_REPLY && !settings.value.testBenchVerified) {
                _statusBanner.value = "Run the RemoteInput Test Bench verification first before enabling Auto-Reply."
                return@launch
            }
            container.preferencesRepository.updateSettings { it.copy(operationMode = mode) }
        }
    }

    fun setMessengerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.preferencesRepository.updateSettings { it.copy(messengerEnabled = enabled) }
        }
    }

    fun setImoEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.preferencesRepository.updateSettings { it.copy(imoEnabled = enabled) }
        }
    }

    fun setLanguage(language: ReplyLanguage) {
        viewModelScope.launch {
            container.preferencesRepository.updateSettings { it.copy(preferredLanguage = language) }
        }
    }

    fun setTone(tone: ReplyTone) {
        viewModelScope.launch {
            container.preferencesRepository.updateSettings { it.copy(replyTone = tone) }
        }
    }

    fun setIgnoreGroupChats(ignore: Boolean) {
        viewModelScope.launch {
            container.preferencesRepository.updateSettings { it.copy(ignoreGroupChats = ignore) }
        }
    }

    fun setCooldownSeconds(seconds: Int) {
        viewModelScope.launch {
            container.preferencesRepository.updateSettings { it.copy(cooldownSeconds = seconds) }
        }
    }

    fun setStoreMessagePreviewInLogs(store: Boolean) {
        viewModelScope.launch {
            container.preferencesRepository.updateSettings { it.copy(storeMessagePreviewInLogs = store) }
        }
    }

    fun setAllowlistOnly(allowlistOnly: Boolean) {
        viewModelScope.launch {
            container.preferencesRepository.updateSettings { it.copy(allowlistOnly = allowlistOnly) }
        }
    }

    fun setAllowedSendersCsv(csv: String) {
        viewModelScope.launch {
            container.preferencesRepository.updateSettings { it.copy(allowedSendersCsv = csv) }
        }
    }

    fun setBlockedSendersCsv(csv: String) {
        viewModelScope.launch {
            container.preferencesRepository.updateSettings { it.copy(blockedSendersCsv = csv) }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            container.activityLogDao.clearAllLogs()
            container.deduplicationGuard.clearHistory()
            _statusBanner.value = "Local activity logs and deduplication cache cleared."
        }
    }

    fun triggerRemoteInputTest() {
        val ctx = getApplication<Application>()
        if (!AtikNotificationListenerService.hasNotificationAccess(ctx)) {
            _statusBanner.value = "Grant Notification Access first to run the RemoteInput test."
            return
        }
        TestNotificationHelper.postRemoteInputTestNotification(ctx)
        _statusBanner.value = "Dispatched test notification with RemoteInput. Observing round-trip..."
    }

    fun triggerUnavailableActionTest() {
        val ctx = getApplication<Application>()
        if (!AtikNotificationListenerService.hasNotificationAccess(ctx)) {
            _statusBanner.value = "Grant Notification Access first."
            return
        }
        TestNotificationHelper.postUnavailableActionTestNotification(ctx)
        _statusBanner.value = "Dispatched non-replyable notification to verify graceful handling."
    }

    fun runSensitiveFilterCheck(sampleText: String) {
        val verdict = SensitiveContentFilter.evaluate("Test Sender", sampleText)
        _filterTestResult.value = when (verdict) {
            is SensitiveContentFilter.FilterVerdict.Safe ->
                "SAFE: Eligible for Gemini Bangla/English draft generation."
            is SensitiveContentFilter.FilterVerdict.Blocked ->
                "BLOCKED (${verdict.category.label}): ${verdict.reason}"
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(app) as T
        }
    }
}
