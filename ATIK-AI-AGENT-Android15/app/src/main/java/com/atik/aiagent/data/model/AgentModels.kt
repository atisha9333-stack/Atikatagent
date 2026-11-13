package com.atik.aiagent.data.model

import android.app.PendingIntent
import android.app.RemoteInput

enum class SupportedApp(
    val displayName: String,
    val packageNames: Set<String>
) {
    MESSENGER(
        displayName = "Messenger",
        packageNames = setOf("com.facebook.orca", "com.facebook.mlite")
    ),
    IMO(
        displayName = "imo",
        packageNames = setOf(
            "com.imo.android.imoim",
            "com.imo.android.imoimhd",
            "com.imo.android.imoimbeta"
        )
    ),
    TEST_BENCH(
        displayName = "Test Bench",
        packageNames = setOf("com.atik.aiagent", "com.atik.aiagent.debug")
    );

    companion object {
        fun fromPackage(packageName: String): SupportedApp? {
            return entries.firstOrNull { packageName in it.packageNames }
        }
    }
}

enum class OperationMode {
    MANUAL_APPROVAL, // Default safe mode: generates draft, waits for ATIK's tap
    AUTO_REPLY       // Requires test verification + explicit user toggle
}

enum class ReplyLanguage(val label: String, val instruction: String) {
    AUTO_DETECT(
        label = "Auto (Bangla / English)",
        instruction = "Reply in the same language as the sender (natural Bangladeshi Bangla script if Bangla, natural Banglish if written in Roman Bengali, or clear polite English if English)."
    ),
    BANGLA(
        label = "Bangla (বাংলা)",
        instruction = "Reply in natural, warm, conversational Bangladeshi Bangla (বাংলা লিপি)."
    ),
    ENGLISH(
        label = "English",
        instruction = "Reply in concise, natural, polite English."
    ),
    BANGLISH(
        label = "Natural Banglish",
        instruction = "Reply in natural everyday Romanized Bengali (Banglish) commonly used in Bangladesh."
    )
}

enum class ReplyTone(val label: String) {
    POLITE_NATURAL("Polite & Natural"),
    CONCISE_BUSY("Concise / Busy"),
    FRIENDLY_WARM("Friendly & Casual")
}

enum class ReplyCapability {
    SUPPORTED,
    UNAVAILABLE
}

enum class LogOutcome(val label: String) {
    DRAFT_PENDING("Pending Approval"),
    SENT_MANUAL("Approved & Sent"),
    SENT_AUTO("Auto-Replied"),
    BLOCKED_SENSITIVE("Blocked (Sensitive)"),
    BLOCKED_DUPLICATE("Skipped (Duplicate/Loop)"),
    UNAVAILABLE_ACTION("No RemoteInput Action"),
    DISMISSED("Dismissed"),
    TEST_PASSED("Test Bench Passed"),
    FAILED("Send Failed")
}

data class ParsedNotification(
    val notificationKey: String,
    val app: SupportedApp,
    val packageName: String,
    val senderName: String,
    val conversationTitle: String?,
    val isGroupConversation: Boolean,
    val messageText: String,
    val postTimeMillis: Long,
    val replyCapability: ReplyCapability,
    val unavailableReason: String? = null,
    val replyActionIntent: PendingIntent? = null,
    val remoteInput: RemoteInput? = null,
    val allRemoteInputs: Array<RemoteInput> = emptyArray()
)

data class PendingDraft(
    val id: String,
    val notificationKey: String,
    val app: SupportedApp,
    val senderName: String,
    val incomingPreview: String,
    val draftedReply: String,
    val detectedLanguage: String,
    val createdAtMillis: Long,
    val replyCapability: ReplyCapability
)

data class AgentSettings(
    val agentEnabled: Boolean = true,
    val operationMode: OperationMode = OperationMode.MANUAL_APPROVAL,
    val messengerEnabled: Boolean = true,
    val imoEnabled: Boolean = true,
    val preferredLanguage: ReplyLanguage = ReplyLanguage.AUTO_DETECT,
    val replyTone: ReplyTone = ReplyTone.POLITE_NATURAL,
    val ignoreGroupChats: Boolean = true,
    val cooldownSeconds: Int = 90,
    val storeMessagePreviewInLogs: Boolean = false, // Privacy-first default: OFF
    val strictSensitiveShield: Boolean = true,
    val allowlistOnly: Boolean = false,
    val allowedSendersCsv: String = "",
    val blockedSendersCsv: String = "",
    val customSignature: String = "",
    val testBenchVerified: Boolean = false,
    val lastTestTimestampMillis: Long = 0L
)
