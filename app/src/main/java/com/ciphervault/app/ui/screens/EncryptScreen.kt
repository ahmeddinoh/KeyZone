package com.ciphervault.app.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.SelectedFile
import com.ciphervault.app.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

private val SectionGap = 18.dp

// =====================================================================
//  Screen
// =====================================================================

@Composable
fun EncryptScreen(vaultViewModel: VaultViewModel) {
    val isLoading by vaultViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val app = context.applicationContext as CipherVaultApp

    val selectedFiles = vaultViewModel.selectedFilesForEncryption
    var deleteOriginalFiles by remember { mutableStateOf(app.vaultConfig.deleteOriginalAfterEncryption) }

    var isBatchEncrypting by remember { mutableStateOf(false) }
    var batchIndex by remember { mutableIntStateOf(0) }
    var batchTotal by remember { mutableIntStateOf(0) }
    var currentProcessingFile by remember { mutableStateOf("") }
    
    val currentFileProgress by vaultViewModel.encryptionProgress.collectAsState()
    val batchProgress = if (batchTotal > 0) {
        ((batchIndex + currentFileProgress) / batchTotal).coerceIn(0f, 1f)
    } else {
        0f
    }

    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showCreateCredentialDialog by remember { mutableStateOf(false) }

    val pendingUris by app.pendingSharedUris.collectAsState()

    fun processSelectedUri(uri: Uri) {
        if (selectedFiles.any { it.uri == uri }) return
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
        selectedFiles.add(SelectedFile(uri, fileName, fileSize, mimeType))
    }

    LaunchedEffect(pendingUris) {
        if (pendingUris.isNotEmpty()) {
            pendingUris.forEach { processSelectedUri(it) }
            app.pendingSharedUris.value = emptyList()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { processSelectedUri(it) }
    }

    fun openPicker() {
        if (!isBatchEncrypting) {
            app.ignoreNextBackgroundLock = true
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    fun startEncryption() {
        val filesToEncrypt = selectedFiles.toList()
        if (filesToEncrypt.isEmpty()) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        selectedFiles.clear()
        batchTotal = filesToEncrypt.size
        batchIndex = 0
        isBatchEncrypting = true
        scope.launch {
            filesToEncrypt.forEachIndexed { index, file ->
                currentProcessingFile = file.name
                batchIndex = index
                // Await completion of each encryption
                vaultViewModel.encryptFileSuspend(
                    uri = file.uri,
                    fileName = file.name,
                    mimeType = file.mimeType,
                    deleteOriginal = deleteOriginalFiles
                )
            }
            isBatchEncrypting = false
            currentProcessingFile = ""
        }
    }

    val totalSelectedSize = selectedFiles.sumOf { it.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Ground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 150.dp)
        ) {
            // ---- Header ----
            item(key = "header") {
                ScreenHeader(modifier = Modifier.padding(bottom = SectionGap + 4.dp))
            }

            if (selectedFiles.isEmpty()) {
                // ---- Empty state: drop zone ----
                item(key = "dropzone") {
                    DropZone(
                        enabled = !isBatchEncrypting,
                        onClick = { openPicker() },
                        modifier = Modifier.padding(bottom = SectionGap + 6.dp)
                    )
                }

                // ---- Empty state: create directly ----
                item(key = "create-label") {
                    Text(
                        text = "Or create something directly",
                        style = VaultType.Meta,
                        color = VaultColors.TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    )
                }
                item(key = "create-note") {
                    GroupedRow(
                        index = 0,
                        count = 2,
                        icon = Icons.AutoMirrored.Outlined.NoteAdd,
                        title = "Secure note",
                        subtitle = "Write something private",
                        onClick = { showCreateNoteDialog = true }
                    )
                }
                item(key = "create-credential") {
                    GroupedRow(
                        index = 1,
                        count = 2,
                        icon = Icons.Outlined.Key,
                        title = "Credential",
                        subtitle = "Save a username and password",
                        onClick = { showCreateCredentialDialog = true }
                    )
                }
            } else {
                // ---- Option: delete originals ----
                item(key = "delete-option") {
                    DeleteOriginalCard(
                        checked = deleteOriginalFiles,
                        enabled = !isBatchEncrypting,
                        onCheckedChange = { deleteOriginalFiles = it },
                        modifier = Modifier.padding(bottom = SectionGap + 4.dp)
                    )
                }

                // ---- List header ----
                item(key = "files-header") {
                    FilesHeader(
                        count = selectedFiles.size,
                        totalSize = formatFileSize(totalSelectedSize),
                        enabled = !isBatchEncrypting,
                        onAddMore = { openPicker() },
                        onClear = { selectedFiles.clear() },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                // ---- Files ----
                itemsIndexed(
                    items = selectedFiles,
                    key = { _, file -> file.uri.toString() }
                ) { index, file ->
                    val kind = fileKindOf(file)
                    GroupedRow(
                        index = index,
                        count = selectedFiles.size,
                        icon = kind.icon,
                        title = file.name,
                        subtitle = kind.label,
                        trailing = {
                            Text(
                                text = formatFileSize(file.size),
                                style = VaultType.Meta,
                                color = VaultColors.TextSecondary
                            )
                            IconButton(
                                onClick = { selectedFiles.remove(file) },
                                enabled = !isBatchEncrypting,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Remove ${file.name}",
                                    tint = VaultColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        }

        // ---- Bottom action bar / progress ----
        AnimatedVisibility(
            visible = selectedFiles.isNotEmpty() || isBatchEncrypting,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(260)) { it } + fadeIn(tween(260)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, VaultColors.Ground.copy(alpha = 0.85f), VaultColors.Ground)
                        )
                    )
            ) {
                if (isBatchEncrypting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp, top = 20.dp)
                    ) {
                        ProgressPanel(
                            index = batchIndex,
                            total = batchTotal,
                            progress = batchProgress,
                            fileName = currentProcessingFile
                        )
                    }
                } else {
                    FloatingEncryptActionRow(
                        fileCount = selectedFiles.size,
                        busy = isLoading,
                        onCancel = { selectedFiles.clear() },
                        onEncrypt = { startEncryption() }
                    )
                }
            }
        }
    }

    if (showCreateNoteDialog) {
        NoteDialog(
            onDismiss = { showCreateNoteDialog = false },
            onSave = { title, content ->
                vaultViewModel.createSecureNote(context, title, content)
                showCreateNoteDialog = false
            }
        )
    }

    if (showCreateCredentialDialog) {
        CredentialDialog(
            onDismiss = { showCreateCredentialDialog = false },
            onSave = { title, user, pass ->
                vaultViewModel.createCredential(context, title, user, pass)
                showCreateCredentialDialog = false
            }
        )
    }
}

// =====================================================================
//  Sections
// =====================================================================

@Composable
private fun ScreenHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Lock your files",
            style = VaultType.PageTitle,
            color = VaultColors.TextPrimary
        )
        Text(
            text = "in the vault.",
            style = VaultType.PageTitle.copy(fontStyle = FontStyle.Italic),
            color = VaultColors.Accent
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Choose what to protect. You can have the originals removed once they're encrypted.",
            style = VaultType.Meta,
            color = VaultColors.TextSecondary
        )
    }
}

