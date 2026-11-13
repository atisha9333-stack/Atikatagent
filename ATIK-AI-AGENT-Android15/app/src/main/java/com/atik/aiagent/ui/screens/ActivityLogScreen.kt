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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atik.aiagent.data.local.ActivityLogEntity
import com.atik.aiagent.ui.theme.AtikColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityLogScreen(logs: List<ActivityLogEntity>) {
    val formatter = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AtikColors.ObsidianBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Local Activity Log",
                color = AtikColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Stored strictly on-device in Room. Sensitive OTPs and payments are never logged.",
                color = AtikColors.TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AtikColors.SurfaceDark),
                    border = BorderStroke(1.dp, AtikColors.HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No activity events recorded yet.",
                        color = AtikColors.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                val outcomeColor = when {
                    log.outcome.contains("Sent", ignoreCase = true) ||
                        log.outcome.contains("Auto", ignoreCase = true) ||
                        log.outcome.contains("Passed", ignoreCase = true) -> AtikColors.SuccessEmerald
                    log.outcome.contains("Blocked", ignoreCase = true) ||
                        log.outcome.contains("No RemoteInput", ignoreCase = true) -> AtikColors.WarningAmber
                    log.outcome.contains("Failed", ignoreCase = true) -> AtikColors.DangerRose
                    else -> AtikColors.AccentCyan
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AtikColors.SurfaceDark),
                    border = BorderStroke(1.dp, AtikColors.HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${log.appName} • ${log.senderDisplay}",
                                color = AtikColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = formatter.format(Date(log.timestampMillis)),
                                color = AtikColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.outcome,
                            color = outcomeColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        Text(
                            text = log.detailNote,
                            color = AtikColors.TextSecondary,
                            fontSize = 12.sp
                        )
                        if (!log.previewSnippet.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Preview: ${log.previewSnippet}",
                                color = AtikColors.TextSecondary.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
