package com.ciphervault.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.data.db.EncryptedFileEntity
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.VaultViewModel
import com.ciphervault.app.ui.screens.formatFileSize

import androidx.compose.ui.res.stringResource
import com.ciphervault.app.R

private val ColorImage = Color(0xFF38BDF8)
private val ColorDocument = Color(0xFFFB7185)
private val ColorAudio = Color(0xFFC084FC)
private val ColorNote = Color(0xFFFBBF24)

@Composable
fun StorageInsightsScreen(
    vaultViewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val files by vaultViewModel.files.collectAsState()
    val totalSize by vaultViewModel.totalSize.collectAsState()

    val sImages = stringResource(R.string.storage_images)
    val sVideos = stringResource(R.string.storage_videos)
    val sDocuments = stringResource(R.string.storage_documents)
    val sAudio = stringResource(R.string.storage_audio)
    val sNotesKeys = stringResource(R.string.storage_notes_keys)
    val sOther = stringResource(R.string.storage_other)

    val segments = remember(files, sImages, sVideos, sDocuments, sAudio, sNotesKeys, sOther) {
        listOf(
            Triple(sImages, ColorImage, files.filter { matchesCategory(it, "Images") }.sumOf { it.originalSize }),
            Triple(sVideos, VaultColors.WarningAmber, files.filter { matchesCategory(it, "Videos") }.sumOf { it.originalSize }),
            Triple(sDocuments, ColorDocument, files.filter { matchesCategory(it, "Documents") }.sumOf { it.originalSize }),
            Triple(sAudio, ColorAudio, files.filter { matchesCategory(it, "Audio") }.sumOf { it.originalSize }),
            Triple(sNotesKeys, ColorNote, files.filter { matchesCategory(it, "Notes & Keys") }.sumOf { it.originalSize }),
            Triple(sOther, VaultColors.TextSecondary, files.filter { matchesCategory(it, "Other") }.sumOf { it.originalSize })
        ).filter { it.third > 0 }.sortedByDescending { it.third }
    }

    val largestFiles = remember(files) {
        files.sortedByDescending { it.originalSize }.take(20)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Ground),
        contentPadding = PaddingValues(
            start = 22.dp,
            end = 22.dp,
            top = 56.dp,
            bottom = 48.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = VaultColors.Surface,
                    border = BorderStroke(1.dp, VaultColors.Hairline),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.setup_back),
                            tint = VaultColors.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = stringResource(R.string.storage_title_1),
                        style = VaultType.PageTitle,
                        color = VaultColors.TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.storage_title_2),
                        style = VaultType.PageTitle.copy(fontStyle = FontStyle.Italic),
                        color = VaultColors.Accent
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, VaultColors.Hairline)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    Text(
                        text = stringResource(R.string.storage_total_payload),
                        style = VaultType.Meta,
                        color = VaultColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatFileSize(totalSize),
                        style = VaultType.PageTitle.copy(fontSize = 32.sp),
                        color = VaultColors.Accent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.storage_encrypted_items, files.size),
                        style = VaultType.Meta,
                        color = VaultColors.TextSecondary
                    )

                    if (segments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Distribution Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(CircleShape)
                                .background(VaultColors.Ground)
                        ) {
                            segments.forEach { (_, color, size) ->
                                val weight = size.toFloat() / totalSize.coerceAtLeast(1L).toFloat()
                                if (weight > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(weight)
                                            .fillMaxHeight()
                                            .background(color)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Legend
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            segments.forEach { (name, color, size) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Text(
                                            text = name,
                                            style = VaultType.ItemTitle.copy(fontSize = 15.sp),
                                            color = VaultColors.TextPrimary
                                        )
                                    }
                                    Text(
                                        text = formatFileSize(size),
                                        style = VaultType.Meta,
                                        color = VaultColors.TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (largestFiles.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.storage_largest_files),
                    style = VaultType.GroupLabel,
                    color = VaultColors.TextSecondary,
                    modifier = Modifier.padding(start = 14.dp, bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, VaultColors.Hairline)
                ) {
                    Column {
                        largestFiles.forEachIndexed { index, file ->
                            val (icon, color) = getIconAndColor(file)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(color.copy(alpha = 0.15f))
                                        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.originalFileName,
                                        style = VaultType.ItemTitle,
                                        color = VaultColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatFileSize(file.originalSize),
                                        style = VaultType.Meta,
                                        color = VaultColors.TextSecondary
                                    )
                                }
                            }

                            if (index < largestFiles.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 70.dp, end = 16.dp),
                                    color = VaultColors.Hairline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun matchesCategory(file: EncryptedFileEntity, category: String): Boolean {
    val mime = file.mimeType
    val noteOrCred = file.originalFileName.endsWith(".note.txt") || file.originalFileName.endsWith(".cred.txt")
    return when (category) {
        "Notes & Keys" -> noteOrCred
        "Images" -> mime?.startsWith("image/") == true
        "Documents" -> !noteOrCred && mime != null && (mime.contains("pdf") || mime.contains("document") || mime.contains("text"))
        "Videos" -> mime?.startsWith("video/") == true
        "Audio" -> mime?.startsWith("audio/") == true
        "Other" -> !noteOrCred && !(mime?.startsWith("image/") == true) && !(mime?.startsWith("video/") == true) && !(mime?.startsWith("audio/") == true) && !(mime != null && (mime.contains("pdf") || mime.contains("document") || mime.contains("text")))
        else -> false
    }
}

private fun getIconAndColor(file: EncryptedFileEntity): Pair<ImageVector, Color> {
    val mime = file.mimeType
    val name = file.originalFileName.lowercase()
    val ext = name.substringAfterLast('.', "")
    return when {
        name.endsWith(".cred.txt") -> Icons.Outlined.Key to VaultColors.Accent
        name.endsWith(".note.txt") -> Icons.Outlined.Description to ColorNote
        mime?.startsWith("image/") == true -> Icons.Outlined.Image to ColorImage
        mime?.startsWith("video/") == true -> Icons.Outlined.Videocam to VaultColors.WarningAmber
        mime?.startsWith("audio/") == true -> Icons.Outlined.AudioFile to ColorAudio
        mime == "application/pdf" || ext == "pdf" -> Icons.Outlined.Description to ColorDocument
        ext in setOf("zip", "rar", "7z", "tar", "gz") || mime?.contains("zip") == true -> Icons.Outlined.FolderZip to VaultColors.TextSecondary
        mime?.contains("pdf") == true || mime?.contains("document") == true || mime?.contains("text") == true || ext in setOf("doc", "docx", "txt", "md", "odt", "rtf") -> Icons.Outlined.Description to ColorDocument
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile to VaultColors.TextSecondary
    }
}