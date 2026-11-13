package com.atik.aiagent.ai

import com.atik.aiagent.data.model.ReplyLanguage
import com.atik.aiagent.data.model.ReplyTone
import com.atik.aiagent.data.model.SupportedApp
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates natural Bangla (বাংলা / Banglish) and English message drafts
 * via Gemini using Firebase AI Logic + Firebase App Check protection.
 *
 * Only non-sensitive message text (already cleared by SensitiveContentFilter) is passed.
 */
class GeminiReplyGenerator {

    data class GeneratedDraft(
        val replyText: String,
        val detectedLanguage: String
    )

    suspend fun generateReply(
        app: SupportedApp,
        senderName: String,
        incomingMessage: String,
        language: ReplyLanguage,
        tone: ReplyTone,
        customSignature: String
    ): Result<GeneratedDraft> = withContext(Dispatchers.IO) {
        runCatching {
            val systemPrompt = buildString {
                appendLine("You are ATIK AI AGENT, a polite personal messaging assistant for ATIK.")
                appendLine("Your job is to draft a short, natural, human-sounding reply to a ${app.displayName} message.")
                appendLine("Language rule: ${language.instruction}")
                appendLine("Tone: ${tone.label}.")
                appendLine("Constraints:")
                appendLine("- Keep the reply concise (1 to 2 natural sentences, under 180 characters when possible).")
                appendLine("- Never invent promises, times, or financial commitments.")
                appendLine("- Never ask for OTPs, passwords, PINs, or payment details.")
                appendLine("- Output ONLY the reply text itself, without quotes, markdown, or explanations.")
            }

            // Firebase AI Logic SDK (protected automatically by Firebase App Check)
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = "gemini-2.5-flash",
                systemInstruction = content { text(systemPrompt) }
            )

            val userPrompt = buildString {
                appendLine("Sender: $senderName")
                appendLine("Message: $incomingMessage")
            }

            val response = model.generateContent(userPrompt)
            val rawReply = response.text?.trim()?.removeSurrounding("\"")
                ?: error("Gemini returned an empty reply.")

            val finalReply = if (customSignature.isNotBlank()) {
                "$rawReply $customSignature".trim()
            } else {
                rawReply
            }

            val detectedLang = detectLanguageLabel(finalReply)
            GeneratedDraft(replyText = finalReply, detectedLanguage = detectedLang)
        }
    }

    private fun detectLanguageLabel(text: String): String {
        val hasBanglaScript = text.any { it in '\u0980'..'\u09FF' }
        return if (hasBanglaScript) "Bangla (বাংলা)" else "English / Romanized"
    }
}
