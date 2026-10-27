package com.ciphervault.app.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.data.db.EncryptedFileEntity
import com.ciphervault.app.ui.theme.BlueAccent
import com.ciphervault.app.ui.theme.CardBorder
import com.ciphervault.app.ui.theme.CardBorderSubtle
import com.ciphervault.app.ui.theme.CyanAccent
import com.ciphervault.app.ui.theme.CyanSubtle
import com.ciphervault.app.ui.theme.DarkBackground
import com.ciphervault.app.ui.theme.DarkCard
import com.ciphervault.app.ui.theme.DarkCardElevated
import com.ciphervault.app.ui.theme.DarkSurfaceVariant
import com.ciphervault.app.ui.theme.ErrorRed
import com.ciphervault.app.ui.theme.ErrorSubtle
import com.ciphervault.app.ui.theme.TextPrimary
import com.ciphervault.app.ui.theme.TextSecondary
import com.ciphervault.app.ui.theme.TextTertiary
import com.ciphervault.app.ui.theme.VaultGreen
import com.ciphervault.app.ui.theme.VaultGreenSubtle
import com.ciphervault.app.ui.theme.WarningAmber
import com.ciphervault.app.ui.theme.WarningSubtle
import com.ciphervault.app.viewmodel.SortOption
import com.ciphervault.app.viewmodel.VaultViewModel

private val CATEGORIES = listOf("All", "Images", "Documents", "Videos", "Audio", "Other")

