package com.ciphervault.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ciphervault.app.ui.components.rememberNavPillClearance
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.AuthViewModel
import com.ciphervault.app.viewmodel.VaultViewModel

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    vaultViewModel: VaultViewModel,
    onNavigateToBackup: () -> Unit,
    onNavigateToChangeKey: () -> Unit
) {
    var biometricOn by remember { mutableStateOf(true) }
    var twoFactorOn by remember { mutableStateOf(true) }
    var encryptedBackupOn by remember { mutableStateOf(true) }
    var darkThemeOn by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 22.dp, end = 22.dp,
            top = 64.dp,
            bottom = rememberNavPillClearance(),
        ),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // ── Header & Profile ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(
                    text = "Settings",
                    style = VaultType.PageTitle,
                    color = VaultColors.TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(VaultColors.SurfaceRaised),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AK",
                            style = VaultType.SectionTitle,
                            color = VaultColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alex Kim",
                            style = VaultType.ItemTitle,
                            color = VaultColors.TextPrimary
                        )
                        Text(
                            text = "alex.kim@example.com",
                            style = VaultType.Meta,
                            color = VaultColors.TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = VaultColors.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // ── Security Group ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SECURITY",
                    style = VaultType.GroupLabel,
                    color = VaultColors.TextSecondary,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultColors.Hairline)
                ) {
                    Column {
                        SettingsToggleRow(
                            icon = Icons.Outlined.Shield,
                            label = "Biometric unlock",
                            checked = biometricOn,
                            onCheckedChange = { biometricOn = it }
                        )
                        HorizontalDivider(color = VaultColors.Hairline, modifier = Modifier.padding(start = 56.dp))
                        SettingsValueRow(
                            icon = Icons.Outlined.Schedule,
                            label = "Auto-lock",
                            value = "1 minute",
                            onClick = { /* show auto-lock options */ }
                        )
                        HorizontalDivider(color = VaultColors.Hairline, modifier = Modifier.padding(start = 56.dp))
                        SettingsToggleRow(
                            icon = Icons.Outlined.Key,
                            label = "Two-factor auth",
                            checked = twoFactorOn,
                            onCheckedChange = { twoFactorOn = it }
                        )
                    }
                }
            }
        }

        // ── Backup & Appearance Group ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "BACKUP & APPEARANCE",
                    style = VaultType.GroupLabel,
                    color = VaultColors.TextSecondary,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultColors.Hairline)
                ) {
                    Column {
                        SettingsToggleRow(
                            icon = Icons.Outlined.Cloud,
                            label = "Encrypted backup",
                            checked = encryptedBackupOn,
                            onCheckedChange = { encryptedBackupOn = it }
                        )
                        HorizontalDivider(color = VaultColors.Hairline, modifier = Modifier.padding(start = 56.dp))
                        SettingsToggleRow(
                            icon = Icons.Outlined.Nightlight,
                            label = "Dark theme",
                            checked = darkThemeOn,
                            onCheckedChange = { darkThemeOn = it }
                        )
                        HorizontalDivider(color = VaultColors.Hairline, modifier = Modifier.padding(start = 56.dp))
                        SettingsValueRow(
                            icon = Icons.Outlined.Info,
                            label = "About Vault",
                            value = "v1.0",
                            onClick = { /* show about screen */ }
                        )
                    }
                }
            }
        }
        
        // Ensure some bottom padding is preserved
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VaultColors.AccentTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VaultColors.Accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = VaultType.ItemTitle,
            color = VaultColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VaultColors.OnAccent,
                checkedTrackColor = VaultColors.Accent,
                uncheckedThumbColor = VaultColors.TextSecondary,
                uncheckedTrackColor = VaultColors.Ground,
                uncheckedBorderColor = VaultColors.Hairline
            ),
            modifier = Modifier.height(24.dp) // standard visual height adjustment
        )
    }
}

@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VaultColors.AccentTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VaultColors.Accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = VaultType.ItemTitle,
            color = VaultColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = VaultType.Meta,
            color = VaultColors.TextSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = VaultColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
