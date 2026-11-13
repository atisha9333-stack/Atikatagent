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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atik.aiagent.service.AtikNotificationListenerService
import com.atik.aiagent.ui.theme.AtikColors

@Composable
fun TestBenchScreen(
    hasNotificationAccess: Boolean,
    isListenerConnected: Boolean,
    testBenchVerified: Boolean,
    lastObservation: AtikNotificationListenerService.TestObservation?,
    lastEchoReceived: String?,
    filterTestResult: String?,
    onRunRemoteInputTest: () -> Unit,
    onRunUnavailableTest: () -> Unit,
    onRunSensitiveFilterCheck: (String) -> Unit
) {
    var sampleMessage by remember { mutableStateOf("Your bKash OTP is 482910. Do not share.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AtikColors.ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Stage 1: Detection & RemoteInput Test Bench",
            color = AtikColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Verify notification listener binding, RemoteInput reply round-trip, and graceful unavailable-action handling BEFORE turning on AI automation.",
            color = AtikColors.TextSecondary,
            fontSize = 13.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AtikColors.SurfaceDark),
            border = BorderStroke(1.dp, AtikColors.HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1. Live RemoteInput Round-Trip Test",
                    color = AtikColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Notification Access: ${if (hasNotificationAccess) "Granted" else "Missing"} • Service Bound: ${if (isListenerConnected) "Yes" else "No"} • Verified: ${if (testBenchVerified) "PASSED" else "Not Yet"}",
                    color = if (testBenchVerified) AtikColors.SuccessEmerald else AtikColors.AccentCyan,
                    fontSize = 12.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRunRemoteInputTest,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AtikColors.AccentPurple)
                    ) {
                        Text("Test RemoteInput", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onRunUnavailableTest,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, AtikColors.AccentBlue)
                    ) {
                        Text("Test No-Reply", fontSize = 12.sp)
                    }
                }

                if (lastObservation != null) {
                    Text(
                        text = "Last Observed: ${lastObservation.detail}",
                        color = AtikColors.TextPrimary,
                        fontSize = 12.sp
                    )
                }
                if (lastEchoReceived != null) {
                    Text(
                        text = "Echo Receiver Confirmed: \"$lastEchoReceived\"",
                        color = AtikColors.SuccessEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AtikColors.SurfaceDark),
            border = BorderStroke(1.dp, AtikColors.HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2. Sensitive Content Shield Tester (Local Only)",
                    color = AtikColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                OutlinedTextField(
                    value = sampleMessage,
                    onValueChange = { sampleMessage = it },
                    label = { Text("Sample incoming message (Bangla / English)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtikColors.AccentBlue,
                        unfocusedBorderColor = AtikColors.HairlineBorder,
                        focusedTextColor = AtikColors.TextPrimary,
                        unfocusedTextColor = AtikColors.TextPrimary
                    )
                )
                Button(
                    onClick = { onRunSensitiveFilterCheck(sampleMessage) },
                    colors = ButtonDefaults.buttonColors(containerColor = AtikColors.AccentBlue)
                ) {
                    Text("Evaluate Safety Filter")
                }
                if (filterTestResult != null) {
                    Text(
                        text = filterTestResult,
                        color = if (filterTestResult.startsWith("BLOCKED")) AtikColors.WarningAmber else AtikColors.SuccessEmerald,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