/**
 * Vault screen with Grid/List layout toggle, sorting, instant search,
 * file details modal sheet, direct SAF export, renaming, and secure deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    vaultViewModel: VaultViewModel,
    onNavigateToEncrypt: (() -> Unit)? = null
) {
    val files by vaultViewModel.files.collectAsState()
    val isLoading by vaultViewModel.isLoading.collectAsState()
    val decryptedUri by vaultViewModel.decryptedUri.collectAsState()
    val viewingNoteContent by vaultViewModel.viewingNoteContent.collectAsState()
    val currentSort by vaultViewModel.sortOption.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isGridView by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }

    var selectedFileForDetails by remember { mutableStateOf<EncryptedFileEntity?>(null) }
    var fileToDelete by remember { mutableStateOf<EncryptedFileEntity?>(null) }
    var fileToRename by remember { mutableStateOf<EncryptedFileEntity?>(null) }
    var fileToEditTags by remember { mutableStateOf<EncryptedFileEntity?>(null) }
    var newFileName by remember { mutableStateOf("") }
    var newTags by remember { mutableStateOf("") }

    val context = LocalContext.current
    val app = context.applicationContext as com.ciphervault.app.CipherVaultApp
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Launcher for exporting decrypted plaintext file directly to user-chosen storage destination
    var fileToExport by remember { mutableStateOf<EncryptedFileEntity?>(null) }
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destinationUri ->
        destinationUri?.let { uri ->
            fileToExport?.let { entity ->
                vaultViewModel.exportDecryptedFile(entity, uri)
                fileToExport = null
            }
        }
    }

    // Launch preview / share when decrypted URI becomes available
    decryptedUri?.let { uri ->
        app.ignoreNextBackgroundLock = true
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
        vaultViewModel.clearDecryptedUri()
    }

    // Filter files by search query and category
    val filteredFiles = files.filter { file ->
        val matchesQuery = searchQuery.isBlank() || file.originalFileName.contains(searchQuery, ignoreCase = true)
        val matchesCategory = when (selectedCategory) {
            "All" -> true
            "Images" -> file.mimeType?.startsWith("image/") == true
            "Documents" -> file.mimeType?.contains("pdf") == true || file.mimeType?.contains("document") == true || file.mimeType?.contains("text") == true
            "Videos" -> file.mimeType?.startsWith("video/") == true
            "Audio" -> file.mimeType?.startsWith("audio/") == true
            "Other" -> file.mimeType == null || (!file.mimeType.startsWith("image/") && !file.mimeType.startsWith("video/") && !file.mimeType.startsWith("audio/"))
            else -> true
        }
        matchesQuery && matchesCategory
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar & Search ──
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Encrypted Vault",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${files.size} file${if (files.size != 1) "s" else ""} locked with AES-256-GCM",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    // View Mode & Sort Action Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Create Note Button
                        IconButton(
                            onClick = { showCreateNoteDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                                contentDescription = "New Secure Note",
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Sort Button & Dropdown
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sort,
                                    contentDescription = "Sort files",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(DarkCardElevated)
                            ) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                color = if (currentSort == option) VaultGreen else TextPrimary,
                                                fontWeight = if (currentSort == option) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            vaultViewModel.setSortOption(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Grid / List Toggle Button
                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = "Toggle Grid/List View",
                                tint = VaultGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search encrypted files...", color = TextTertiary) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = "Search", tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        cursorColor = VaultGreen,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CATEGORIES) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) VaultGreen else DarkCard)
                                .border(1.dp, if (isSelected) VaultGreen else CardBorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Loading spinner
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VaultGreen, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
            }

            // File Content Display (Grid or List)
            if (filteredFiles.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != "All") "No matching files"
                            else "No encrypted files yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try a different search term"
                            else "Tap + or go to Encrypt tab to add secure files",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else if (isGridView) {
                // ── Grid View Layout ──
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = filteredFiles, key = { it.id }) { file ->
                        VaultFileGridCard(
                            file = file,
                            onClick = { selectedFileForDetails = file },
                            onMore = { selectedFileForDetails = file }
                        )
                    }
                }
            } else {
                // ── List View Layout ──
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = filteredFiles, key = { it.id }) { file ->
                        VaultFileListCard(
                            file = file,
                            onClick = { selectedFileForDetails = file },
                            onDecrypt = {
                                if (file.originalFileName.endsWith(".note.txt")) {
                                    vaultViewModel.viewSecureNote(file, context)
                                } else {
                                    vaultViewModel.decryptFile(file)
                                }
                            },
                            onMore = { selectedFileForDetails = file }
                        )
                    }
                }
            }
        }

        // Floating Action Button for Quick Encrypt
        if (onNavigateToEncrypt != null) {
            FloatingActionButton(
                onClick = onNavigateToEncrypt,
                containerColor = VaultGreen,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Files", modifier = Modifier.size(28.dp))
            }
        }
    }

    // ── File Details & Actions Bottom Sheet ──
    selectedFileForDetails?.let { file ->
        ModalBottomSheet(
            onDismissRequest = { selectedFileForDetails = null },
            sheetState = sheetState,
            containerColor = DarkCardElevated,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(CardBorder)
                )
            }
        ) {
            val (icon, iconColor, iconBg) = getFileVisuals(file.mimeType)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.originalFileName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatFileSize(file.originalSize)} • ${file.mimeType ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Metadata Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Security Cipher", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                            Text("AES-256-GCM (Hardware key)", style = MaterialTheme.typography.bodySmall, color = VaultGreen, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Encrypted On", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                            Text(formatDate(file.encryptedAt), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action 1: Open Decrypted Preview
                OutlinedButton(
                    onClick = {
                        if (file.originalFileName.endsWith(".note.txt")) {
                            vaultViewModel.viewSecureNote(file, context)
                        } else {
                            vaultViewModel.decryptFile(file)
                        }
                        selectedFileForDetails = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Filled.LockOpen, contentDescription = null, tint = VaultGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Decrypted Preview", color = VaultGreen, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2: Export Decrypted File to Phone Storage
                OutlinedButton(
                    onClick = {
                        fileToExport = file
                        selectedFileForDetails = null
                        app.ignoreNextBackgroundLock = true
                        exportFileLauncher.launch(file.originalFileName)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Phone Storage", color = CyanAccent, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 3: Tags
                OutlinedButton(
                    onClick = {
                        newTags = file.tags
                        fileToEditTags = file
                        selectedFileForDetails = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Tags", color = TextPrimary, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 4: Rename
                OutlinedButton(
                    onClick = {
                        newFileName = file.originalFileName
                        fileToRename = file
                        selectedFileForDetails = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rename File", color = TextPrimary, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 5: Delete
                OutlinedButton(
                    onClick = {
                        fileToDelete = file
                        selectedFileForDetails = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Filled.DeleteOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete from Vault", color = ErrorRed, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Rename Dialog
    fileToRename?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            containerColor = DarkCardElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Rename Encrypted File") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    singleLine = true,
                    label = { Text("New file name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vaultViewModel.renameFile(file, newFileName)
                        fileToRename = null
                    },
                    enabled = newFileName.isNotBlank()
                ) {
                    Text("Rename", color = VaultGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Edit Tags Dialog
    fileToEditTags?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToEditTags = null },
            containerColor = DarkCardElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Edit File Tags") },
            text = {
                Column {
                    Text("Enter comma-separated tags", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTags,
                        onValueChange = { newTags = it },
                        singleLine = true,
                        label = { Text("Tags") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultGreen,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vaultViewModel.updateTags(file, newTags)
                        fileToEditTags = null
                    }
                ) {
                    Text("Save", color = VaultGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToEditTags = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            containerColor = DarkCardElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.DeleteOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Encrypted File")
                }
            },
            text = {
                Text("Permanently delete \"${file.originalFileName}\" from the vault? This ciphertext cannot be recovered.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vaultViewModel.deleteFile(file)
                    fileToDelete = null
                }) {
                    Text("Delete Permanently", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Create Note Dialog
    if (showCreateNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateNoteDialog = false },
            containerColor = DarkCardElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("New Secure Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        singleLine = true,
                        label = { Text("Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultGreen,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Note content") },
                        minLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultGreen,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vaultViewModel.createSecureNote(context, noteTitle, noteContent)
                        showCreateNoteDialog = false
                    },
                    enabled = noteTitle.isNotBlank() && noteContent.isNotBlank()
                ) {
                    Text("Save to Vault", color = VaultGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNoteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // View Note Dialog
    viewingNoteContent?.let { (title, content) ->
        AlertDialog(
            onDismissRequest = { vaultViewModel.clearViewingNote() },
            containerColor = DarkCardElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text(title) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text(content, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vaultViewModel.clearViewingNote() }) {
                    Text("Close", color = VaultGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Sleek List Card for a Vault File.
 */
