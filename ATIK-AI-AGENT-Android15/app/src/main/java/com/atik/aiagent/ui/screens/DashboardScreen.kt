package com.atik.aiagent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atik.aiagent.data.model.AgentSettings
import com.atik.aiagent.data.model.OperationMode
import com.atik.aiagent.data.model.PendingDraft
import com.atik.aiagent.ui.theme.AtikColors
import com.atik.aiagent.util.XiaomiDeviceHelper

@Composable
fun DashboardScreen(
    settings: AgentSettings,
    hasNotificationAccess: Boolean,
    isListenerConnected: Boolean,
    isBatteryUnrestricted: Boolean,
    pendingDrafts: List<PendingDraft>,
    onToggleAgent: (Boolean) -> Unit,
    onUpdateDraftText: (String, String) -> Unit,
    onApproveDraft: (PendingDraft) -> Unit,
    onDismissDraft: (PendingDraft) -> Unit,
    onNavigateToTestBench: () -> Unit,
    onNavigateToXiaomiGuide: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AtikColors.ObsidianBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Futuristic Header Card with Purple-Blue Gradient Accent
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AtikColors.SurfaceDark),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AtikColors.AccentPurple.copy(alpha = 0.7f),
                            AtikColors.AccentBlue.copy(alpha = 0.6f)
                        )
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ATIK AI AGENT",
                                color = AtikColors.TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Redmi Turbo 3 • Android 15 • Firebase AI Logic",
                                color = AtikColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = settings.agentEnabled,
                            onCheckedChange = onToggleAgent,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AtikColors.AccentPurple
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = AtikColors.HairlineBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusMetric(
                            label = "Listener",
                            value = when {
                                !hasNotificationAccess -> "Access Needed"
                                isListenerConnected -> "Connected"
                                else -> "Standby"
                            },
                            color = if (hasNotificationAccess && isListenerConnected) {
                                AtikColors.SuccessEmerald
                            } else {
                                AtikColors.WarningAmber
                            }
                        )
                        StatusMetric(
                            label = "Reply Mode",
                            value = if (settings.operationMode == OperationMode.MANUAL_APPROVAL) {
                                "Manual (Default)"
                            } else {
                                "Auto-Reply ON"
                            },
                            color = if (settings.operationMode == OperationMode.MANUAL_APPROVAL) {
                                AtikColors.AccentCyan
                            } else {
                                AtikColors.AccentVioletGlow
                            }
                        )
                        StatusMetric(
                            label = "Test Check",
                            value = if (settings.testBenchVerified) "Verified" else "Pending",
                            color = if (settings.testBenchVerified) {
                                AtikColors.SuccessEmerald
                            } else {
                                AtikColors.WarningAmber
                            }
                        )
                    }
                }
            }
        }

        if (!hasNotificationAccess) {
            item {
                ActionAlertCard(
                    title = "Grant Notification Access",
                    description = "On Redmi Turbo 3 (China ROM), unlock Restricted Settings first in App Info before enabling Notification Access.",
                    buttonLabel = "Open Notification Access",
                    onClick = { XiaomiDeviceHelper.openNotificationListenerSettings(context) }
                )
            }
        }

        if (!isBatteryUnrestricted) {
            item {
                ActionAlertCard(
                    title = "HyperOS Battery Restriction Detected",
                    description = "Xiaomi China ROM may suspend background notification replies. Set Battery Saver to No Restrictions.",
                    buttonLabel = "Open Xiaomi Guide",
                    onClick = onNavigateToXiaomiGuide
                )
            }
        }

        if (!settings.testBenchVerified) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AtikColors.SurfaceElevated),
                    border = BorderStroke(1.dp, AtikColors.AccentBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stage 1: Verify RemoteInput Before AI Auto-Reply",
                                color = AtikColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Test Messenger & imo notification detection separately first.",
                                color = AtikColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onNavigateToTestBench,
                            colors = ButtonDefaults.buttonColors(containerColor = AtikColors.AccentBlue)
                        ) {
                            Text("Run Test", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manual Approval Queue (${pendingDrafts.size})",
                    color = AtikColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Messenger & imo",
                    color = AtikColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (pendingDrafts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AtikColors.SurfaceDark),
                    border = BorderStroke(1.dp, AtikColors.HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = AtikColors.AccentPurple,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No pending drafts right now",
                            color = AtikColors.TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Incoming Messenger or imo messages with RemoteInput will appear here with natural Bangla/English drafts for your approval.",
                            color = AtikColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(pendingDrafts, key = { it.id }) { draft ->
                PendingDraftCard(
                    draft = draft,
                    onEditReply = { updated -> onUpdateDraftText(draft.id, updated) },
                    onApprove = { onApproveDraft(draft) },
                    onDismiss = { onDismissDraft(draft) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusMetric(label: String, value: String, color: Color) {
    Column {
        Text(text = label, color = AtikColors.TextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionAlertCard(
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AtikColors.SurfaceDark),
        border = BorderStroke(1.dp, AtikColors.WarningAmber.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = AtikColors.WarningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = AtikColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                color = AtikColors.TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = AtikColors.AccentPurple)
            ) {
                Text(buttonLabel, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PendingDraftCard(
    draft: PendingDraft,
    onEditReply: (String) -> Unit,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AtikColors.SurfaceDark),
        border = BorderStroke(1.dp, AtikColors.HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${draft.app.displayName} • ${draft.senderName}",
                    color = AtikColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = draft.detectedLanguage,
                    color = AtikColors.AccentCyan,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AtikColors.ObsidianBg, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = draft.incomingPreview,
                    color = AtikColors.TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = draft.draftedReply,
                onValueChange = onEditReply,
                label = { Text("Gemini Draft (Editable)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AtikColors.AccentPurple,
                    unfocusedBorderColor = AtikColors.HairlineBorder,
                    focusedTextColor = AtikColors.TextPrimary,
                    unfocusedTextColor = AtikColors.TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, AtikColors.HairlineBorder)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dismiss")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1.4f),
                    colors = ButtonDefaults.buttonColors(containerColor = AtikColors.AccentPurple)
                ) {
                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Approve & Send")
                }
            }
        }
    }
}
