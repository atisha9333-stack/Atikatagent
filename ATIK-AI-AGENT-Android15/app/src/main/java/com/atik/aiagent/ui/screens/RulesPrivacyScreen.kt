package com.atik.aiagent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atik.aiagent.data.model.AgentSettings
import com.atik.aiagent.data.model.OperationMode
import com.atik.aiagent.data.model.ReplyLanguage
import com.atik.aiagent.data.model.ReplyTone
import com.atik.aiagent.ui.theme.AtikColors

@Composable
fun RulesPrivacyScreen(
    settings: AgentSettings,
    onSetOperationMode: (OperationMode) -> Unit,
    onSetMessenger: (Boolean) -> Unit,
    onSetImo: (Boolean) -> Unit,
    onSetLanguage: (ReplyLanguage) -> Unit,
    onSetTone: (ReplyTone) -> Unit,
    onSetIgnoreGroups: (Boolean) -> Unit,
    onSetCooldownSeconds: (Int) -> Unit,
    onSetStorePreview: (Boolean) -> Unit,
    onSetAllowlistOnly: (Boolean) -> Unit,
    onSetAllowedCsv: (String) -> Unit,
    onSetBlockedCsv: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AtikColors.ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Reply Rules & Privacy Controls",
            color = AtikColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Mode & Supported Apps
        SectionCard(title = "Automation Mode & Target Apps") {
            ToggleRow(
                title = "Auto-Reply Mode (Off = Manual Approval Default)",
                subtitle = if (settings.testBenchVerified) {
                    "Requires explicit toggle. Manual approval is safer."
                } else {
                    "Locked until Test Bench RemoteInput check passes."
                },
                checked = settings.operationMode == OperationMode.AUTO_REPLY,
                onCheckedChange = { auto ->
                    onSetOperationMode(if (auto) OperationMode.AUTO_REPLY else OperationMode.MANUAL_APPROVAL)
                }
            )
            HorizontalDivider(color = AtikColors.HairlineBorder)
            ToggleRow(
                title = "Messenger (com.facebook.orca)",
                subtitle = "Detect incoming Messenger notifications & RemoteInput",
                checked = settings.messengerEnabled,
                onCheckedChange = onSetMessenger
            )
            ToggleRow(
                title = "imo (com.imo.android.imoim*)",
                subtitle = "Detect imo notifications; skips call/media alerts without RemoteInput",
                checked = settings.imoEnabled,
                onCheckedChange = onSetImo
            )
            ToggleRow(
                title = "Ignore Group Chats",
                subtitle = "Recommended to prevent noisy group reply loops",
                checked = settings.ignoreGroupChats,
                onCheckedChange = onSetIgnoreGroups
            )
        }

        // Language & Tone
        SectionCard(title = "Gemini Language & Tone (Firebase AI Logic)") {
            Text(text = "Reply Language", color = AtikColors.TextSecondary, fontSize = 12.sp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ReplyLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = settings.preferredLanguage == lang,
                        onClick = { onSetLanguage(lang) },
                        label = { Text(lang.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AtikColors.AccentPurple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Tone", color = AtikColors.TextSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReplyTone.entries.forEach { tone ->
                    FilterChip(
                        selected = settings.replyTone == tone,
                        onClick = { onSetTone(tone) },
                        label = { Text(tone.label.substringBefore(" "), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AtikColors.AccentBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Anti-Loop & Contact Rules
        SectionCard(title = "Anti-Loop Cooldown & Contact Filters") {
            Text(
                text = "Per-Conversation Cooldown: ${settings.cooldownSeconds}s",
                color = AtikColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = settings.cooldownSeconds.toFloat(),
                onValueChange = { onSetCooldownSeconds(it.toInt()) },
                valueRange = 30f..600f,
                colors = SliderDefaults.colors(
                    thumbColor = AtikColors.AccentPurple,
                    activeTrackColor = AtikColors.AccentBlue
                )
            )
            ToggleRow(
                title = "Allowlist Contacts Only",
                subtitle = "Only generate replies for names listed below",
                checked = settings.allowlistOnly,
                onCheckedChange = onSetAllowlistOnly
            )
            OutlinedTextField(
                value = settings.allowedSendersCsv,
                onValueChange = onSetAllowedCsv,
                label = { Text("Allowed Senders (comma-separated)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AtikColors.AccentPurple,
                    unfocusedBorderColor = AtikColors.HairlineBorder,
                    focusedTextColor = AtikColors.TextPrimary,
                    unfocusedTextColor = AtikColors.TextPrimary
                )
            )
            OutlinedTextField(
                value = settings.blockedSendersCsv,
                onValueChange = onSetBlockedCsv,
                label = { Text("Blocked Senders (comma-separated)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AtikColors.AccentPurple,
                    unfocusedBorderColor = AtikColors.HairlineBorder,
                    focusedTextColor = AtikColors.TextPrimary,
                    unfocusedTextColor = AtikColors.TextPrimary
                )
            )
        }

        // Privacy Settings
        SectionCard(title = "Privacy & Data Minimization") {
            ToggleRow(
                title = "Strict OTP, Password & Payment Shield",
                subtitle = "Always enforced locally before Gemini calls (cannot be disabled)",
                checked = true,
                onCheckedChange = {}
            )
            ToggleRow(
                title = "Store Message Snippets in Local Log",
                subtitle = "Default OFF: logs record only app, sender, outcome, and timestamp",
                checked = settings.storeMessagePreviewInLogs,
                onCheckedChange = onSetStorePreview
            )
            Text(
                text = "Zero Cloud History Upload: Full notification history is never synced or uploaded.",
                color = AtikColors.AccentCyan,
                fontSize = 12.sp
            )
            Button(
                onClick = onClearLogs,
                colors = ButtonDefaults.buttonColors(containerColor = AtikColors.DangerRose)
            ) {
                Text("Wipe Local Activity Log & Dedup Cache")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AtikColors.SurfaceDark),
        border = BorderStroke(1.dp, AtikColors.HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = AtikColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = AtikColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = AtikColors.TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AtikColors.AccentPurple
            )
        )
    }
}