/** The one expressive element on the screen: a large dashed target that invites a tap. */
@Composable
private fun DropZone(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(VaultColors.AccentTint.copy(alpha = 0.55f))
            .dashedBorder(VaultColors.Accent.copy(alpha = 0.5f), cornerRadius = 28.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = "Choose files to encrypt",
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(
                icon = Icons.Outlined.FileUpload,
                size = 76.dp,
                iconSize = 34.dp,
                background = VaultColors.Ground.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Select files to encrypt",
                style = VaultType.ItemTitle,
                color = VaultColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Photos, documents, videos and archives all work.",
                style = VaultType.Meta,
                color = VaultColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(VaultColors.Accent)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Browse files",
                    style = VaultType.ItemTitle.copy(fontSize = 14.sp),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DeleteOriginalCard(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (checked) VaultColors.WarningAmber.copy(alpha = 0.6f) else VaultColors.Hairline,
        label = "deleteBorder"
    )
    val containerColor by animateColorAsState(
        targetValue = if (checked) VaultColors.WarningAmber.copy(alpha = 0.08f) else VaultColors.Surface,
        label = "deleteContainer"
    )
    val iconTint = if (checked) VaultColors.WarningAmber else VaultColors.TextSecondary

    VaultCard(
        modifier = modifier,
        borderColor = borderColor,
        containerColor = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = Icons.Outlined.DeleteSweep,
                size = 42.dp,
                shape = RoundedCornerShape(12.dp),
                tint = iconTint,
                background = VaultColors.SurfaceRaised
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Delete originals after encrypting",
                    style = VaultType.Meta,
                    color = VaultColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (checked) {
                        "Originals are permanently deleted once they're encrypted."
                    } else {
                        "Your original files stay where they are."
                    },
                    style = VaultType.Meta,
                    color = if (checked) VaultColors.WarningAmber else VaultColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = VaultColors.WarningAmber,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = VaultColors.TextSecondary,
                    uncheckedTrackColor = VaultColors.SurfaceRaised,
                    uncheckedBorderColor = VaultColors.Hairline
                )
            )
        }
    }
}

