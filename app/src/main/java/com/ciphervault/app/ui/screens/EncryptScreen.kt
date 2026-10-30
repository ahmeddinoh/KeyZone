package com.ciphervault.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.ciphervault.app.ui.components.rememberNavPillClearance
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.VaultViewModel

@Composable
fun EncryptScreen(
    vaultViewModel: VaultViewModel,
) {
    var requirePassword by remember { mutableStateOf(true) }
    var deleteOriginal by remember { mutableStateOf(false) }
    var selectedAlgorithm by remember { mutableStateOf("AES-256") }
    val algorithms = listOf("AES-256", "ChaCha20", "RSA-4096")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 22.dp, end = 22.dp,
            top = 64.dp,
            bottom = rememberNavPillClearance(),
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Header ──
        item {
            Column {
                Text(
                    text = "Encrypt",
                    style = VaultType.PageTitle,
                    color = VaultColors.TextPrimary
                )
                Text(
                    text = "Securely lock your files in the vault.",
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary
                )
            }
        }

        // ── Drop Zone ──
        item {
            val dashedStroke = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(VaultColors.Accent.copy(alpha = 0.35f))
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    // Apply a custom dashed border using drawBehind or simply basic modifier if possible
                    // Let's implement dashed border correctly:
                    .clip(RoundedCornerShape(22.dp))
                    .background(VaultColors.AccentTint.copy(alpha = 0.05f))
                    .clickable { /* trigger file picker */ },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                        color = VaultColors.Accent.copy(alpha = 0.35f),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(VaultColors.AccentTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = VaultColors.Accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Choose a file to encrypt",
                        style = VaultType.ItemTitle,
                        color = VaultColors.TextPrimary
                    )
                    Text(
                        text = "Images, documents, or media",
                        style = VaultType.Meta,
                        color = VaultColors.TextSecondary
                    )
                }
            }
        }

        // ── Algorithm Selector ──
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(VaultColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    algorithms.forEach { algo ->
                        val isSelected = selectedAlgorithm == algo
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(if (isSelected) VaultColors.SurfaceRaised else Color.Transparent)
                                .clickable { selectedAlgorithm = algo },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = algo,
                                style = VaultType.Chip,
                                color = if (isSelected) VaultColors.TextPrimary else VaultColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ── Options ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OptionToggleCard(
                    title = "Require password",
                    subtitle = "Prompt for password on unlock",
                    checked = requirePassword,
                    onCheckedChange = { requirePassword = it }
                )
                OptionToggleCard(
                    title = "Delete original",
                    subtitle = "Remove source file after encrypting",
                    checked = deleteOriginal,
                    onCheckedChange = { deleteOriginal = it }
                )
            }
        }

        // ── Primary Button ──
        item {
            Button(
                onClick = { /* Encrypt */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultColors.Accent,
                    contentColor = VaultColors.OnAccent
                )
            ) {
                Text(
                    text = "Encrypt file",
                    style = VaultType.Button
                )
            }
        }
    }
}

@Composable
private fun OptionToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultColors.Hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = VaultType.ItemTitle,
                    color = VaultColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VaultColors.OnAccent,
                    checkedTrackColor = VaultColors.Accent,
                    uncheckedThumbColor = VaultColors.TextSecondary,
                    uncheckedTrackColor = VaultColors.Ground,
                    uncheckedBorderColor = VaultColors.Hairline
                )
            )
        }
    }
}
