package com.ciphervault.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.ciphervault.app.ui.components.rememberNavPillClearance
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    vaultViewModel: VaultViewModel,
    onNavigateToTab: (String) -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val files by vaultViewModel.files.collectAsState()
    val recentFiles = remember(files) { files.take(5) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 22.dp, end = 22.dp,
            top = 64.dp,
            bottom = rememberNavPillClearance(),
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Top Row ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Good evening",
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VaultColors.SurfaceRaised),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AK", style = VaultType.Chip, color = VaultColors.TextPrimary)
                }
            }
        }

        // ── Headline ──
        item {
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

        // ── Status Card ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultColors.Hairline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(92.dp),
                            color = VaultColors.SurfaceRaised,
                            strokeWidth = 6.dp,
                        )
                        CircularProgressIndicator(
                            progress = { 0.88f },
                            modifier = Modifier.size(92.dp),
                            color = VaultColors.Accent,
                            strokeWidth = 6.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = VaultColors.Accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Text(
                            text = "Vault status",
                            style = VaultType.Meta,
                            color = VaultColors.TextSecondary
                        )
                        Text(
                            text = "Protected",
                            style = VaultType.Status,
                            color = VaultColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AES-256 · Synced 2 min ago",
                            style = VaultType.Meta,
                            color = VaultColors.TextSecondary
                        )
                    }
                }
            }
        }

        // ── Quick Actions ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Add,
                    label = "Add",
                    onClick = { onNavigateToTab("vault") }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.DocumentScanner,
                    label = "Scan",
                    onClick = { }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Key,
                    label = "Encrypt",
                    onClick = { onNavigateToTab("encrypt") }
                )
            }
        }

        // ── Recent Files Header ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent",
                    style = VaultType.SectionTitle,
                    color = VaultColors.TextPrimary
                )
                Text(
                    text = "See all",
                    style = VaultType.Meta,
                    color = VaultColors.Accent,
                    modifier = Modifier.clickable { onNavigateToTab("vault") }
                )
            }
        }

        // ── Recent Files List ──
        if (files.isEmpty()) {
            item {
                Text(
                    text = "Vault is empty",
                    style = VaultType.ItemTitle,
                    color = VaultColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }
        } else {
            items(recentFiles, key = { it.id }) { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(VaultColors.AccentTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilePresent,
                            contentDescription = null,
                            tint = VaultColors.Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.originalFileName,
                            style = VaultType.ItemTitle,
                            color = VaultColors.TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "Encrypted · ${timeAgo(file.encryptedAt)}",
                            style = VaultType.Meta,
                            color = VaultColors.TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = VaultColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultColors.Hairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VaultColors.Accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = VaultType.Chip,
                color = VaultColors.TextPrimary
            )
        }
    }
}

fun timeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (60 * 1000)
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hours ago"
        else -> "$days days ago"
    }
}