@Composable
private fun VaultFileListCard(
    file: EncryptedFileEntity,
    onClick: () -> Unit,
    onDecrypt: () -> Unit,
    onMore: () -> Unit
) {
    val (icon, iconColor, iconBg) = getFileVisuals(file.mimeType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.originalFileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatFileSize(file.originalSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultGreen,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ${formatDate(file.encryptedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (file.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\uD83C\uDFF7\uFE0F ${file.tags}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDecrypt,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VaultGreenSubtle)
            ) {
                Icon(imageVector = Icons.Filled.LockOpen, contentDescription = "View", tint = VaultGreen, modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onMore) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More", tint = TextSecondary)
            }
        }
    }
}

/**
 * Sleek 2-Column Grid Card for a Vault File.
 */
@Composable
private fun VaultFileGridCard(
    file: EncryptedFileEntity,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val (icon, iconColor, iconBg) = getFileVisuals(file.mimeType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }

                IconButton(
                    onClick = onMore,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Options", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = file.originalFileName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatFileSize(file.originalSize),
                style = MaterialTheme.typography.bodySmall,
                color = VaultGreen,
                fontWeight = FontWeight.Medium
            )
            if (file.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\uD83C\uDFF7\uFE0F ${file.tags}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getFileVisuals(mimeType: String?): Triple<ImageVector, Color, Color> {
    return when {
        mimeType == null -> Triple(Icons.Filled.InsertDriveFile, CyanAccent, CyanSubtle)
        mimeType.startsWith("image/") -> Triple(Icons.Filled.Image, VaultGreen, VaultGreenSubtle)
        mimeType.startsWith("video/") -> Triple(Icons.Filled.VideoFile, WarningAmber, WarningSubtle)
        mimeType.startsWith("audio/") -> Triple(Icons.Filled.AudioFile, BlueAccent, Color(0x1F3B82F6))
        mimeType.contains("pdf") || mimeType.contains("document") || mimeType.contains("text") ->
            Triple(Icons.Filled.Description, CyanAccent, CyanSubtle)
        else -> Triple(Icons.Filled.InsertDriveFile, TextSecondary, DarkSurfaceVariant)
    }
}