@Composable
private fun FilesHeader(
    count: Int,
    totalSize: String,
    enabled: Boolean,
    onAddMore: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "$count ${if (count == 1) "file" else "files"} selected",
                style = VaultType.ItemTitle,
                color = VaultColors.TextPrimary
            )
            Text(
                text = "$totalSize in total",
                style = VaultType.Meta,
                color = VaultColors.TextSecondary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onAddMore,
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 10.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = VaultColors.Accent)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add more", style = VaultType.Meta, fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = onClear,
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 10.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = VaultColors.TextSecondary)
            ) {
                Text("Clear", style = VaultType.Meta)
            }
        }
    }
}

@Composable
private fun FloatingEncryptActionRow(
    fileCount: Int,
    busy: Boolean,
    onCancel: () -> Unit,
    onEncrypt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp, top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel button on the left
        Button(
            onClick = onCancel,
            enabled = !busy,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VaultColors.SurfaceRaised,
                contentColor = VaultColors.TextPrimary,
                disabledContainerColor = VaultColors.SurfaceRaised.copy(alpha = 0.5f),
                disabledContentColor = VaultColors.TextSecondary.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, VaultColors.GlassBorder),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
            modifier = Modifier.height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Cancel selection",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Cancel",
                style = VaultType.ItemTitle.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold
            )
        }

        // Floating Encrypt button on the right
        Button(
            onClick = onEncrypt,
            enabled = !busy && fileCount > 0,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VaultColors.Accent,
                contentColor = Color.Black,
                disabledContainerColor = VaultColors.Accent.copy(alpha = 0.4f),
                disabledContentColor = Color.Black.copy(alpha = 0.6f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 3.dp),
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Working...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Icon(
                    imageVector = Icons.Outlined.EnhancedEncryption,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Encrypt $fileCount ${if (fileCount == 1) "file" else "files"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Shown in the bottom bar while a batch is running. The selection list is cleared when
 * encryption starts, so progress has to live outside of it to stay visible.
 */
@Composable
private fun ProgressPanel(
    index: Int,
    total: Int,
    progress: Float,
    fileName: String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400),
        label = "batchProgress"
    )
    val indeterminate = progress <= 0f

    VaultCard(
        borderColor = VaultColors.Accent.copy(alpha = 0.5f),
        containerColor = VaultColors.SurfaceRaised
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Encrypting file ${(index + 1).coerceAtMost(total)} of $total",
                    style = VaultType.Meta,
                    color = VaultColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (!indeterminate) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = VaultType.Meta,
                        color = VaultColors.Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (indeterminate) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = VaultColors.Accent,
                    trackColor = VaultColors.Ground
                )
            } else {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = VaultColors.Accent,
                    trackColor = VaultColors.Ground
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = fileName,
                style = VaultType.Meta,
                color = VaultColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =====================================================================
//  Dialogs
// =====================================================================

@Composable
private fun NoteDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    VaultDialog(
        icon = Icons.AutoMirrored.Outlined.NoteAdd,
        title = "New secure note",
        subtitle = "Give it a title, then write whatever you want to keep private.",
        confirmLabel = "Save note",
        confirmEnabled = title.isNotBlank() && content.isNotBlank(),
        onConfirm = { onSave(title, content) },
        onDismiss = onDismiss
    ) {
        VaultTextField(
            value = title,
            onValueChange = { title = it },
            label = "Title",
            leadingIcon = Icons.Outlined.Title,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            )
        )
        VaultTextField(
            value = content,
            onValueChange = { content = it },
            label = "Note",
            minLines = 5,
            maxLines = 8,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
    }
}

@Composable
private fun CredentialDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, username: String, password: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    VaultDialog(
        icon = Icons.Outlined.Key,
        title = "New credential",
        subtitle = "Save a login so it's always in your vault.",
        confirmLabel = "Save credential",
        confirmEnabled = title.isNotBlank() && password.isNotBlank(),
        onConfirm = { onSave(title, username, password) },
        onDismiss = onDismiss
    ) {
        VaultTextField(
            value = title,
            onValueChange = { title = it },
            label = "Title (for example, Netflix)",
            leadingIcon = Icons.Outlined.Title,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            )
        )
        VaultTextField(
            value = username,
            onValueChange = { username = it },
            label = "Username or email",
            leadingIcon = Icons.Outlined.Person,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        VaultTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            leadingIcon = Icons.Outlined.Lock,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailing = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            }
        )
    }
}

