package com.atik.aiagent.security

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Prevents duplicate replies (when Android/HyperOS re-posts or updates a notification)
 * and prevents infinite reply loops (self-echo or rapid back-and-forth bursts).
 */
class DeduplicationGuard {

    sealed class GuardResult {
        data object Allowed : GuardResult()
        data class Rejected(val reason: String) : GuardResult()
    }

    private data class ConversationState(
        val lastIncomingHash: String,
        val lastIncomingTimestamp: Long,
        val lastRepliedAtMillis: Long,
        val lastReplyHash: String?,
        val repliesInCurrentHour: Int,
        val hourWindowStartMillis: Long
    )

    private val conversationMap = ConcurrentHashMap<String, ConversationState>()
    private val recentNotificationKeys = ConcurrentHashMap<String, Long>()

    companion object {
        private const val MAX_REPLIES_PER_HOUR_PER_CONVERSATION = 8
        private const val ECHO_GUARD_WINDOW_MS = 120_000L
        private const val KEY_DEDUP_WINDOW_MS = 10_000L
    }

    fun checkIncoming(
        packageName: String,
        notificationKey: String,
        senderName: String,
        messageText: String,
        postTimeMillis: Long,
        cooldownSeconds: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): GuardResult {
        val conversationId = normalizeConversationId(packageName, senderName)
        val messageHash = sha256("${senderName.trim().lowercase()}|${messageText.trim()}")
        val keySignature = "$notificationKey|$postTimeMillis|$messageHash"

        val previousKeySeen = recentNotificationKeys[keySignature]
        if (previousKeySeen != null && (nowMillis - previousKeySeen) < KEY_DEDUP_WINDOW_MS) {
            return GuardResult.Rejected("Duplicate notification update ignored")
        }

        val state = conversationMap[conversationId]
        if (state != null) {
            if (state.lastIncomingHash == messageHash && (nowMillis - state.lastIncomingTimestamp) < 300_000L) {
                return GuardResult.Rejected("Identical message already processed")
            }

            if (state.lastReplyHash != null &&
                state.lastReplyHash == sha256(messageText.trim()) &&
                (nowMillis - state.lastRepliedAtMillis) < ECHO_GUARD_WINDOW_MS
            ) {
                return GuardResult.Rejected("Self-echo detected; loop prevented")
            }

            val elapsedSinceReply = nowMillis - state.lastRepliedAtMillis
            if (state.lastRepliedAtMillis > 0L && elapsedSinceReply < cooldownSeconds * 1000L) {
                val remaining = ((cooldownSeconds * 1000L - elapsedSinceReply) / 1000L).coerceAtLeast(1L)
                return GuardResult.Rejected("Conversation cooldown active (${remaining}s left)")
            }

            val withinSameHour = (nowMillis - state.hourWindowStartMillis) < 3_600_000L
            if (withinSameHour && state.repliesInCurrentHour >= MAX_REPLIES_PER_HOUR_PER_CONVERSATION) {
                return GuardResult.Rejected("Hourly reply cap reached for safety")
            }
        }

        recentNotificationKeys[keySignature] = nowMillis
        pruneOldKeys(nowMillis)
        return GuardResult.Allowed
    }

    fun recordIncomingSeen(
        packageName: String,
        senderName: String,
        messageText: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val conversationId = normalizeConversationId(packageName, senderName)
        val messageHash = sha256("${senderName.trim().lowercase()}|${messageText.trim()}")
        conversationMap.compute(conversationId) { _, existing ->
            existing?.copy(
                lastIncomingHash = messageHash,
                lastIncomingTimestamp = nowMillis
            ) ?: ConversationState(
                lastIncomingHash = messageHash,
                lastIncomingTimestamp = nowMillis,
                lastRepliedAtMillis = 0L,
                lastReplyHash = null,
                repliesInCurrentHour = 0,
                hourWindowStartMillis = nowMillis
            )
        }
    }

    fun recordReplySent(
        packageName: String,
        senderName: String,
        replyText: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val conversationId = normalizeConversationId(packageName, senderName)
        val replyHash = sha256(replyText.trim())
        conversationMap.compute(conversationId) { _, existing ->
            if (existing == null) {
                ConversationState(
                    lastIncomingHash = "",
                    lastIncomingTimestamp = nowMillis,
                    lastRepliedAtMillis = nowMillis,
                    lastReplyHash = replyHash,
                    repliesInCurrentHour = 1,
                    hourWindowStartMillis = nowMillis
                )
            } else {
                val sameHour = (nowMillis - existing.hourWindowStartMillis) < 3_600_000L
                existing.copy(
                    lastRepliedAtMillis = nowMillis,
                    lastReplyHash = replyHash,
                    repliesInCurrentHour = if (sameHour) existing.repliesInCurrentHour + 1 else 1,
                    hourWindowStartMillis = if (sameHour) existing.hourWindowStartMillis else nowMillis
                )
            }
        }
    }

    fun clearHistory() {
        conversationMap.clear()
        recentNotificationKeys.clear()
    }

    private fun normalizeConversationId(packageName: String, senderName: String): String {
        return "${packageName.lowercase()}::${senderName.trim().lowercase()}"
    }

    private fun pruneOldKeys(nowMillis: Long) {
        if (recentNotificationKeys.size > 200) {
            recentNotificationKeys.entries.removeIf { (_, seenAt) -> nowMillis - seenAt > 300_000L }
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
