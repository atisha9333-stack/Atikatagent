package com.atik.aiagent.di

import android.content.Context
import com.atik.aiagent.ai.GeminiReplyGenerator
import com.atik.aiagent.data.local.AtikDatabase
import com.atik.aiagent.data.preferences.AgentPreferencesRepository
import com.atik.aiagent.security.DeduplicationGuard
import com.atik.aiagent.service.RemoteInputDispatcher

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AtikDatabase by lazy { AtikDatabase.getInstance(appContext) }
    val activityLogDao by lazy { database.activityLogDao() }
    val preferencesRepository by lazy { AgentPreferencesRepository(appContext) }
    val deduplicationGuard by lazy { DeduplicationGuard() }
    val remoteInputDispatcher by lazy { RemoteInputDispatcher(appContext) }
    val geminiReplyGenerator by lazy { GeminiReplyGenerator() }
}
