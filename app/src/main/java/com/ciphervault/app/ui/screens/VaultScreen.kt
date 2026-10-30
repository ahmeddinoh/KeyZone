package com.ciphervault.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.data.db.EncryptedFileEntity
import com.ciphervault.app.ui.components.rememberNavPillClearance
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.VaultViewModel

private val CATEGORIES = listOf("All", "Documents", "Passwords", "Media")

@Composable
fun VaultScreen(
    vaultViewModel: VaultViewModel,
    onNavigateToEncrypt: (() -> Unit)? = null,
    onNavigateToImagePreview: ((Long) -> Unit)? = null,
    onNavigateToMediaPreview: ((Long) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

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
        // ── Header ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Vault",
                    style = VaultType.PageTitle,
                    color = VaultColors.TextPrimary
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VaultColors.AccentTint)
                        .clickable { onNavigateToEncrypt?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add",
                        tint = VaultColors.Accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // ── Search ──
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                placeholder = { Text("Search your vault", color = VaultColors.TextSecondary, style = VaultType.Meta) },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search", tint = VaultColors.TextSecondary)
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VaultColors.Hairline,
                    unfocusedBorderColor = VaultColors.Hairline,
                    focusedContainerColor = VaultColors.Surface,
                    unfocusedContainerColor = VaultColors.Surface,
                    cursorColor = VaultColors.Accent,
                    focusedTextColor = VaultColors.TextPrimary,
                    unfocusedTextColor = VaultColors.TextPrimary
                )
            )
        }

        // ── Filter Chips ──
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CATEGORIES) { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) VaultColors.Accent else Color.Transparent)
                            .border(1.dp, if (isSelected) Color.Transparent else VaultColors.Hairline, CircleShape)
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            style = VaultType.Chip,
                            color = if (isSelected) VaultColors.OnAccent else VaultColors.TextSecondary
                        )
                    }
                }
            }
        }

        // ── Folder Grid ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FolderCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Description,
                    label = "Documents",
                    count = "42"
                )
                FolderCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Key,
                    label = "Passwords",
                    count = "31"
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FolderCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Image,
                    label = "Photos",
                    count = "24"
                )
                FolderCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Notes,
                    label = "Notes",
                    count = "18"
                )
            }
        }

        // ── Recently Added Header ──
        item {
            Text(
                text = "Recently added",
                style = VaultType.SectionTitle,
                color = VaultColors.TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // ── Recently Added List ──
        if (files.isEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(VaultColors.AccentTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.FilePresent, contentDescription = null, tint = VaultColors.Accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Insurance policy", style = VaultType.ItemTitle, color = VaultColors.TextPrimary)
                        Text(text = "PDF · 1.2 MB", style = VaultType.Meta, color = VaultColors.TextSecondary)
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(VaultColors.AccentTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.FilePresent, contentDescription = null, tint = VaultColors.Accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Wallet backup", style = VaultType.ItemTitle, color = VaultColors.TextPrimary)
                        Text(text = "Note · 4 KB", style = VaultType.Meta, color = VaultColors.TextSecondary)
                    }
                }
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
                        val sizeStr = formatFileSize(file.originalSize)
                        Text(
                            text = "${file.mimeType ?: "Unknown"} · $sizeStr",
                            style = VaultType.Meta,
                            color = VaultColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: String
) {
    Card(
        modifier = modifier.height(126.dp),
        colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultColors.Hairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(VaultColors.AccentTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VaultColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column {
                Text(
                    text = label,
                    style = VaultType.ItemTitle,
                    color = VaultColors.TextPrimary
                )
                Text(
                    text = count,
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary
                )
            }
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
