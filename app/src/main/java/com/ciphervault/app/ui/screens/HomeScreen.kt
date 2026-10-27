package com.ciphervault.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.ui.theme.CardBorder
import com.ciphervault.app.ui.theme.CardBorderSubtle
import com.ciphervault.app.ui.theme.CyanAccent
import com.ciphervault.app.ui.theme.CyanSubtle
import com.ciphervault.app.ui.theme.DarkBackground
import com.ciphervault.app.ui.theme.DarkCard
import com.ciphervault.app.ui.theme.DarkCardElevated
import com.ciphervault.app.ui.theme.DarkSurfaceVariant
import com.ciphervault.app.ui.theme.TextPrimary
import com.ciphervault.app.ui.theme.TextSecondary
import com.ciphervault.app.ui.theme.TextTertiary
import com.ciphervault.app.ui.theme.VaultGreen
import com.ciphervault.app.ui.theme.VaultGreenSubtle
import com.ciphervault.app.ui.theme.WarningAmber
import com.ciphervault.app.ui.theme.WarningSubtle
import com.ciphervault.app.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern, refined Dashboard screen showing vault security status and quick actions.
 */
@Composable
fun HomeScreen(
    vaultViewModel: VaultViewModel,
    onNavigateToTab: (String) -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val files by vaultViewModel.files.collectAsState()
    val fileCount by vaultViewModel.fileCount.collectAsState()
    val totalSize by vaultViewModel.totalSize.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header Bar ──
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CipherVault",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VaultGreenSubtle)
                                .border(1.dp, VaultGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v2.0",
                                color = VaultGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Zero-knowledge encrypted storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .border(1.dp, CardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Security Status",
                        tint = VaultGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // ── Hero Security Status Banner ──
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VaultGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = VaultGreenSubtle),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(VaultGreen.copy(alpha = 0.15f))
                            .border(1.dp, VaultGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = VaultGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(VaultGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AES-256-GCM ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = VaultGreen,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Hardware-Grade Encryption",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Key derived via PBKDF2 • Offline TOTP 2FA",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // ── Stats Cards ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Lock,
                    iconColor = CyanAccent,
                    iconBg = CyanSubtle,
                    label = "Files Secured",
                    value = fileCount.toString()
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Storage,
                    iconColor = WarningAmber,
                    iconBg = WarningSubtle,
                    label = "Encrypted Storage",
                    value = formatFileSize(totalSize)
                )
            }
        }

        // ── Quick Actions ──
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Add,
                    label = "Encrypt Files",
                    subtitle = "Add files to vault",
                    color = VaultGreen,
                    onClick = { onNavigateToTab("encrypt") }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Folder,
                    label = "Open Vault",
                    subtitle = "Browse all files",
                    color = CyanAccent,
                    onClick = { onNavigateToTab("vault") }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CloudUpload,
                    label = "Backup & 2FA",
                    subtitle = "Export & sync",
                    color = WarningAmber,
                    onClick = onNavigateToBackup
                )
            }
        }

        // ── Recent Files Header ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Files",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (files.isNotEmpty()) {
                    Text(
                        text = "View all (${files.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToTab("vault") }
                    )
                }
            }
        }

        // Recent Files List or Empty State
        if (files.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Vault is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap \"Encrypt Files\" to add your first secure file",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(files.take(5)) { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VaultGreenSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                tint = VaultGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.originalFileName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = "${formatFileSize(file.originalSize)} • ${formatDate(file.encryptedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Encrypted",
                                tint = VaultGreen.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    label: String,
    value: String
) {
    Card(
        modifier = modifier.border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val idx = digitGroups.coerceIn(0, units.lastIndex)
    return "%.1f %s".format(bytes / Math.pow(1024.0, idx.toDouble()), units[idx])
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
