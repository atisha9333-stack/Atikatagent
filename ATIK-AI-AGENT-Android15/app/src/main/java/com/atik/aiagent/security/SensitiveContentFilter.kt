package com.atik.aiagent.security

/**
 * Privacy-first local inspection filter.
 * Runs BEFORE any message leaves the device for Gemini / Firebase AI Logic.
 * Blocks OTPs, passwords, mobile banking (bKash/Nagad/Rocket/Upay), cards, and sensitive IDs.
 */
object SensitiveContentFilter {

    enum class SensitiveCategory(val label: String) {
        OTP_VERIFICATION("OTP / Verification Code"),
        CREDENTIALS_PASSWORD("Password / Token / PIN"),
        FINANCIAL_PAYMENT("Payment / Bank / MFS Alert"),
        PERSONAL_IDENTITY("NID / Passport / Sensitive ID")
    }

    sealed class FilterVerdict {
        data object Safe : FilterVerdict()
        data class Blocked(
            val category: SensitiveCategory,
            val reason: String
        ) : FilterVerdict()
    }

    private val otpContextRegex = Regex(
        pattern = """(?i)\b(otp|one[\s-]?time|verification\s*code|verify\s*code|login\s*code|auth\s*code|2fa|security\s*code|passcode|pin\s*code)\b|ওটিপি|ভেরিফিকেশন|যাচাইকরণ|গোপন\s*কোড|লগইন\s*কোড""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val digitCodeRegex = Regex("""\b\d{4,8}\b""")

    private val credentialsRegex = Regex(
        pattern = """(?i)\b(password|passwd|pwd|secret\s*key|api\s*key|bearer\s*token|recovery\s*phrase|seed\s*phrase|private\s*key|atm\s*pin|mpin|tpin)\b|পাসওয়ার্ড|পাসওয়ার্ড|গোপন\s*নম্বর|পিন\s*নম্বর""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val financialRegex = Regex(
        pattern = """(?i)\b(bkash|nagad|rocket|upay|visa|mastercard|amex|cvv|cvc|iban|swift|trxid|txnid|cash\s*in|cash\s*out|send\s*money|payment\s*received|bank\s*account|card\s*ending|debit\s*card|credit\s*card|balance\s*is)\b|বিকাশ|নগদ|রকেট|উপায়|উপায়|টাকা\s*পাঠানো|ক্যাশ\s*আউট|ক্যাশ\s*ইন|লেনদেন|ব্যাংক\s*অ্যাকাউন্ট""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val cardNumberRegex = Regex("""\b(?:\d[ -]*?){13,19}\b""")

    private val identityRegex = Regex(
        pattern = """(?i)\b(nid\s*no|national\s*id|passport\s*no|ssn|social\s*security|tin\s*certificate|driving\s*license)\b|এনআইডি|জাতীয়\s*পরিচয়পত্র|জাতীয়\s*পরিচয়পত্র|পাসপোর্ট\s*নম্বর""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun evaluate(sender: String, message: String): FilterVerdict {
        val combined = "$sender $message".trim()
        if (combined.isBlank()) {
            return FilterVerdict.Blocked(
                category = SensitiveCategory.CREDENTIALS_PASSWORD,
                reason = "Empty notification payload skipped"
            )
        }

        if (otpContextRegex.containsMatchIn(combined) &&
            (digitCodeRegex.containsMatchIn(message) || combined.contains("code", ignoreCase = true) || combined.contains("কোড"))
        ) {
            return FilterVerdict.Blocked(
                category = SensitiveCategory.OTP_VERIFICATION,
                reason = "OTP or verification code detected; blocked locally"
            )
        }

        if (credentialsRegex.containsMatchIn(combined)) {
            return FilterVerdict.Blocked(
                category = SensitiveCategory.CREDENTIALS_PASSWORD,
                reason = "Password, PIN, or secret credential detected; blocked locally"
            )
        }

        if (financialRegex.containsMatchIn(combined) || cardNumberRegex.containsMatchIn(message)) {
            return FilterVerdict.Blocked(
                category = SensitiveCategory.FINANCIAL_PAYMENT,
                reason = "Payment, MFS (bKash/Nagad), or bank details detected; blocked locally"
            )
        }

        if (identityRegex.containsMatchIn(combined)) {
            return FilterVerdict.Blocked(
                category = SensitiveCategory.PERSONAL_IDENTITY,
                reason = "Government ID or sensitive personal document info detected; blocked locally"
            )
        }

        return FilterVerdict.Safe
    }
}
