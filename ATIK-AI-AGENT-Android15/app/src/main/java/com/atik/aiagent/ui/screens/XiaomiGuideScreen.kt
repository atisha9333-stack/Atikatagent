package com.atik.aiagent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atik.aiagent.ui.theme.AtikColors
import com.atik.aiagent.util.XiaomiDeviceHelper

@Composable
fun XiaomiGuideScreen(
    hasNotificationAccess: Boolean,
    isBatteryUnrestricted: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AtikColors.ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Redmi Turbo 3 • HyperOS China ROM Guide",
            color = AtikColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Detected Device: ${XiaomiDeviceHelper.deviceSummary()}",
            color = AtikColors.AccentCyan,
            fontSize = 12.sp
        )

        GuideStepCard(
            step = "Step 1: Allow Restricted Settings (Android 15 China ROM)",
            body = "If you sideloaded the APK instead of Play Store, HyperOS blocks Notification Access by default. Open App Info -> tap the top-right 3-dot menu (⋮) -> Allow restricted settings -> verify fingerprint/PIN.",
            buttonText = "Open App Info",
            onClick = { XiaomiDeviceHelper.openAppDetailsSettings(context) }
        )

        GuideStepCard(
            step = "Step 2: Enable Notification Access (${if (hasNotificationAccess) "Granted" else "Action Needed"})",
            body = "Enable ATIK AI AGENT Notification Bridge so Android binds NotificationListenerService cleanly without Accessibility or Device Admin.",
            buttonText = "Open Notification Access",
            onClick = { XiaomiDeviceHelper.openNotificationListenerSettings(context) }
        )

        GuideStepCard(
            step = "Step 3: Battery Saver -> No Restrictions (${if (isBatteryUnrestricted) "Unrestricted" else "Restricted"})",
            body = "HyperOS China ROM aggressively kills background services. In App Info -> Battery saver -> select 'No restrictions'.",
            buttonText = "Open Battery Settings",
            onClick = { XiaomiDeviceHelper.openBatteryOptimizationSettings(context) }
        )

        GuideStepCard(
            step = "Step 4: Enable Autostart & Lock in Recents",
            body = "Open Xiaomi Security -> Permissions -> Autostart -> enable ATIK AI AGENT. Also open Recent Apps, long-press ATIK AI AGENT, and tap the Padlock icon so HyperOS does not swipe-kill the listener.",
            buttonText = "Open Xiaomi Autostart",
            onClick = { XiaomiDeviceHelper.openXiaomiAutostartSettings(context) }
        )

        GuideStepCard(
            step = "Step 5: Google Basic Services for App Check (China ROM)",
            body = "On Xiaomi China ROM, go to Settings -> Additional settings -> Accounts & sync -> Google Basic Services -> ON so Firebase App Check (Play Integrity) can attest release builds. For local USB debug builds, DebugAppCheckProviderFactory is automatically used.",
            buttonText = "Open App Info",
            onClick = { XiaomiDeviceHelper.openAppDetailsSettings(context) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GuideStepCard(
    step: String,
    body: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AtikColors.SurfaceDark),
        border = BorderStroke(1.dp, AtikColors.HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = step,
                color = AtikColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = body,
                color = AtikColors.TextSecondary,
                fontSize = 12.sp
            )
            OutlinedButton(
                onClick = onClick,
                border = BorderStroke(1.dp, AtikColors.AccentPurple)
            ) {
                Text(
                    buttonText,
                    color = AtikColors.TextPrimary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
