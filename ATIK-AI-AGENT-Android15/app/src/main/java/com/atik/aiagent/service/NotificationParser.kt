package com.atik.aiagent.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.os.Build
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.atik.aiagent.data.model.ParsedNotification
import com.atik.aiagent.data.model.ReplyCapability
import com.atik.aiagent.data.model.SupportedApp

object NotificationParser {

    fun parse(sbn: StatusBarNotification): ParsedNotification? {
        val supportedApp = SupportedApp.fromPackage(sbn.packageName) ?: return null
        val notification = sbn.notification ?: return null

        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return null
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return null
        if (notification.category == Notification.CATEGORY_CALL) return null

        val extras = notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) ||
            (!conversationTitle.isNullOrBlank() && conversationTitle != title)

        val messageText = extractBestMessageText(extras)
        if (title.isBlank() && messageText.isBlank()) return null
        if (title.equals("You", ignoreCase = true) || title == "আপনি") return null

        val replyAction = findReplyAction(notification)
        val capability = if (replyAction != null) ReplyCapability.SUPPORTED else ReplyCapability.UNAVAILABLE
        val unavailableReason = if (replyAction == null) {
            "App notification does not expose an inline RemoteInput reply action."
        } else null

        return ParsedNotification(
            notificationKey = sbn.key,
            app = supportedApp,
            packageName = sbn.packageName,
            senderName = title.ifBlank { conversationTitle ?: supportedApp.displayName },
            conversationTitle = conversationTitle,
            isGroupConversation = isGroup,
            messageText = messageText,
            postTimeMillis = sbn.postTime,
            replyCapability = capability,
            unavailableReason = unavailableReason,
            replyActionIntent = replyAction?.actionIntent,
            remoteInput = replyAction?.remoteInputs?.firstOrNull(),
            allRemoteInputs = replyAction?.remoteInputs ?: emptyArray()
        )
    }

    private fun extractBestMessageText(extras: android.os.Bundle): String {
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

        val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelableArray(Notification.EXTRA_MESSAGES, Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        }

        val latestStyleText = messages
            ?.mapNotNull { item ->
                val bundle = item as? android.os.Bundle
                bundle?.getCharSequence("text")?.toString()?.trim()
            }
            ?.lastOrNull { it.isNotBlank() }

        return when {
            !latestStyleText.isNullOrBlank() -> latestStyleText
            !bigText.isNullOrBlank() -> bigText
            !text.isNullOrBlank() -> text
            else -> ""
        }
    }

    private data class ResolvedReplyAction(
        val actionIntent: PendingIntent,
        val remoteInputs: Array<RemoteInput>
    )

    private fun findReplyAction(notification: Notification): ResolvedReplyAction? {
        val actions = notification.actions ?: return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            for (action in actions) {
                val remoteInputs = action.remoteInputs
                if (action.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY &&
                    action.actionIntent != null &&
                    !remoteInputs.isNullOrEmpty()
                ) {
                    return ResolvedReplyAction(action.actionIntent, remoteInputs)
                }
            }
        }

        for (action in actions) {
            val remoteInputs = action.remoteInputs
            if (action.actionIntent != null && !remoteInputs.isNullOrEmpty()) {
                if (remoteInputs.any { it.allowFreeFormInput }) {
                    return ResolvedReplyAction(action.actionIntent, remoteInputs)
                }
            }
        }

        return null
    }
}
