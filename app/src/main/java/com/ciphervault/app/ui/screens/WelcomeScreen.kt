package com.ciphervault.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType

/**
 * First-time welcome screen.
 * The user chooses to either create a new vault or import an existing one.
 *
 * Layout: the brand sits in the upper, roomy part of the screen, while the warning and the
 * two actions are anchored at the bottom, within thumb reach.
 */
@Composable
fun WelcomeScreen(
    onCreateVault: () -> Unit,
    onImportVault: () -> Unit
) {
    // One entrance sequence: rings, then the title, then the actions.
    val entrance = remember { MutableTransitionState(false).apply { targetState = true } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Ground)
            .systemBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        // ---- Brand ----
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VaultEmblem(entrance)

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visibleState = entrance,
                enter = fadeIn(tween(600, delayMillis = 350)) +
                        slideInVertically(tween(600, delayMillis = 350)) { it / 6 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "KeyZone",
                            style = VaultType.PageTitle,
                            color = VaultColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "2.0",
                            style = VaultType.Meta,
                            color = VaultColors.Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(VaultColors.AccentTint)
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Your files are encrypted with AES-256-GCM.\nYour PIN or password is the only key.",
                        style = VaultType.Meta,
                        color = VaultColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // ---- Warning and actions ----
        AnimatedVisibility(
            visibleState = entrance,
            enter = fadeIn(tween(500, delayMillis = 600)) +
                    slideInVertically(tween(500, delayMillis = 600)) { it / 4 }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                NoRecoveryNotice()

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onCreateVault,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultColors.Accent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Create new vault", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onImportVault,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, VaultColors.Hairline),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = VaultColors.Surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        tint = VaultColors.Accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Import existing vault",
                        color = VaultColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Losing the credentials means losing the data, so this is shown at full contrast
 * instead of as faded fine print.
 */
@Composable
private fun NoRecoveryNotice() {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VaultColors.WarningAmber.copy(alpha = 0.08f))
            .border(1.dp, VaultColors.WarningAmber.copy(alpha = 0.35f), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = VaultColors.WarningAmber,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Zero-knowledge protection",
                style = VaultType.ItemTitle.copy(fontSize = 14.sp),
                color = VaultColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "We can't recover your files if you lose your PIN, password or 2FA credentials.",
                style = VaultType.Meta,
                color = VaultColors.TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

/** A shield inside three concentric rings that settle in one after another. */
@Composable
private fun VaultEmblem(entrance: MutableTransitionState<Boolean>) {
    Box(
        modifier = Modifier.size(208.dp),
        contentAlignment = Alignment.Center
    ) {
        EmblemRing(entrance, size = 208.dp, borderAlpha = 0.12f, fill = Color.Transparent, delay = 0)
        EmblemRing(entrance, size = 156.dp, borderAlpha = 0.24f, fill = Color.Transparent, delay = 120)
        EmblemRing(entrance, size = 108.dp, borderAlpha = 0.45f, fill = VaultColors.AccentTint, delay = 240)

        AnimatedVisibility(
            visibleState = entrance,
            enter = fadeIn(tween(500, delayMillis = 400)) +
                    scaleIn(tween(500, delayMillis = 400), initialScale = 0.7f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = VaultColors.Accent,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
private fun EmblemRing(
    entrance: MutableTransitionState<Boolean>,
    size: Dp,
    borderAlpha: Float,
    fill: Color,
    delay: Int
) {
    AnimatedVisibility(
        visibleState = entrance,
        enter = fadeIn(tween(700, delayMillis = delay)) +
                scaleIn(tween(700, delayMillis = delay), initialScale = 0.75f)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(fill)
                .border(1.dp, VaultColors.Accent.copy(alpha = borderAlpha), CircleShape)
        )
    }
}