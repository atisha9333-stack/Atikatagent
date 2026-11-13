package com.atik.aiagent.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.atik.aiagent.data.model.AgentSettings
import com.atik.aiagent.data.model.OperationMode
import com.atik.aiagent.data.model.ReplyLanguage
import com.atik.aiagent.data.model.ReplyTone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.agentDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "atik_ai_agent_prefs"
)

class AgentPreferencesRepository(private val context: Context) {

    private object Keys {
        val AGENT_ENABLED = booleanPreferencesKey("agent_enabled")
        val OPERATION_MODE = stringPreferencesKey("operation_mode")
        val MESSENGER_ENABLED = booleanPreferencesKey("messenger_enabled")
        val IMO_ENABLED = booleanPreferencesKey("imo_enabled")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val REPLY_TONE = stringPreferencesKey("reply_tone")
        val IGNORE_GROUP_CHATS = booleanPreferencesKey("ignore_group_chats")
        val COOLDOWN_SECONDS = intPreferencesKey("cooldown_seconds")
        val STORE_MESSAGE_PREVIEW = booleanPreferencesKey("store_message_preview")
        val STRICT_SENSITIVE_SHIELD = booleanPreferencesKey("strict_sensitive_shield")
        val ALLOWLIST_ONLY = booleanPreferencesKey("allowlist_only")
        val ALLOWED_SENDERS_CSV = stringPreferencesKey("allowed_senders_csv")
        val BLOCKED_SENDERS_CSV = stringPreferencesKey("blocked_senders_csv")
        val CUSTOM_SIGNATURE = stringPreferencesKey("custom_signature")
        val TEST_BENCH_VERIFIED = booleanPreferencesKey("test_bench_verified")
        val LAST_TEST_TIMESTAMP = longPreferencesKey("last_test_timestamp")
    }

    val settingsFlow: Flow<AgentSettings> = context.agentDataStore.data.map { prefs ->
        AgentSettings(
            agentEnabled = prefs[Keys.AGENT_ENABLED] ?: true,
            operationMode = prefs[Keys.OPERATION_MODE]
                ?.let { runCatching { OperationMode.valueOf(it) }.getOrNull() }
                ?: OperationMode.MANUAL_APPROVAL,
            messengerEnabled = prefs[Keys.MESSENGER_ENABLED] ?: true,
            imoEnabled = prefs[Keys.IMO_ENABLED] ?: true,
            preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE]
                ?.let { runCatching { ReplyLanguage.valueOf(it) }.getOrNull() }
                ?: ReplyLanguage.AUTO_DETECT,
            replyTone = prefs[Keys.REPLY_TONE]
                ?.let { runCatching { ReplyTone.valueOf(it) }.getOrNull() }
                ?: ReplyTone.POLITE_NATURAL,
            ignoreGroupChats = prefs[Keys.IGNORE_GROUP_CHATS] ?: true,
            cooldownSeconds = prefs[Keys.COOLDOWN_SECONDS] ?: 90,
            storeMessagePreviewInLogs = prefs[Keys.STORE_MESSAGE_PREVIEW] ?: false,
            strictSensitiveShield = prefs[Keys.STRICT_SENSITIVE_SHIELD] ?: true,
            allowlistOnly = prefs[Keys.ALLOWLIST_ONLY] ?: false,
            allowedSendersCsv = prefs[Keys.ALLOWED_SENDERS_CSV] ?: "",
            blockedSendersCsv = prefs[Keys.BLOCKED_SENDERS_CSV] ?: "",
            customSignature = prefs[Keys.CUSTOM_SIGNATURE] ?: "",
            testBenchVerified = prefs[Keys.TEST_BENCH_VERIFIED] ?: false,
            lastTestTimestampMillis = prefs[Keys.LAST_TEST_TIMESTAMP] ?: 0L
        )
    }

    suspend fun updateSettings(transform: (AgentSettings) -> AgentSettings) {
        context.agentDataStore.edit { prefs ->
            val current = AgentSettings(
                agentEnabled = prefs[Keys.AGENT_ENABLED] ?: true,
                operationMode = prefs[Keys.OPERATION_MODE]
                    ?.let { runCatching { OperationMode.valueOf(it) }.getOrNull() }
                    ?: OperationMode.MANUAL_APPROVAL,
                messengerEnabled = prefs[Keys.MESSENGER_ENABLED] ?: true,
                imoEnabled = prefs[Keys.IMO_ENABLED] ?: true,
                preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE]
                    ?.let { runCatching { ReplyLanguage.valueOf(it) }.getOrNull() }
                    ?: ReplyLanguage.AUTO_DETECT,
                replyTone = prefs[Keys.REPLY_TONE]
                    ?.let { runCatching { ReplyTone.valueOf(it) }.getOrNull() }
                    ?: ReplyTone.POLITE_NATURAL,
                ignoreGroupChats = prefs[Keys.IGNORE_GROUP_CHATS] ?: true,
                cooldownSeconds = prefs[Keys.COOLDOWN_SECONDS] ?: 90,
                storeMessagePreviewInLogs = prefs[Keys.STORE_MESSAGE_PREVIEW] ?: false,
                strictSensitiveShield = prefs[Keys.STRICT_SENSITIVE_SHIELD] ?: true,
                allowlistOnly = prefs[Keys.ALLOWLIST_ONLY] ?: false,
                allowedSendersCsv = prefs[Keys.ALLOWED_SENDERS_CSV] ?: "",
                blockedSendersCsv = prefs[Keys.BLOCKED_SENDERS_CSV] ?: "",
                customSignature = prefs[Keys.CUSTOM_SIGNATURE] ?: "",
                testBenchVerified = prefs[Keys.TEST_BENCH_VERIFIED] ?: false,
                lastTestTimestampMillis = prefs[Keys.LAST_TEST_TIMESTAMP] ?: 0L
            )
            val updated = transform(current)
            prefs[Keys.AGENT_ENABLED] = updated.agentEnabled
            prefs[Keys.OPERATION_MODE] = updated.operationMode.name
            prefs[Keys.MESSENGER_ENABLED] = updated.messengerEnabled
            prefs[Keys.IMO_ENABLED] = updated.imoEnabled
            prefs[Keys.PREFERRED_LANGUAGE] = updated.preferredLanguage.name
            prefs[Keys.REPLY_TONE] = updated.replyTone.name
            prefs[Keys.IGNORE_GROUP_CHATS] = updated.ignoreGroupChats
            prefs[Keys.COOLDOWN_SECONDS] = updated.cooldownSeconds.coerceIn(30, 3600)
            prefs[Keys.STORE_MESSAGE_PREVIEW] = updated.storeMessagePreviewInLogs
            prefs[Keys.STRICT_SENSITIVE_SHIELD] = true // Mandatory safety floor
            prefs[Keys.ALLOWLIST_ONLY] = updated.allowlistOnly
            prefs[Keys.ALLOWED_SENDERS_CSV] = updated.allowedSendersCsv
            prefs[Keys.BLOCKED_SENDERS_CSV] = updated.blockedSendersCsv
            prefs[Keys.CUSTOM_SIGNATURE] = updated.customSignature
            prefs[Keys.TEST_BENCH_VERIFIED] = updated.testBenchVerified
            prefs[Keys.LAST_TEST_TIMESTAMP] = updated.lastTestTimestampMillis
        }
    }
}
