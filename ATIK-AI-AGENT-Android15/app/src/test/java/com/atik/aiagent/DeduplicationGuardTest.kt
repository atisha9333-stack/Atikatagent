package com.atik.aiagent

import com.atik.aiagent.security.DeduplicationGuard
import org.junit.Assert.assertTrue
import org.junit.Test

class DeduplicationGuardTest {

    @Test
    fun rejectsDuplicateNotificationRepost() {
        val guard = DeduplicationGuard()
        val now = 1_700_000_000_000L

        val first = guard.checkIncoming(
            packageName = "com.facebook.orca",
            notificationKey = "0|com.facebook.orca|1|tag|100",
            senderName = "Rafiq",
            messageText = "Kemon achen?",
            postTimeMillis = now,
            cooldownSeconds = 60,
            nowMillis = now
        )
        assertTrue(first is DeduplicationGuard.GuardResult.Allowed)

        val duplicate = guard.checkIncoming(
            packageName = "com.facebook.orca",
            notificationKey = "0|com.facebook.orca|1|tag|100",
            senderName = "Rafiq",
            messageText = "Kemon achen?",
            postTimeMillis = now,
            cooldownSeconds = 60,
            nowMillis = now + 2_000L
        )
        assertTrue(duplicate is DeduplicationGuard.GuardResult.Rejected)
    }

    @Test
    fun enforcesCooldownAndSelfEchoLoopPrevention() {
        val guard = DeduplicationGuard()
        val now = 1_700_000_000_000L

        guard.recordReplySent(
            packageName = "com.imo.android.imoim",
            senderName = "Karim",
            replyText = "Alhamdulillah valo achi, ektu pore call dicchi.",
            nowMillis = now
        )

        val echo = guard.checkIncoming(
            packageName = "com.imo.android.imoim",
            notificationKey = "0|com.imo.android.imoim|2|tag|100",
            senderName = "Karim",
            messageText = "Alhamdulillah valo achi, ektu pore call dicchi.",
            postTimeMillis = now + 5_000L,
            cooldownSeconds = 90,
            nowMillis = now + 5_000L
        )
        assertTrue(echo is DeduplicationGuard.GuardResult.Rejected)

        val afterCooldown = guard.checkIncoming(
            packageName = "com.imo.android.imoim",
            notificationKey = "0|com.imo.android.imoim|3|tag|100",
            senderName = "Karim",
            messageText = "Accha thik ache bhai",
            postTimeMillis = now + 95_000L,
            cooldownSeconds = 90,
            nowMillis = now + 95_000L
        )
        assertTrue(afterCooldown is DeduplicationGuard.GuardResult.Allowed)
    }
}
