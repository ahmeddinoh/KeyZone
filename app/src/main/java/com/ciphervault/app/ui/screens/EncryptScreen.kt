package com.ciphervault.app.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.ui.theme.CardBorder
import com.ciphervault.app.ui.theme.CardBorderSubtle
import com.ciphervault.app.ui.theme.CyanAccent
import com.ciphervault.app.ui.theme.CyanSubtle
import com.ciphervault.app.ui.theme.DarkBackground
import com.ciphervault.app.ui.theme.DarkCard
import com.ciphervault.app.ui.theme.DarkSurfaceVariant
import com.ciphervault.app.ui.theme.TextPrimary
import com.ciphervault.app.ui.theme.TextSecondary
import com.ciphervault.app.ui.theme.TextTertiary
import com.ciphervault.app.ui.theme.VaultGreen
import com.ciphervault.app.ui.theme.VaultGreenSubtle
import com.ciphervault.app.ui.theme.WarningAmber
import com.ciphervault.app.ui.theme.WarningSubtle
import com.ciphervault.app.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String?
)

/**
 * Modern Encrypt Screen with multi-file selection, pre-encryption compression estimates,
 * determinate progress tracking, and option to delete source files after encryption.
 */
@Composable
fun EncryptScreen(vaultViewModel: VaultViewModel) {
    val isLoading by vaultViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as CipherVaultApp

    val selectedFiles = remember { mutableStateListOf<SelectedFile>() }
    var deleteOriginalFiles by remember { mutableStateOf(app.vaultConfig.deleteOriginalAfterEncryption) }

    // Determinate batch progress states
    var isBatchEncrypting by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableFloatStateOf(0f) }
    var currentProcessingFile by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            var fileName = "Unknown file"
            var fileSize = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) fileSize = cursor.getLong(sizeIndex)
                }
            }
            val mimeType = context.contentResolver.getType(uri)

            if (selectedFiles.none { it.uri == uri }) {
                selectedFiles.add(SelectedFile(uri, fileName, fileSize, mimeType))
            }
        }
    }

    val totalSelectedSize = selectedFiles.sumOf { it.size }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Bar ──
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Encrypt Files",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AES-256-GCM authenticated encryption",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(VaultGreenSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = VaultGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── File Selection Box ──
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (selectedFiles.isEmpty()) CardBorder else VaultGreen.copy(alpha = 0.5f),
                        RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(20.dp),
                onClick = { if (!isBatchEncrypting) filePickerLauncher.launch(arrayOf("*/*")) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(VaultGreenSubtle)
                            .border(1.dp, VaultGreen.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = null,
                            tint = VaultGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Select Files to Encrypt",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Supports photos, documents, videos, and archives",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        enabled = !isBatchEncrypting,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VaultGreen.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = VaultGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse Files", color = VaultGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Pre-Encryption Compression & Size Estimate Card ──
        if (selectedFiles.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyanAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pre-Encryption Compression",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyanSubtle)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "V2 AUTO-GZIP",
                                    color = CyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Payload", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                Text(
                                    formatFileSize(totalSelectedSize),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Selected Items", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                Text(
                                    "${selectedFiles.size} file${if (selectedFiles.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VaultGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ── Option: Delete Original Source Files ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (deleteOriginalFiles) WarningSubtle else DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = null,
                                    tint = if (deleteOriginalFiles) WarningAmber else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Delete original after encrypting",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Permanently removes unencrypted source via SAF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = deleteOriginalFiles,
                            onCheckedChange = { deleteOriginalFiles = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = VaultGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }
                }
            }

            // ── Selected Files List Header ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Files (${selectedFiles.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Clear All",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.clickable { selectedFiles.clear() }
                    )
                }
            }

            // ── Selected Files List ──
            items(selectedFiles) { file ->
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
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = formatFileSize(file.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = { selectedFiles.remove(file) },
                            enabled = !isBatchEncrypting,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove",
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── Determinate Progress Bar during Batch Encryption ──
            if (isBatchEncrypting) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VaultGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Encrypting: $currentProcessingFile",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${(batchProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { batchProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = VaultGreen,
                                trackColor = DarkBackground
                            )
                        }
                    }
                }
            }

            // ── Encrypt Action Button ──
            item {
                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val filesToEncrypt = selectedFiles.toList()
                        selectedFiles.clear()
                        isBatchEncrypting = true
                        scope.launch {
                            val total = filesToEncrypt.size
                            filesToEncrypt.forEachIndexed { index, file ->
                                currentProcessingFile = file.name
                                batchProgress = index.toFloat() / total
                                vaultViewModel.encryptFile(
                                    uri = file.uri,
                                    fileName = file.name,
                                    mimeType = file.mimeType,
                                    deleteOriginal = deleteOriginalFiles
                                )
                            }
                            batchProgress = 1f
                            isBatchEncrypting = false
                            currentProcessingFile = ""
                        }
                    },
                    enabled = !isBatchEncrypting && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isBatchEncrypting || isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Encrypting Files...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Filled.EnhancedEncryption, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Encrypt ${selectedFiles.size} File${if (selectedFiles.size > 1) "s" else ""}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
