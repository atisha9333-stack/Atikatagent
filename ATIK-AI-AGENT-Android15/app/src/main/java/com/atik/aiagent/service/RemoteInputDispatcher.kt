package com.atik.aiagent.service

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.atik.aiagent.data.model.ParsedNotification
import com.atik.aiagent.data.model.ReplyCapability
import java.util.concurrent.ConcurrentHashMap

/**
 * Safely dispatches replies via official Android RemoteInput + PendingIntent APIs.
 * Stores active ParsedNotification references in memory only (never serialized)
 * so manual approval can trigger the exact PendingIntent when ATIK approves a draft.
 */
class RemoteInputDispatcher(private val context: Context) {

    sealed class DispatchResult {
        data object Sent : DispatchResult()
        data class Unavailable(val reason: String) : DispatchResult()
        data class Failed(val reason: String) : DispatchResult()
    }

    private val activeCache = ConcurrentHashMap<String, ParsedNotification>()

    fun cacheActiveNotification(id: String, parsed: ParsedNotification) {
        activeCache[id] = parsed
        if (activeCache.size > 50) {
            val oldest = activeCache.entries.minByOrNull { it.value.postTimeMillis }?.key
            if (oldest != null) activeCache.remove(oldest)
        }
    }

    fun getCached(id: String): ParsedNotification? = activeCache[id]

    fun removeCached(id: String) {
        activeCache.remove(id)
    }

    fun sendReply(parsed: ParsedNotification, replyText: String): DispatchResult {
        if (parsed.replyCapability != ReplyCapability.SUPPORTED ||
            parsed.replyActionIntent == null ||
            parsed.remoteInput == null
        ) {
            return DispatchResult.Unavailable(
                parsed.unavailableReason ?: "RemoteInput reply action is unavailable for this notification."
            )
        }

        val cleanText = replyText.trim()
        if (cleanText.isEmpty()) {
            return DispatchResult.Failed("Reply text cannot be empty.")
        }

        return try {
            val fillInIntent = Intent()
            val resultsBundle = Bundle()
            for (input in parsed.allRemoteInputs) {
                resultsBundle.putCharSequence(input.resultKey, cleanText)
            }
            RemoteInput.addResultsToIntent(parsed.allRemoteInputs, fillInIntent, resultsBundle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                RemoteInput.setResultsSource(fillInIntent, RemoteInput.SOURCE_FREE_FORM_INPUT)
            }
            parsed.replyActionIntent.send(context, 0, fillInIntent)
            DispatchResult.Sent
        } catch (e: PendingIntent.CanceledException) {
            DispatchResult.Unavailable("Reply action expired or was dismissed by ${parsed.app.displayName}.")
        } catch (e: SecurityException) {
            DispatchResult.Failed("Security exception dispatching RemoteInput: ${e.message}")
        } catch (e: Exception) {
            DispatchResult.Failed("Failed to send reply: ${e.message ?: "Unknown error"}")
        }
    }
}
