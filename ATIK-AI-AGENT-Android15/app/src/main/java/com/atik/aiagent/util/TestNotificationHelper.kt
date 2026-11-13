package com.atik.aiagent.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stage 1 Verification Utility:
 * Fires real Android notifications (one WITH RemoteInput, one WITHOUT RemoteInput)
 * so ATIK can verify NotificationListenerService + RemoteInput round-trip independently
 * before enabling Gemini AI automation.
 */
object TestNotificationHelper {

    private const val CHANNEL_ID = "atik_test_bench_channel"
    private const val ACTION_TEST_REPLY = "com.atik.aiagent.ACTION_TEST_REMOTE_INPUT"
    private const val KEY_TEXT_REPLY = "key_atik_test_reply"
    private const val TEST_NOTIF_WITH_INPUT_ID = 9101
    private const val TEST_NOTIF_NO_INPUT_ID = 9102

    private val _lastEchoReceived = MutableStateFlow<String?>(null)
    val lastEchoReceived: StateFlow<String?> = _lastEchoReceived.asStateFlow()

    private var receiverRegistered = false

    private val replyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = RemoteInput.getResultsFromIntent(intent)
            val replyText = bundle?.getCharSequence(KEY_TEXT_REPLY)?.toString()
            if (!replyText.isNullOrBlank()) {
                _lastEchoReceived.value = replyText
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(TEST_NOTIF_WITH_INPUT_ID)
            }
        }
    }

    fun registerReceiverIfNeeded(context: Context) {
        if (receiverRegistered) return
        val filter = IntentFilter(ACTION_TEST_REPLY)
        ContextCompat.registerReceiver(
            context.applicationContext,
            replyReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
    }

    fun postRemoteInputTestNotification(context: Context) {
        registerReceiverIfNeeded(context)
        ensureChannel(context)

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Test Inline Reply")
            .build()

        val replyIntent = Intent(ACTION_TEST_REPLY).apply {
            setPackage(context.packageName)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_NOTIF_WITH_INPUT_ID,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Test Contact (RemoteInput)")
            .setContentText("[AUTO_ECHO_TEST] Assalamu Alaikum, free achen?")
            .addAction(replyAction)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(TEST_NOTIF_WITH_INPUT_ID, notification)
    }

    fun postUnavailableActionTestNotification(context: Context) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("imo Alert (No RemoteInput)")
            .setContentText("Missed voice call or photo attachment (reply action unavailable)")
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(TEST_NOTIF_NO_INPUT_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ATIK RemoteInput Test Bench",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }
    }
}