@Composable
private fun VaultDialog(
    icon: ImageVector,
    title: String,
    subtitle: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = VaultColors.SurfaceRaised,
        icon = { IconBadge(icon = icon, size = 52.dp, iconSize = 26.dp) },
        title = {
            Text(
                text = title,
                style = VaultType.ItemTitle,
                color = VaultColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = subtitle,
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
                content()
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultColors.Accent,
                    contentColor = Color.Black,
                    disabledContainerColor = VaultColors.Accent.copy(alpha = 0.3f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    val leading: (@Composable () -> Unit)? = if (leadingIcon != null) {
        { Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp)) }
    } else {
        null
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = leading,
        trailingIcon = trailing,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VaultColors.Accent,
            unfocusedBorderColor = VaultColors.Hairline,
            focusedTextColor = VaultColors.TextPrimary,
            unfocusedTextColor = VaultColors.TextPrimary,
            focusedLabelColor = VaultColors.Accent,
            unfocusedLabelColor = VaultColors.TextSecondary,
            cursorColor = VaultColors.Accent,
            focusedLeadingIconColor = VaultColors.Accent,
            unfocusedLeadingIconColor = VaultColors.TextSecondary,
            focusedTrailingIconColor = VaultColors.TextSecondary,
            unfocusedTrailingIconColor = VaultColors.TextSecondary,
            focusedContainerColor = VaultColors.Ground.copy(alpha = 0.4f),
            unfocusedContainerColor = VaultColors.Ground.copy(alpha = 0.4f)
        )
    )
}

// =====================================================================
//  Building blocks
// =====================================================================

/** A row that is part of a visually grouped list: only the first/last rows round their outer corners. */
@Composable
private fun GroupedRow(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    val shape = groupedShape(index, count)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VaultColors.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = icon,
                size = 42.dp,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = VaultType.ItemTitle.copy(fontSize = 15.sp),
                    color = VaultColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
        if (index < count - 1) {
            Box(
                modifier = Modifier
                    .padding(start = 70.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VaultColors.Hairline)
            )
        }
    }
}

private fun groupedShape(index: Int, count: Int, radius: Dp = 18.dp): Shape = when {
    count <= 1 -> RoundedCornerShape(radius)
    index == 0 -> RoundedCornerShape(topStart = radius, topEnd = radius)
    index == count - 1 -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    else -> RectangleShape
}

@Composable
private fun VaultCard(
    modifier: Modifier = Modifier,
    borderColor: Color = VaultColors.Hairline,
    containerColor: Color = VaultColors.Surface,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    shape: Shape = CircleShape,
    tint: Color = VaultColors.Accent,
    background: Color = VaultColors.AccentTint
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background)
            .border(1.dp, tint.copy(alpha = 0.25f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.5.dp,
    dash: Dp = 10.dp,
    gap: Dp = 8.dp
): Modifier = drawBehind {
    val stroke = strokeWidth.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))
        )
    )
}

// =====================================================================
//  File type helpers
// =====================================================================

private data class FileKind(val icon: ImageVector, val label: String)

private fun fileKindOf(file: SelectedFile): FileKind {
    val mime = file.mimeType.orEmpty()
    val ext = file.name.substringAfterLast('.', "").lowercase()
    return when {
        mime.startsWith("image/") -> FileKind(Icons.Outlined.Image, "Image")
        mime.startsWith("video/") -> FileKind(Icons.Outlined.Videocam, "Video")
        mime.startsWith("audio/") -> FileKind(Icons.Outlined.AudioFile, "Audio")
        mime == "application/pdf" || ext == "pdf" -> FileKind(Icons.Outlined.PictureAsPdf, "PDF document")
        ext in setOf("zip", "rar", "7z", "tar", "gz") || mime.contains("zip") ->
            FileKind(Icons.Outlined.FolderZip, "Archive")
        mime.startsWith("text/") || ext in setOf("doc", "docx", "txt", "md", "odt", "rtf") ->
            FileKind(Icons.Outlined.Description, "Document")
        ext.isNotEmpty() -> FileKind(Icons.Outlined.Description, "${ext.uppercase()} file")
        else -> FileKind(Icons.Outlined.Description, "File")
    }
}