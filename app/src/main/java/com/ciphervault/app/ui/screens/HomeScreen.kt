package com.ciphervault.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ciphervault.app.ui.components.rememberNavPillClearance
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.VaultViewModel
import java.util.Calendar
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun HomeScreen(
    vaultViewModel: VaultViewModel,
    onNavigateToTab: (String) -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val files by vaultViewModel.files.collectAsState()
    val recentFiles = remember(files) { files.take(5) }
    val totalSize by vaultViewModel.totalSize.collectAsState()

    val greeting = remember { getTimeBasedGreeting() }

    // Smooth entry animation for the vault ring
    var triggerAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggerAnimation = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (triggerAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "VaultRingProgress"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 22.dp,
            end = 22.dp,
            top = 56.dp,
            bottom = rememberNavPillClearance() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        // ── Top Bar & Headline ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live Security Status Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(VaultColors.Accent)
                        )
                        Text(
                            text = greeting,
                            style = VaultType.Meta,
                            color = VaultColors.TextSecondary
                        )
                    }

                    // Backup Action Pill (uses onNavigateToBackup)
                    Surface(
                        onClick = onNavigateToBackup,
                        shape = CircleShape,
                        color = VaultColors.Surface,
                        border = BorderStroke(1.dp, VaultColors.Hairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = "Backup Vault",
                                tint = VaultColors.Accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Backup",
                                style = VaultType.Meta,
                                color = VaultColors.TextPrimary
                            )
                        }
                    }
                }

                // Editorial Headline
                Column {
                    Text(
                        text = "Everything is",
                        style = VaultType.PageTitle,
                        color = VaultColors.TextPrimary
                    )
                    Text(
                        text = "safely locked.",
                        style = VaultType.PageTitle.copy(fontStyle = FontStyle.Italic),
                        color = VaultColors.Accent
                    )
                }
            }
        }

        // ── Hero Vault Status Card ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, VaultColors.Hairline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    VaultColors.AccentTint.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Radial-lit Progress Indicator
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(88.dp)
                        ) {
                            // Inner ambient glow
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(VaultColors.AccentTint)
                            )
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxSize(),
                                color = VaultColors.SurfaceRaised,
                                strokeWidth = 5.dp,
                            )
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = VaultColors.Accent,
                                strokeWidth = 5.dp,
                                strokeCap = StrokeCap.Round
                            )
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = VaultColors.Accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "VAULT STORAGE",
                                    style = VaultType.Meta,
                                    color = VaultColors.TextSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(VaultColors.AccentTint)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AES-256",
                                        style = VaultType.Meta,
                                        color = VaultColors.Accent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = formatFileSize(totalSize),
                                style = VaultType.Status,
                                color = VaultColors.TextPrimary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Zero-knowledge encrypted",
                                style = VaultType.Meta,
                                color = VaultColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = VaultColors.Hairline, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Telemetry Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = VaultColors.TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${files.size} ${if (files.size == 1) "file" else "files"} secured",
                                style = VaultType.Meta,
                                color = VaultColors.TextSecondary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigateToTab("vault") }
                                .padding(vertical = 2.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Open vault",
                                style = VaultType.Meta,
                                color = VaultColors.Accent
                            )
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = VaultColors.Accent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Recent Files Section ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent activity",
                        style = VaultType.SectionTitle,
                        color = VaultColors.TextPrimary
                    )
                    if (files.isNotEmpty()) {
                        Text(
                            text = "See all (${files.size})",
                            style = VaultType.Meta,
                            color = VaultColors.Accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigateToTab("vault") }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Content: Empty State vs Grouped Card List
                if (files.isEmpty()) {
                    EmptyVaultCard(onActionClick = { onNavigateToTab("vault") })
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, VaultColors.Hairline)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            recentFiles.forEachIndexed { index, file ->
                                RecentFileRow(
                                    fileName = file.originalFileName,
                                    encryptedAt = file.encryptedAt,
                                    onClick = {
                                        vaultViewModel.setHighlightedFile(file.id)
                                        onNavigateToTab("vault")
                                    }
                                )
                                if (index < recentFiles.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                        color = VaultColors.Hairline,
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentFileRow(
    fileName: String,
    encryptedAt: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VaultColors.AccentTint)
                .border(1.dp, VaultColors.Hairline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForFileName(fileName),
                contentDescription = null,
                tint = VaultColors.Accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = VaultType.ItemTitle,
                color = VaultColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Encrypted · ${timeAgo(encryptedAt)}",
                style = VaultType.Meta,
                color = VaultColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = VaultColors.TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EmptyVaultCard(onActionClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onActionClick),
        colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, VaultColors.Hairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(VaultColors.AccentTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = VaultColors.Accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Your vault is empty",
                style = VaultType.ItemTitle,
                color = VaultColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap to encrypt and lock your first file",
                style = VaultType.Meta,
                color = VaultColors.TextSecondary
            )
        }
    }
}

// ── Helpers ──

private fun iconForFileName(fileName: String): ImageVector {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "webp", "gif", "heic" -> Icons.Outlined.Image
        "mp4", "mov", "mkv", "avi" -> Icons.Outlined.Videocam
        "mp3", "wav", "flac", "m4a" -> Icons.Outlined.AudioFile
        "pdf", "doc", "docx", "txt", "md" -> Icons.Outlined.Description
        "zip", "rar", "7z", "tar" -> Icons.Outlined.FolderZip
        else -> Icons.Outlined.FilePresent
    }
}

private fun getTimeBasedGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    val idx = digitGroups.coerceIn(0, units.lastIndex)
    return "%.1f %s".format(bytes / 1024.0.pow(idx.toDouble()), units[idx])
}

fun timeAgo(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = diff / (60 * 1000)
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours ${if (hours == 1L) "hr" else "hrs"} ago"
        else -> "$days ${if (days == 1L) "day" else "days"} ago"
    }
}
