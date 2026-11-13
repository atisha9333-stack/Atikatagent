package com.atik.aiagent

import com.atik.aiagent.security.SensitiveContentFilter
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveContentFilterTest {

    @Test
    fun blocksEnglishOtpMessages() {
        val verdict = SensitiveContentFilter.evaluate(
            sender = "Service",
            message = "Your verification code is 492817. Valid for 5 minutes."
        )
        assertTrue(verdict is SensitiveContentFilter.FilterVerdict.Blocked)
    }

    @Test
    fun blocksBanglaOtpAndBkashMessages() {
        val otpVerdict = SensitiveContentFilter.evaluate(
            sender = "Alert",
            message = "আপনার ওটিপি কোড হলো 839201"
        )
        val bkashVerdict = SensitiveContentFilter.evaluate(
            sender = "bKash",
            message = "বিকাশ টাকা পাঠানো সফল হয়েছে TrxID 9X2K1L"
        )
        assertTrue(otpVerdict is SensitiveContentFilter.FilterVerdict.Blocked)
        assertTrue(bkashVerdict is SensitiveContentFilter.FilterVerdict.Blocked)
    }

    @Test
    fun blocksPasswordsAndCardNumbers() {
        val pwdVerdict = SensitiveContentFilter.evaluate(
            sender = "Friend",
            message = "My wifi password is correcthorse99"
        )
        val cardVerdict = SensitiveContentFilter.evaluate(
            sender = "Friend",
            message = "Here is card 4532 7182 9910 2211"
        )
        assertTrue(pwdVerdict is SensitiveContentFilter.FilterVerdict.Blocked)
        assertTrue(cardVerdict is SensitiveContentFilter.FilterVerdict.Blocked)
    }

    @Test
    fun allowsNormalBanglaAndEnglishConversations() {
        val bangla = SensitiveContentFilter.evaluate(
            sender = "Sakib",
            message = "ভাই আজকে সন্ধ্যায় কফি খাবেন?"
        )
        val english = SensitiveContentFilter.evaluate(
            sender = "Nadia",
            message = "Hey Atik, are you free for a quick call around 6 PM?"
        )
        assertTrue(bangla is SensitiveContentFilter.FilterVerdict.Safe)
        assertTrue(english is SensitiveContentFilter.FilterVerdict.Safe)
    }
}
