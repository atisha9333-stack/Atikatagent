package com.atik.aiagent.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.atik.aiagent.AtikAiAgentApp
import com.atik.aiagent.MainActivity
import com.atik.aiagent.data.local.ActivityLogEntity
import com.atik.aiagent.data.model.AgentSettings
import com.atik.aiagent.data.model.LogOutcome
import com.atik.aiagent.data.model.OperationMode
import com.atik.aiagent.data.model.PendingDraft
import com.atik.aiagent.data.model.ReplyCapability
import com.atik.aiagent.data.model.SupportedApp
import com.atik.aiagent.security.DeduplicationGuard
import com.atik.aiagent.security.SensitiveContentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Official Android NotificationListenerService for ATIK AI AGENT.
 * Handles Messenger, imo, and local Test Bench notifications with strict lifecycle safety,
 * RemoteInput verification, sensitive-content shielding, and Manual Approval default.
 */
class AtikNotificationListenerService : NotificationListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
    }

    override fun onListenerDisconnected() {
        _isConnected.value = false
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        _isConnected.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val parsed = NotificationParser.parse(sbn) ?: return

        val appContainer = (application as AtikAiAgentApp).container
        serviceScope.launch {
            val settings = appContainer.preferencesRepository.settingsFlow.first()

            // Stage 1: Separate Test Bench Inspection (works even before AI automation is enabled)
            if (parsed.app == SupportedApp.TEST_BENCH) {
                handleTestBenchNotification(parsed, appContainer)
                return@launch
            }

            if (!settings.agentEnabled) return@launch
            if (parsed.app == SupportedApp.MESSENGER && !settings.messengerEnabled) return@launch
            if (parsed.app == SupportedApp.IMO && !settings.imoEnabled) return@launch
            if (settings.ignoreGroupChats && parsed.isGroupConversation) return@launch

            if (!isSenderEligible(parsed.senderName, settings)) return@launch

            // Privacy Gate 1: Sensitive Content Shield (runs locally BEFORE anything else)
            val sensitivityVerdict = SensitiveContentFilter.evaluate(
                sender = parsed.senderName,
                message = parsed.messageText
            )
            if (sensitivityVerdict is SensitiveContentFilter.FilterVerdict.Blocked) {
                appContainer.activityLogDao.insertLog(
                    ActivityLogEntity(
                        timestampMillis = System.currentTimeMillis(),
                        appName = parsed.app.displayName,
                        senderDisplay = maskSender(parsed.senderName),
                        outcome = LogOutcome.BLOCKED_SENSITIVE.label,
                        detailNote = "${sensitivityVerdict.category.label}: ${sensitivityVerdict.reason}",
                        previewSnippet = null // Never store sensitive text
                    )
                )
                return@launch
            }

            // Safety Gate 2: Deduplication & Anti-Loop Guard
            val dedupResult = appContainer.deduplicationGuard.checkIncoming(
                packageName = parsed.packageName,
                notificationKey = parsed.notificationKey,
                senderName = parsed.senderName,
                messageText = parsed.messageText,
                postTimeMillis = parsed.postTimeMillis,
                cooldownSeconds = settings.cooldownSeconds
            )
            if (dedupResult is DeduplicationGuard.GuardResult.Rejected) {
                appContainer.activityLogDao.insertLog(
                    ActivityLogEntity(
                        timestampMillis = System.currentTimeMillis(),
                        appName = parsed.app.displayName,
                        senderDisplay = maskSender(parsed.senderName),
                        outcome = LogOutcome.BLOCKED_DUPLICATE.label,
                        detailNote = dedupResult.reason,
                        previewSnippet = null
                    )
                )
                return@launch
            }
            appContainer.deduplicationGuard.recordIncomingSeen(
                packageName = parsed.packageName,
                senderName = parsed.senderName,
                messageText = parsed.messageText
            )

            // Safety Gate 3: Handle unavailable RemoteInput gracefully
            if (parsed.replyCapability == ReplyCapability.UNAVAILABLE) {
                appContainer.activityLogDao.insertLog(
                    ActivityLogEntity(
                        timestampMillis = System.currentTimeMillis(),
                        appName = parsed.app.displayName,
                        senderDisplay = parsed.senderName,
                        outcome = LogOutcome.UNAVAILABLE_ACTION.label,
                        detailNote = parsed.unavailableReason ?: "No RemoteInput action exposed",
                        previewSnippet = if (settings.storeMessagePreviewInLogs) parsed.messageText.take(80) else null
                    )
                )
                return@launch
            }

            // Generate natural Bangla / English reply via Firebase AI Logic (Gemini + App Check)
            val draftResult = appContainer.geminiReplyGenerator.generateReply(
                app = parsed.app,
                senderName = parsed.senderName,
                incomingMessage = parsed.messageText,
                language = settings.preferredLanguage,
                tone = settings.replyTone,
                customSignature = settings.customSignature
            )

            draftResult.onSuccess { generated ->
                val draftId = UUID.randomUUID().toString()

                // Auto-reply ONLY if explicitly enabled AND Test Bench verified
                if (settings.operationMode == OperationMode.AUTO_REPLY && settings.testBenchVerified) {
                    when (val sent = appContainer.remoteInputDispatcher.sendReply(parsed, generated.replyText)) {
                        is RemoteInputDispatcher.DispatchResult.Sent -> {
                            appContainer.deduplicationGuard.recordReplySent(
                                packageName = parsed.packageName,
                                senderName = parsed.senderName,
                                replyText = generated.replyText
                            )
                            appContainer.activityLogDao.insertLog(
                                ActivityLogEntity(
                                    timestampMillis = System.currentTimeMillis(),
                                    appName = parsed.app.displayName,
                                    senderDisplay = parsed.senderName,
                                    outcome = LogOutcome.SENT_AUTO.label,
                                    detailNote = "Auto-replied (${generated.detectedLanguage})",
                                    previewSnippet = if (settings.storeMessagePreviewInLogs) generated.replyText.take(80) else null
                                )
                            )
                        }

                        is RemoteInputDispatcher.DispatchResult.Unavailable -> {
                            appContainer.activityLogDao.insertLog(
                                ActivityLogEntity(
                                    timestampMillis = System.currentTimeMillis(),
                                    appName = parsed.app.displayName,
                                    senderDisplay = parsed.senderName,
                                    outcome = LogOutcome.UNAVAILABLE_ACTION.label,
                                    detailNote = sent.reason,
                                    previewSnippet = null
                                )
                            )
                        }

                        is RemoteInputDispatcher.DispatchResult.Failed -> {
                            appContainer.activityLogDao.insertLog(
                                ActivityLogEntity(
                                    timestampMillis = System.currentTimeMillis(),
                                    appName = parsed.app.displayName,
                                    senderDisplay = parsed.senderName,
                                    outcome = LogOutcome.FAILED.label,
                                    detailNote = sent.reason,
                                    previewSnippet = null
                                )
                            )
                        }
                    }
                } else {
                    // Default Mode: Manual Approval Queue
                    appContainer.remoteInputDispatcher.cacheActiveNotification(draftId, parsed)
                    val pending = PendingDraft(
                        id = draftId,
                        notificationKey = parsed.notificationKey,
                        app = parsed.app,
                        senderName = parsed.senderName,
                        incomingPreview = parsed.messageText,
                        draftedReply = generated.replyText,
                        detectedLanguage = generated.detectedLanguage,
                        createdAtMillis = System.currentTimeMillis(),
                        replyCapability = parsed.replyCapability
                    )
                    _pendingDrafts.update { current -> (listOf(pending) + current).take(25) }
                    postApprovalAlertNotification(pending)

                    appContainer.activityLogDao.insertLog(
                        ActivityLogEntity(
                            timestampMillis = System.currentTimeMillis(),
                            appName = parsed.app.displayName,
                            senderDisplay = parsed.senderName,
                            outcome = LogOutcome.DRAFT_PENDING.label,
                            detailNote = "Draft ready (${generated.detectedLanguage}); awaiting manual approval",
                            previewSnippet = if (settings.storeMessagePreviewInLogs) parsed.messageText.take(80) else null
                        )
                    )
                }
            }.onFailure { error ->
                appContainer.activityLogDao.insertLog(
                    ActivityLogEntity(
                        timestampMillis = System.currentTimeMillis(),
                        appName = parsed.app.displayName,
                        senderDisplay = parsed.senderName,
                        outcome = LogOutcome.FAILED.label,
                        detailNote = "Gemini / App Check error: ${error.message ?: "Network unavailable"}",
                        previewSnippet = null
                    )
                )
            }
        }
    }

    private suspend fun handleTestBenchNotification(
        parsed: com.atik.aiagent.data.model.ParsedNotification,
        container: com.atik.aiagent.di.AppContainer
    ) {
        val statusMsg = if (parsed.replyCapability == ReplyCapability.SUPPORTED) {
            "Detected RemoteInput key='${parsed.remoteInput?.resultKey}' on test notification."
        } else {
            "Detected non-replyable notification gracefully (RemoteInput unavailable)."
        }

        _lastTestObservation.value = TestObservation(
            timestampMillis = System.currentTimeMillis(),
            sender = parsed.senderName,
            hasRemoteInput = parsed.replyCapability == ReplyCapability.SUPPORTED,
            remoteInputKey = parsed.remoteInput?.resultKey,
            detail = statusMsg
        )

        if (parsed.replyCapability == ReplyCapability.SUPPORTED &&
            parsed.messageText.contains("[AUTO_ECHO_TEST]")
        ) {
            val result = container.remoteInputDispatcher.sendReply(
                parsed,
                "ATIK Test Bench RemoteInput Round-Trip OK"
            )
            if (result is RemoteInputDispatcher.DispatchResult.Sent) {
                container.preferencesRepository.updateSettings {
                    it.copy(
                        testBenchVerified = true,
                        lastTestTimestampMillis = System.currentTimeMillis()
                    )
                }
                container.activityLogDao.insertLog(
                    ActivityLogEntity(
                        timestampMillis = System.currentTimeMillis(),
                        appName = SupportedApp.TEST_BENCH.displayName,
                        senderDisplay = "Self-Test",
                        outcome = LogOutcome.TEST_PASSED.label,
                        detailNote = "Notification detection + RemoteInput reply verified before AI automation",
                        previewSnippet = null
                    )
                )
            }
        }
    }

    private fun isSenderEligible(sender: String, settings: AgentSettings): Boolean {
        val normalized = sender.trim().lowercase()
        val blocked = settings.blockedSendersCsv.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        if (blocked.any { normalized.contains(it) }) return false

        if (settings.allowlistOnly) {
            val allowed = settings.allowedSendersCsv.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
            return allowed.any { normalized.contains(it) }
        }
        return true
    }

    private fun maskSender(name: String): String {
        val trimmed = name.trim()
        if (trimmed.length <= 2) return "**"
        return "${trimmed.take(2)}***"
    }

    private fun postApprovalAlertNotification(draft: PendingDraft) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "atik_approval_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pending Reply Approvals",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            draft.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("ATIK AI AGENT • ${draft.app.displayName}")
            .setContentText("Draft ready for ${draft.senderName}: ${draft.draftedReply}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(draft.id.hashCode(), notification) }
    }

    data class TestObservation(
        val timestampMillis: Long,
        val sender: String,
        val hasRemoteInput: Boolean,
        val remoteInputKey: String?,
        val detail: String
    )

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _pendingDrafts = MutableStateFlow<List<PendingDraft>>(emptyList())
        val pendingDrafts: StateFlow<List<PendingDraft>> = _pendingDrafts.asStateFlow()

        private val _lastTestObservation = MutableStateFlow<TestObservation?>(null)
        val lastTestObservation: StateFlow<TestObservation?> = _lastTestObservation.asStateFlow()

        fun removePendingDraft(id: String) {
            _pendingDrafts.update { list -> list.filterNot { it.id == id } }
        }

        fun updateDraftText(id: String, newReply: String) {
            _pendingDrafts.update { list ->
                list.map { if (it.id == id) it.copy(draftedReply = newReply) else it }
            }
        }

        fun hasNotificationAccess(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val component = ComponentName(context, AtikNotificationListenerService::class.java)
            return enabledListeners.contains(component.flattenToString()) ||
                enabledListeners.contains(component.flattenToShortString())
        }
    }
}
