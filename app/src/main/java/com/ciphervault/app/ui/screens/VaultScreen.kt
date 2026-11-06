package com.ciphervault.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.data.db.EncryptedFileEntity
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.SortOption
import com.ciphervault.app.viewmodel.VaultViewModel
import com.ciphervault.app.R
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val CATEGORIES = listOf("All", "Starred", "Notes & Keys", "Images", "Documents", "Videos", "Audio", "Other")
private const val PINNED_TAG = "pinned"

// Distinct semantic palette for file categories
private val ColorImage = Color(0xFF38BDF8)      // Sky Blue
private val ColorDocument = Color(0xFFFB7185)   // Coral Rose
private val ColorAudio = Color(0xFFC084FC)      // Soft Violet
private val ColorNote = Color(0xFFFBBF24)       // Warm Gold
private val ColorStar = Color(0xFFFACC15)       // Star Yellow

// =====================================================================
//  Screen
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VaultScreen(
    vaultViewModel: VaultViewModel,
    onNavigateToEncrypt: (() -> Unit)? = null,
    onNavigateToImagePreview: ((Long) -> Unit)? = null,
    onNavigateToMediaPreview: ((Long) -> Unit)? = null
) {
    val files by vaultViewModel.files.collectAsState()
    val isLoading by vaultViewModel.isLoading.collectAsState()
    val decryptedUri by vaultViewModel.decryptedUri.collectAsState()
    val viewingNoteContent by vaultViewModel.viewingNoteContent.collectAsState()
    val viewingCredentialContent by vaultViewModel.viewingCredentialContent.collectAsState()
    val currentSort by vaultViewModel.sortOption.collectAsState()
    val highlightedFileId by vaultViewModel.highlightedFileId.collectAsState()

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var isGridView by remember { mutableStateOf(false) }
    var showStorageInsights by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var prefillNoteContent by remember { mutableStateOf("") }
    var showPasswordGeneratorDialog by remember { mutableStateOf(false) }

    // Multi-selection state
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }

    // Single file action states
    var selectedFileForDetails by remember { mutableStateOf<EncryptedFileEntity?>(null) }
    var fileToDelete by remember { mutableStateOf<EncryptedFileEntity?>(null) }
    var fileToRename by remember { mutableStateOf<EncryptedFileEntity?>(null) }
    var fileToEditTags by remember { mutableStateOf<EncryptedFileEntity?>(null) }
    var newFileNameBase by remember { mutableStateOf("") }
    var renameExtensionSuffix by remember { mutableStateOf("") }
    var newTags by remember { mutableStateOf("") }

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val app = context.applicationContext as com.ciphervault.app.CipherVaultApp
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Exit selection mode on system back press
    BackHandler(enabled = isSelectionMode) {
        selectedIds = emptySet()
    }

    // Prune stale IDs if files are deleted
    LaunchedEffect(files) {
        val validIds = files.map { it.id }.toSet()
        selectedIds = selectedIds.intersect(validIds)
    }

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

    LaunchedEffect(decryptedUri) {
        decryptedUri?.let { uri ->
            app.ignoreNextBackgroundLock = true
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
            vaultViewModel.clearDecryptedUri()
        }
    }

    fun openFile(file: EncryptedFileEntity) {
        val mime = file.mimeType ?: ""
        when {
            isNote(file) || isCredential(file) ->
                vaultViewModel.viewSecureNote(file, context)
            mime.startsWith("image/") && onNavigateToImagePreview != null ->
                onNavigateToImagePreview(file.id)
            (mime.startsWith("video/") || mime.startsWith("audio/")) && onNavigateToMediaPreview != null ->
                onNavigateToMediaPreview(file.id)
            else -> vaultViewModel.decryptFile(file)
        }
    }

    fun togglePin(file: EncryptedFileEntity) {
        val currentTags = parseTags(file.tags).toMutableList()
        if (currentTags.any { it.equals(PINNED_TAG, ignoreCase = true) }) {
            currentTags.removeAll { it.equals(PINNED_TAG, ignoreCase = true) }
        } else {
            currentTags.add(0, PINNED_TAG)
        }
        vaultViewModel.updateTags(file, currentTags.joinToString(", "))
    }

    fun toggleSelection(fileId: Long) {
        selectedIds = if (selectedIds.contains(fileId)) {
            selectedIds - fileId
        } else {
            selectedIds + fileId
        }
    }

    // Extract all user tags (excluding internal 'pinned' marker) for quick filtering & suggestions
    val allUserTags = remember(files) {
        files.flatMap { parseTags(it.tags) }
            .filterNot { it.equals(PINNED_TAG, ignoreCase = true) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    // Category counts
    val categoryCounts = remember(files) {
        CATEGORIES.associateWith { category ->
            files.count { file -> matchesCategory(file, category) }
        }
    }

    // Filter and pin-sort files
    val filteredFiles = remember(files, searchQuery, selectedCategory, selectedTagFilter) {
        files.filter { file ->
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                    displayNameOf(file).contains(q, ignoreCase = true) ||
                    file.originalFileName.contains(q, ignoreCase = true) ||
                    file.tags.contains(q, ignoreCase = true) ||
                    (file.mimeType?.contains(q, ignoreCase = true) == true)

            val matchesCat = matchesCategory(file, selectedCategory)
            val matchesTag = selectedTagFilter == null ||
                    parseTags(file.tags).any { it.equals(selectedTagFilter, ignoreCase = true) }

            matchesQuery && matchesCat && matchesTag
        }.sortedByDescending { isPinned(it) }
    }

    val totalVaultBytes = remember(files) { files.sumOf { it.originalSize } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Ground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ---- Dynamic Header vs Multi-Select Bar ----
            AnimatedContent(
                targetState = isSelectionMode,
                label = "HeaderOrSelection"
            ) { selectionActive ->
                if (selectionActive) {
                    SelectionTopBar(
                        selectedCount = selectedIds.size,
                        totalVisibleCount = filteredFiles.size,
                        onClearSelection = { selectedIds = emptySet() },
                        onSelectAll = {
                            selectedIds = if (selectedIds.size == filteredFiles.size) {
                                emptySet()
                            } else {
                                filteredFiles.map { it.id }.toSet()
                            }
                        },
                        onBatchTag = { showBatchTagDialog = true },
                        onBatchDelete = { showBatchDeleteDialog = true }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.vault_files_are),
                                style = VaultType.PageTitle,
                                color = VaultColors.TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.vault_securely_vaulted),
                                style = VaultType.PageTitle.copy(fontStyle = FontStyle.Italic),
                                color = VaultColors.Accent
                            )
                            if (files.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(VaultColors.Surface)
                                        .border(1.dp, VaultColors.Hairline, CircleShape)
                                        .clickable { showStorageInsights = !showStorageInsights }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Shield,
                                        contentDescription = null,
                                        tint = VaultColors.Accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.vault_item_count,
                                            files.size,
                                            if (files.size == 1) stringResource(R.string.vault_item) else stringResource(R.string.vault_items),
                                            formatFileSize(totalVaultBytes)
                                        ),
                                        style = VaultType.Meta,
                                        color = VaultColors.TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.PieChart,
                                        contentDescription = stringResource(R.string.vault_toggle_storage),
                                        tint = if (showStorageInsights) VaultColors.Accent else VaultColors.TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Box {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(VaultColors.Accent)
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = stringResource(R.string.vault_add),
                                        onClick = { showAddMenu = true }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = stringResource(R.string.vault_add),
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showAddMenu,
                                onDismissRequest = { showAddMenu = false },
                                modifier = Modifier.background(VaultColors.SurfaceRaised)
                            ) {
                                if (onNavigateToEncrypt != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_encrypt_files), color = VaultColors.TextPrimary) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Lock, null, tint = VaultColors.Accent, modifier = Modifier.size(20.dp))
                                        },
                                        onClick = {
                                            showAddMenu = false
                                            onNavigateToEncrypt()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_new_secure_note), color = VaultColors.TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Outlined.NoteAdd, null, tint = ColorNote, modifier = Modifier.size(20.dp))
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        prefillNoteContent = ""
                                        showCreateNoteDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_password_generator), color = VaultColors.TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Key, null, tint = ColorImage, modifier = Modifier.size(20.dp))
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        showPasswordGeneratorDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ---- Collapsible Storage Breakdown Card ----
            AnimatedVisibility(
                visible = showStorageInsights && files.isNotEmpty() && !isSelectionMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                VaultStorageBreakdownCard(
                    files = files,
                    totalBytes = totalVaultBytes,
                    onSelectCategory = { cat ->
                        selectedCategory = if (selectedCategory == cat) "All" else cat
                    },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
                )
            }

            // ---- Search, Sort and View Toggle ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.search_hint_vault), color = VaultColors.TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = VaultColors.TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = stringResource(R.string.vault_clear_search),
                                    tint = VaultColors.TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultColors.Accent,
                        unfocusedBorderColor = VaultColors.Hairline,
                        focusedContainerColor = VaultColors.Surface,
                        unfocusedContainerColor = VaultColors.Surface,
                        cursorColor = VaultColors.Accent,
                        focusedTextColor = VaultColors.TextPrimary,
                        unfocusedTextColor = VaultColors.TextPrimary
                    )
                )

                Box {
                    ToolButton(
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(R.string.vault_sort),
                        onClick = { showSortMenu = true }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(VaultColors.SurfaceRaised)
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sortOptionLabel(option),
                                        color = if (currentSort == option) VaultColors.Accent else VaultColors.TextPrimary,
                                        fontWeight = if (currentSort == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                trailingIcon = if (currentSort == option) {
                                    { Icon(Icons.Outlined.Check, null, tint = VaultColors.Accent, modifier = Modifier.size(18.dp)) }
                                } else null,
                                onClick = {
                                    vaultViewModel.setSortOption(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                ToolButton(
                    icon = if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = if (isGridView) stringResource(R.string.vault_switch_list) else stringResource(R.string.vault_switch_grid),
                    onClick = { isGridView = !isGridView }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Categories with Live Counts ----
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CATEGORIES) { category ->
                    val count = categoryCounts[category] ?: 0
                    if (category == "All" || count > 0 || selectedCategory == category) {
                        CategoryChip(
                            label = category,
                            count = count,
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }

            // ---- Quick Tag Filter Strip ----
            AnimatedVisibility(visible = allUserTags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    items(allUserTags) { tag ->
                        val isSelected = selectedTagFilter.equals(tag, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) VaultColors.AccentTint else VaultColors.Surface)
                                .border(
                                    1.dp,
                                    if (isSelected) VaultColors.Accent else VaultColors.Hairline,
                                    CircleShape
                                )
                                .clickable {
                                    selectedTagFilter = if (isSelected) null else tag
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$tag",
                                style = VaultType.Meta,
                                fontSize = 11.sp,
                                color = if (isSelected) VaultColors.Accent else VaultColors.TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Reserved height for progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(2.dp)
            ) {
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = VaultColors.Accent,
                        trackColor = Color.Transparent
                    )
                }
            }

            var localHighlightedFileId by remember { mutableStateOf<Long?>(null) }

            // ---- Content ----
            LaunchedEffect(highlightedFileId) {
                if (highlightedFileId != null) {
                    searchQuery = ""
                    selectedCategory = "All"
                    selectedTagFilter = null
                }
            }

            LaunchedEffect(highlightedFileId, filteredFiles) {
                highlightedFileId?.let { id ->
                    val index = filteredFiles.indexOfFirst { it.id == id }
                    if (index != -1) {
                        localHighlightedFileId = id
                        if (isGridView) {
                            gridState.animateScrollToItem(index)
                        } else {
                            listState.animateScrollToItem(index)
                        }
                    }
                    vaultViewModel.setHighlightedFile(null)
                    delay(1500)
                    localHighlightedFileId = null
                }
            }
            if (filteredFiles.isEmpty() && !isLoading) {
                val vaultIsEmpty = files.isEmpty()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    if (vaultIsEmpty) {
                        EmptyState(
                            icon = Icons.Outlined.Lock,
                            title = stringResource(R.string.vault_empty_title),
                            body = stringResource(R.string.vault_empty_body),
                            actionLabel = if (onNavigateToEncrypt != null) stringResource(R.string.action_encrypt_files) else stringResource(R.string.vault_create_note),
                            onAction = {
                                if (onNavigateToEncrypt != null) onNavigateToEncrypt()
                                else showCreateNoteDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val activeTagFilter = selectedTagFilter
                        EmptyState(
                            icon = Icons.Outlined.Search,
                            title = stringResource(R.string.vault_no_matches_title),
                            body = buildString {
                                append(stringResource(R.string.vault_nothing_matches))
                                if (searchQuery.isNotBlank()) append("\"$searchQuery\" ")
                                if (selectedCategory != "All") append(stringResource(R.string.vault_in_category, categoryLabelOf(selectedCategory)))
                                if (activeTagFilter != null) append(stringResource(R.string.vault_tagged, activeTagFilter))
                            }.trim() + ".",
                            actionLabel = stringResource(R.string.vault_reset_filters),
                            onAction = {
                                searchQuery = ""
                                selectedCategory = "All"
                                selectedTagFilter = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = filteredFiles, key = { it.id }) { file ->
                        val isSelected = selectedIds.contains(file.id)
                        VaultFileGridCard(
                            file = file,
                            isSelected = isSelected,
                            isHighlighted = localHighlightedFileId == file.id,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) toggleSelection(file.id)
                                else openFile(file)
                            },
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                toggleSelection(file.id)
                            },
                            onMore = { selectedFileForDetails = file }
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)
                ) {
                    itemsIndexed(
                        items = filteredFiles,
                        key = { _, file -> file.id }
                    ) { index, file ->
                        val isSelected = selectedIds.contains(file.id)
                        VaultFileListRow(
                            file = file,
                            index = index,
                            count = filteredFiles.size,
                            isSelected = isSelected,
                            isHighlighted = localHighlightedFileId == file.id,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) toggleSelection(file.id)
                                else openFile(file)
                            },
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                toggleSelection(file.id)
                            },
                            onMore = { selectedFileForDetails = file }
                        )
                    }
                }
            }
        }
    }

    // =================================================================
    //  File details sheet
    // =================================================================

    selectedFileForDetails?.let { file ->
        ModalBottomSheet(
            onDismissRequest = { selectedFileForDetails = null },
            sheetState = sheetState,
            containerColor = VaultColors.SurfaceRaised,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(VaultColors.Hairline)
                )
            }
        ) {
            FileDetailsContent(
                file = file,
                onOpen = {
                    openFile(file)
                    selectedFileForDetails = null
                },
                onTogglePin = {
                    togglePin(file)
                    selectedFileForDetails = null
                },
                onExport = {
                    fileToExport = file
                    selectedFileForDetails = null
                    app.ignoreNextBackgroundLock = true
                    exportFileLauncher.launch(file.originalFileName)
                },
                onEditTags = {
                    newTags = parseTags(file.tags)
                        .filterNot { it.equals(PINNED_TAG, ignoreCase = true) }
                        .joinToString(", ")
                    fileToEditTags = file
                    selectedFileForDetails = null
                },
                onRename = {
                    val (base, ext) = splitFileNameAndExtension(file.originalFileName)
                    newFileNameBase = base
                    renameExtensionSuffix = ext
                    fileToRename = file
                    selectedFileForDetails = null
                },
                onDelete = {
                    fileToDelete = file
                    selectedFileForDetails = null
                }
            )
        }
    }

    // =================================================================
    //  Dialogs
    // =================================================================

    // Smart Extension-Preserving Rename Dialog
    fileToRename?.let { file ->
        VaultDialog(
            icon = Icons.Outlined.Edit,
            title = stringResource(R.string.vault_rename_title),
            subtitle = if (renameExtensionSuffix.isNotEmpty()) {
                stringResource(R.string.vault_rename_desc, renameExtensionSuffix)
            } else null,
            confirmLabel = stringResource(R.string.vault_rename_btn),
            confirmEnabled = newFileNameBase.isNotBlank(),
            onConfirm = {
                val finalName = newFileNameBase.trim() + renameExtensionSuffix
                vaultViewModel.renameFile(file, finalName)
                fileToRename = null
            },
            onDismiss = { fileToRename = null }
        ) {
            VaultTextField(
                value = newFileNameBase,
                onValueChange = { newFileNameBase = it },
                label = stringResource(R.string.vault_rename_label),
                singleLine = true
            )
        }
    }

    // Interactive Tag Editor Dialog
    fileToEditTags?.let { file ->
        val wasPinned = isPinned(file)
        InteractiveTagDialog(
            initialTags = newTags,
            existingVaultTags = allUserTags,
            onSave = { updatedUserTags ->
                val finalTags = buildList {
                    if (wasPinned) add(PINNED_TAG)
                    addAll(updatedUserTags)
                }.joinToString(", ")
                vaultViewModel.updateTags(file, finalTags)
                fileToEditTags = null
            },
            onDismiss = { fileToEditTags = null }
        )
    }

    // Batch Tag Dialog
    if (showBatchTagDialog) {
        InteractiveTagDialog(
            initialTags = "",
            existingVaultTags = allUserTags,
            title = stringResource(R.string.vault_tag_multi_title, selectedIds.size),
            subtitle = stringResource(R.string.vault_tag_multi_desc),
            onSave = { addedTags ->
                val selectedFiles = files.filter { selectedIds.contains(it.id) }
                selectedFiles.forEach { file ->
                    val merged = (parseTags(file.tags) + addedTags)
                        .distinctBy { it.lowercase() }
                        .joinToString(", ")
                    vaultViewModel.updateTags(file, merged)
                }
                showBatchTagDialog = false
                selectedIds = emptySet()
            },
            onDismiss = { showBatchTagDialog = false }
        )
    }

    // Single Delete Confirmation
    fileToDelete?.let { file ->
        VaultDialog(
            icon = Icons.Outlined.DeleteOutline,
            iconTint = VaultColors.ErrorRed,
            title = stringResource(R.string.vault_delete_title),
            subtitle = stringResource(R.string.vault_delete_subtitle, displayNameOf(file)),
            confirmLabel = stringResource(R.string.vault_delete_btn),
            confirmColor = VaultColors.ErrorRed,
            confirmContentColor = Color.White,
            onConfirm = {
                vaultViewModel.deleteFile(file)
                fileToDelete = null
            },
            onDismiss = { fileToDelete = null }
        ) {}
    }

    // Batch Delete Confirmation
    if (showBatchDeleteDialog) {
        VaultDialog(
            icon = Icons.Outlined.DeleteOutline,
            iconTint = VaultColors.ErrorRed,
            title = stringResource(R.string.vault_delete_multi_title, selectedIds.size),
            subtitle = stringResource(R.string.vault_delete_multi_desc, selectedIds.size),
            confirmLabel = stringResource(R.string.vault_delete_multi_btn, selectedIds.size),
            confirmColor = VaultColors.ErrorRed,
            confirmContentColor = Color.White,
            onConfirm = {
                val toDelete = files.filter { selectedIds.contains(it.id) }
                toDelete.forEach { vaultViewModel.deleteFile(it) }
                selectedIds = emptySet()
                showBatchDeleteDialog = false
            },
            onDismiss = { showBatchDeleteDialog = false }
        ) {}
    }

    // Create Secure Note Dialog
    if (showCreateNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf(prefillNoteContent) }
        VaultDialog(
            icon = Icons.AutoMirrored.Outlined.NoteAdd,
            iconTint = ColorNote,
            title = stringResource(R.string.action_new_secure_note),
            subtitle = stringResource(R.string.vault_note_subtitle),
            confirmLabel = stringResource(R.string.vault_note_save),
            confirmEnabled = noteTitle.isNotBlank() && noteContent.isNotBlank(),
            onConfirm = {
                vaultViewModel.createSecureNote(context, noteTitle.trim(), noteContent)
                prefillNoteContent = ""
                showCreateNoteDialog = false
            },
            onDismiss = {
                prefillNoteContent = ""
                showCreateNoteDialog = false
            }
        ) {
            VaultTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                label = stringResource(R.string.vault_note_title_label),
                leadingIcon = Icons.Outlined.Title,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )
            VaultTextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                label = stringResource(R.string.vault_note_content),
                minLines = 5,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(R.string.format_length_characters, noteContent.length),
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary
                )
            }
        }
    }

    // Built-in Password Generator Dialog
    if (showPasswordGeneratorDialog) {
        PasswordGeneratorDialog(
            onCopy = { generated ->
                copyToClipboard(context, "Generated Password", generated)
            },
            onSaveAsNote = { generated ->
                showPasswordGeneratorDialog = false
                prefillNoteContent = "Password: $generated"
                showCreateNoteDialog = true
            },
            onDismiss = { showPasswordGeneratorDialog = false }
        )
    }

    // View Secure Note Dialog with Copy Action
    viewingNoteContent?.let { (title, content) ->
        var copied by remember { mutableStateOf(false) }
        LaunchedEffect(copied) {
            if (copied) {
                delay(2000)
                copied = false
            }
        }

        VaultDialog(
            icon = Icons.Outlined.Description,
            iconTint = ColorNote,
            title = title.removeSuffix(".note.txt"),
            confirmLabel = stringResource(R.string.vault_done),
            dismissLabel = if (copied) stringResource(R.string.vault_copied) else stringResource(R.string.vault_copy_all),
            onConfirm = { vaultViewModel.clearViewingNote() },
            onDismiss = {
                copyToClipboard(context, "Secure Note", content)
                copied = true
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(VaultColors.Ground)
                    .padding(16.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = content,
                        style = VaultType.Meta.copy(fontSize = 14.sp, lineHeight = 21.sp),
                        color = VaultColors.TextPrimary
                    )
                }
            }
        }
    }

    // View Credential Dialog with Copy & Entropy Meter
    viewingCredentialContent?.let { (title, creds) ->
        CredentialViewDialog(
            title = title.removeSuffix(".cred.txt"),
            username = creds["username"] ?: "",
            password = creds["password"] ?: "",
            onCopyUsername = { copyToClipboard(context, "Username", creds["username"] ?: "") },
            onCopyPassword = { copyToClipboard(context, "Password", creds["password"] ?: "") },
            onClose = { vaultViewModel.clearViewingCredential() }
        )
    }
}

// =====================================================================
//  Top Selection Bar & Storage Breakdown
// =====================================================================

@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    totalVisibleCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onBatchTag: () -> Unit,
    onBatchDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(VaultColors.SurfaceRaised)
            .border(1.dp, VaultColors.Accent.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.vault_cancel_selection), tint = VaultColors.TextPrimary)
            }
            Text(
                text = stringResource(R.string.vault_selected_count, selectedCount),
                style = VaultType.ItemTitle,
                color = VaultColors.Accent
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSelectAll) {
                Icon(
                    imageVector = Icons.Outlined.SelectAll,
                    contentDescription = if (selectedCount == totalVisibleCount) stringResource(R.string.vault_deselect_all) else stringResource(R.string.vault_select_all),
                    tint = if (selectedCount == totalVisibleCount) VaultColors.Accent else VaultColors.TextPrimary
                )
            }
            IconButton(onClick = onBatchTag) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = stringResource(R.string.vault_tag_selected),
                    tint = VaultColors.TextPrimary
                )
            }
            IconButton(onClick = onBatchDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.vault_delete_selected),
                    tint = VaultColors.ErrorRed
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VaultStorageBreakdownCard(
    files: List<EncryptedFileEntity>,
    totalBytes: Long,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(files) {
        listOf(
            Triple("Images", ColorImage, files.filter { matchesCategory(it, "Images") }.sumOf { it.originalSize }),
            Triple("Videos", VaultColors.WarningAmber, files.filter { matchesCategory(it, "Videos") }.sumOf { it.originalSize }),
            Triple("Documents", ColorDocument, files.filter { matchesCategory(it, "Documents") }.sumOf { it.originalSize }),
            Triple("Audio", ColorAudio, files.filter { matchesCategory(it, "Audio") }.sumOf { it.originalSize }),
            Triple("Notes & Keys", ColorNote, files.filter { matchesCategory(it, "Notes & Keys") }.sumOf { it.originalSize }),
            Triple("Other", VaultColors.TextSecondary, files.filter { matchesCategory(it, "Other") }.sumOf { it.originalSize })
        ).filter { it.third > 0L }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(VaultColors.Surface)
            .border(1.dp, VaultColors.Hairline, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.vault_storage_breakdown),
                style = VaultType.Meta,
                color = VaultColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "AES-256-GCM",
                style = VaultType.Meta,
                fontSize = 11.sp,
                color = VaultColors.Accent,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented proportional bar
        if (totalBytes > 0L && segments.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(VaultColors.Ground),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                segments.forEach { (_, color, bytes) ->
                    val weight = (bytes.toFloat() / totalBytes.toFloat()).coerceAtLeast(0.04f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            segments.forEach { (label, color, bytes) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onSelectCategory(label) }
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${categoryLabelOf(label)} (${formatFileSize(bytes)})",
                        style = VaultType.Meta,
                        fontSize = 11.sp,
                        color = VaultColors.TextPrimary
                    )
                }
            }
        }
    }
}

// =====================================================================
//  Controls
// =====================================================================

@Composable
private fun ToolButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(shape)
            .background(VaultColors.Surface)
            .border(1.dp, VaultColors.Hairline, shape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = VaultColors.Accent,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) VaultColors.Accent else VaultColors.Surface)
            .border(
                1.dp,
                if (selected) VaultColors.Accent else VaultColors.Hairline,
                CircleShape
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (label == "Starred") {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = if (selected) Color.Black else ColorStar,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = categoryLabelOf(label),
            style = VaultType.Meta,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.Black else VaultColors.TextSecondary
        )
        if (count > 0) {
            Text(
                text = count.toString(),
                style = VaultType.Meta,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.Black.copy(alpha = 0.75f) else VaultColors.TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(
                icon = icon,
                size = 76.dp,
                iconSize = 34.dp,
                tint = VaultColors.Accent,
                background = VaultColors.SurfaceRaised
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                style = VaultType.ItemTitle,
                color = VaultColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                style = VaultType.Meta,
                color = VaultColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = onAction,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultColors.Accent,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =====================================================================
//  File list and grid
// =====================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultFileListRow(
    file: EncryptedFileEntity,
    index: Int,
    count: Int,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit
) {
    val (icon, iconColor, iconBg) = getFileVisuals(file.mimeType, file.originalFileName)
    val shape = groupedShape(index, count)
    val pinned = isPinned(file)
    val visibleTags = remember(file.tags) {
        parseTags(file.tags).filterNot { it.equals(PINNED_TAG, ignoreCase = true) }
    }

    val highlightColor by animateColorAsState(
        targetValue = if (isHighlighted) VaultColors.Accent.copy(alpha = 0.2f) else if (isSelected) VaultColors.AccentTint else VaultColors.Surface,
        animationSpec = tween(durationMillis = 500),
        label = "highlightColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(highlightColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = VaultColors.Accent,
                        checkmarkColor = Color.Black,
                        uncheckedColor = VaultColors.TextSecondary
                    ),
                    modifier = Modifier.size(46.dp)
                )
            } else {
                IconBadge(
                    icon = icon,
                    size = 46.dp,
                    shape = RoundedCornerShape(13.dp),
                    tint = iconColor,
                    background = iconBg
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pinned) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = stringResource(R.string.vault_pinned),
                            tint = ColorStar,
                            modifier = Modifier
                                .size(15.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = displayNameOf(file),
                        style = VaultType.ItemTitle.copy(fontSize = 15.sp),
                        color = VaultColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = kindLabelOf(file) ?: formatFileSize(file.originalSize),
                        style = VaultType.Meta,
                        color = iconColor,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = formatDate(file.encryptedAt),
                        style = VaultType.Meta,
                        color = VaultColors.TextSecondary
                    )
                }
                if (visibleTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TagChips(tags = visibleTags)
                }
            }

            if (!isSelectionMode) {
                IconButton(onClick = onMore) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.vault_more_options, displayNameOf(file)),
                        tint = VaultColors.TextSecondary
                    )
                }
            }
        }

        if (index < count - 1) {
            Box(
                modifier = Modifier
                    .padding(start = 74.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VaultColors.Hairline)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultFileGridCard(
    file: EncryptedFileEntity,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit
) {
    val (icon, iconColor, iconBg) = getFileVisuals(file.mimeType, file.originalFileName)
    val pinned = isPinned(file)
    val visibleTags = remember(file.tags) {
        parseTags(file.tags).filterNot { it.equals(PINNED_TAG, ignoreCase = true) }
    }

    val highlightColor by animateColorAsState(
        targetValue = if (isHighlighted) VaultColors.Accent.copy(alpha = 0.2f) else if (isSelected) VaultColors.AccentTint else VaultColors.Surface,
        animationSpec = tween(durationMillis = 500),
        label = "highlightColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) VaultColors.Accent else if (isSelected) VaultColors.Accent else VaultColors.Hairline,
        animationSpec = tween(durationMillis = 500),
        label = "borderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = highlightColor
        ),
        border = BorderStroke(
            width = if (isSelected || isHighlighted) 1.5.dp else 1.dp,
            color = borderColor
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 14.dp, end = 6.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                IconBadge(
                    icon = icon,
                    size = 44.dp,
                    shape = RoundedCornerShape(12.dp),
                    tint = iconColor,
                    background = iconBg
                )
                if (isSelectionMode) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (isSelected) VaultColors.Accent else VaultColors.TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier
                            .padding(end = 8.dp, top = 4.dp)
                            .size(22.dp)
                    )
                } else {
                    IconButton(onClick = onMore, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.vault_more_options, displayNameOf(file)),
                            tint = VaultColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (pinned) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = stringResource(R.string.vault_pinned),
                        tint = ColorStar,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp)
                    )
                }
                Text(
                    text = displayNameOf(file),
                    style = VaultType.ItemTitle.copy(fontSize = 14.sp),
                    color = VaultColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = kindLabelOf(file) ?: formatFileSize(file.originalSize),
                style = VaultType.Meta,
                color = iconColor,
                fontWeight = FontWeight.Medium
            )

            if (visibleTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TagChips(tags = visibleTags, maxVisible = 1)
            }
        }
    }
}

@Composable
private fun TagChips(
    tags: List<String>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 2
) {
    if (tags.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tags.take(maxVisible).forEach { tag ->
            Text(
                text = "#$tag",
                style = VaultType.Meta,
                fontSize = 11.sp,
                color = VaultColors.Accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 110.dp)
                    .clip(CircleShape)
                    .background(VaultColors.AccentTint)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        if (tags.size > maxVisible) {
            Text(
                text = "+${tags.size - maxVisible}",
                style = VaultType.Meta,
                fontSize = 11.sp,
                color = VaultColors.TextSecondary
            )
        }
    }
}

// =====================================================================
//  Details sheet content
// =====================================================================

@Composable
private fun FileDetailsContent(
    file: EncryptedFileEntity,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onExport: () -> Unit,
    onEditTags: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, iconColor, iconBg) = getFileVisuals(file.mimeType, file.originalFileName)
    val pinned = isPinned(file)
    val visibleTags = remember(file.tags) {
        parseTags(file.tags).filterNot { it.equals(PINNED_TAG, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = icon,
                size = 56.dp,
                iconSize = 28.dp,
                shape = RoundedCornerShape(16.dp),
                tint = iconColor,
                background = iconBg
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayNameOf(file),
                    style = VaultType.ItemTitle,
                    color = VaultColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatFileSize(file.originalSize)} • ${kindLabelOf(file) ?: (file.mimeType ?: stringResource(R.string.vault_binary_data))}",
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Security & Metadata Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VaultColors.Ground)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.label_cipher), style = VaultType.Meta, color = VaultColors.TextSecondary)
                Text(
                    stringResource(R.string.vault_cipher_hardware),
                    style = VaultType.Meta,
                    color = VaultColors.Accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.label_encrypted_on), style = VaultType.Meta, color = VaultColors.TextSecondary)
                Text(formatDate(file.encryptedAt), style = VaultType.Meta, color = VaultColors.TextPrimary)
            }
            if (visibleTags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.label_tags), style = VaultType.Meta, color = VaultColors.TextSecondary)
                    TagChips(tags = visibleTags, maxVisible = 3)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary action
        Button(
            onClick = onOpen,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VaultColors.Accent,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(Icons.Outlined.LockOpen, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(stringResource(R.string.action_open_decrypted_preview), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Secondary actions
        SheetAction(
            index = 0,
            count = 4,
            icon = if (pinned) Icons.Outlined.Star else Icons.Outlined.StarBorder,
            iconTint = ColorStar,
            label = if (pinned) stringResource(R.string.vault_remove_starred) else stringResource(R.string.vault_add_starred),
            onClick = onTogglePin
        )
        SheetAction(
            index = 1,
            count = 4,
            icon = Icons.Outlined.Download,
            label = stringResource(R.string.vault_export_copy),
            onClick = onExport
        )
        SheetAction(
            index = 2,
            count = 4,
            icon = Icons.AutoMirrored.Outlined.Label,
            label = stringResource(R.string.vault_manage_tags),
            onClick = onEditTags
        )
        SheetAction(
            index = 3,
            count = 4,
            icon = Icons.Outlined.Edit,
            label = stringResource(R.string.vault_rename_btn),
            onClick = onRename
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Destructive action
        SheetAction(
            index = 0,
            count = 1,
            icon = Icons.Outlined.DeleteOutline,
            label = stringResource(R.string.vault_delete_from_vault),
            tint = VaultColors.ErrorRed,
            iconTint = VaultColors.ErrorRed,
            onClick = onDelete
        )

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SheetAction(
    index: Int,
    count: Int,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = VaultColors.TextPrimary,
    iconTint: Color = VaultColors.Accent
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(groupedShape(index, count, radius = 16.dp))
            .background(VaultColors.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = VaultType.ItemTitle.copy(fontSize = 15.sp),
                color = tint
            )
        }
        if (index < count - 1) {
            Box(
                modifier = Modifier
                    .padding(start = 50.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VaultColors.Hairline)
            )
        }
    }
}

// =====================================================================
//  Dialogs (Credentials, Interactive Tags, Password Generator)
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractiveTagDialog(
    initialTags: String,
    existingVaultTags: List<String>,
    title: String = stringResource(R.string.vault_manage_tags),
    subtitle: String = stringResource(R.string.vault_tag_subtitle),
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var currentTagList by remember {
        mutableStateOf(parseTags(initialTags).filterNot { it.equals(PINNED_TAG, ignoreCase = true) })
    }
    var tagInput by remember { mutableStateOf("") }

    fun commitPendingInput() {
        val cleaned = tagInput.split(',').map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
        if (cleaned.isNotEmpty()) {
            currentTagList = (currentTagList + cleaned).distinctBy { it.lowercase() }
            tagInput = ""
        }
    }

    VaultDialog(
        icon = Icons.AutoMirrored.Outlined.Label,
        title = title,
        subtitle = subtitle,
        confirmLabel = stringResource(R.string.vault_tag_save),
        onConfirm = {
            commitPendingInput()
            onSave(currentTagList)
        },
        onDismiss = onDismiss
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VaultTextField(
                value = tagInput,
                onValueChange = {
                    if (it.endsWith(",")) {
                        val newTag = it.removeSuffix(",").trim().removePrefix("#")
                        if (newTag.isNotEmpty()) {
                            currentTagList = (currentTagList + newTag).distinctBy { t -> t.lowercase() }
                        }
                        tagInput = ""
                    } else {
                        tagInput = it
                    }
                },
                label = stringResource(R.string.vault_tag_new),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { commitPendingInput() },
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VaultColors.Accent)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.action_add_tag), tint = Color.Black)
            }
        }

        if (currentTagList.isNotEmpty()) {
            Text(stringResource(R.string.label_assigned_tags), style = VaultType.Meta, color = VaultColors.TextSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentTagList.forEach { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(VaultColors.AccentTint)
                            .border(1.dp, VaultColors.Accent, CircleShape)
                            .clickable {
                                currentTagList = currentTagList.filterNot { it.equals(tag, ignoreCase = true) }
                            }
                            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text("#$tag", style = VaultType.Meta, color = VaultColors.Accent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.vault_remove_tag, tag),
                            tint = VaultColors.Accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        val suggestions = existingVaultTags.filterNot { existing ->
            currentTagList.any { it.equals(existing, ignoreCase = true) }
        }
        if (suggestions.isNotEmpty()) {
            Text(stringResource(R.string.action_quick_add_vault), style = VaultType.Meta, color = VaultColors.TextSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.take(8).forEach { suggestion ->
                    Text(
                        text = "+ #$suggestion",
                        style = VaultType.Meta,
                        fontSize = 11.sp,
                        color = VaultColors.TextSecondary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(VaultColors.Ground)
                            .border(1.dp, VaultColors.Hairline, CircleShape)
                            .clickable {
                                currentTagList = (currentTagList + suggestion).distinctBy { it.lowercase() }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordGeneratorDialog(
    onCopy: (String) -> Unit,
    onSaveAsNote: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var length by remember { mutableFloatStateOf(20f) }
    var useUpper by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var generatedPassword by remember {
        mutableStateOf(generateRandomPassword(20, true, true, true))
    }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    fun regenerate() {
        generatedPassword = generateRandomPassword(
            length = length.toInt(),
            includeUpper = useUpper,
            includeDigits = useDigits,
            includeSymbols = useSymbols
        )
    }

    VaultDialog(
        icon = Icons.Outlined.Key,
        iconTint = ColorImage,
        title = stringResource(R.string.action_password_generator),
        subtitle = stringResource(R.string.vault_pwd_gen_sub),
        confirmLabel = stringResource(R.string.vault_save_as_note),
        dismissLabel = stringResource(R.string.dialog_close),
        onConfirm = { onSaveAsNote(generatedPassword) },
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VaultColors.Ground)
                .border(1.dp, VaultColors.Hairline, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = generatedPassword,
                    style = VaultType.ItemTitle.copy(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
                    color = VaultColors.Accent,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { regenerate() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.vault_regenerate), tint = VaultColors.TextSecondary)
                }
                IconButton(
                    onClick = {
                        onCopy(generatedPassword)
                        copied = true
                    }
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.vault_copy_password),
                        tint = if (copied) VaultColors.Accent else VaultColors.TextPrimary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.format_length_characters, length.toInt()), style = VaultType.Meta, color = VaultColors.TextPrimary)
            val (strengthLabel, strengthColor) = passwordStrengthLabel(generatedPassword)
            Text(strengthLabel, style = VaultType.Meta, color = strengthColor, fontWeight = FontWeight.Bold)
        }

        Slider(
            value = length,
            onValueChange = {
                length = it
                regenerate()
            },
            valueRange = 8f..40f,
            steps = 31,
            colors = SliderDefaults.colors(
                thumbColor = VaultColors.Accent,
                activeTrackColor = VaultColors.Accent,
                inactiveTrackColor = VaultColors.Ground
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GeneratorToggleChip(stringResource(R.string.vault_pwd_az), useUpper, Modifier.weight(1f)) {
                useUpper = !useUpper
                regenerate()
            }
            GeneratorToggleChip("0-9", useDigits, Modifier.weight(1f)) {
                useDigits = !useDigits
                regenerate()
            }
            GeneratorToggleChip("!@#", useSymbols, Modifier.weight(1f)) {
                useSymbols = !useSymbols
                regenerate()
            }
        }
    }
}

@Composable
private fun GeneratorToggleChip(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) VaultColors.AccentTint else VaultColors.Ground)
            .border(1.dp, if (checked) VaultColors.Accent else VaultColors.Hairline, RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = VaultType.Meta,
            fontWeight = FontWeight.Bold,
            color = if (checked) VaultColors.Accent else VaultColors.TextSecondary
        )
    }
}

@Composable
private fun CredentialViewDialog(
    title: String,
    username: String,
    password: String,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onClose: () -> Unit
) {
    var reveal by remember { mutableStateOf(false) }
    var copiedField by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedField) {
        if (copiedField != null) {
            delay(1800)
            copiedField = null
        }
    }

    val (strengthLabel, strengthColor) = passwordStrengthLabel(password)

    VaultDialog(
        icon = Icons.Outlined.Key,
        title = title,
        confirmLabel = stringResource(R.string.dialog_close),
        dismissLabel = null,
        onConfirm = onClose,
        onDismiss = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VaultColors.Ground)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_username_or_email), color = VaultColors.TextSecondary, style = VaultType.Chip)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = username.ifEmpty { stringResource(R.string.vault_not_set) },
                        color = if (username.isEmpty()) VaultColors.TextSecondary else VaultColors.TextPrimary,
                        style = VaultType.ItemTitle
                    )
                }
                if (username.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onCopyUsername()
                            copiedField = "user"
                        }
                    ) {
                        Icon(
                            imageVector = if (copiedField == "user") Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.vault_copy_username),
                            tint = if (copiedField == "user") VaultColors.Accent else VaultColors.TextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VaultColors.Hairline)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.filter_password), color = VaultColors.TextSecondary, style = VaultType.Chip)
                        if (password.isNotEmpty()) {
                            Text(
                                text = strengthLabel,
                                style = VaultType.Meta,
                                fontSize = 10.sp,
                                color = strengthColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (reveal) password else "\u2022".repeat(12),
                        color = VaultColors.TextPrimary,
                        style = VaultType.ItemTitle.copy(fontFamily = if (reveal) FontFamily.Monospace else FontFamily.Default)
                    )
                }
                IconButton(onClick = { reveal = !reveal }) {
                    Icon(
                        imageVector = if (reveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (reveal) stringResource(R.string.lock_hide_password) else stringResource(R.string.lock_show_password),
                        tint = VaultColors.TextSecondary
                    )
                }
                if (password.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onCopyPassword()
                            copiedField = "pass"
                        }
                    ) {
                        Icon(
                            imageVector = if (copiedField == "pass") Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.vault_copy_password),
                            tint = if (copiedField == "pass") VaultColors.Accent else VaultColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultDialog(
    icon: ImageVector,
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    iconTint: Color = VaultColors.Accent,
    confirmEnabled: Boolean = true,
    confirmColor: Color = VaultColors.Accent,
    confirmContentColor: Color = Color.Black,
    dismissLabel: String? = stringResource(R.string.dialog_cancel),
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = VaultColors.SurfaceRaised,
        icon = {
            IconBadge(
                icon = icon,
                size = 52.dp,
                iconSize = 26.dp,
                tint = iconTint,
                background = iconTint.copy(alpha = 0.12f)
            )
        },
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
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = VaultType.Meta,
                        color = VaultColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                content()
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor,
                    contentColor = confirmContentColor,
                    disabledContainerColor = confirmColor.copy(alpha = 0.3f),
                    disabledContentColor = confirmContentColor.copy(alpha = 0.5f)
                )
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = if (dismissLabel != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel, color = VaultColors.TextSecondary)
                }
            }
        } else {
            null
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
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
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
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
            focusedContainerColor = VaultColors.Ground.copy(alpha = 0.4f),
            unfocusedContainerColor = VaultColors.Ground.copy(alpha = 0.4f)
        )
    )
}

// =====================================================================
//  Building blocks
// =====================================================================

@Composable
private fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    shape: Shape = CircleShape,
    tint: Color = VaultColors.Accent,
    background: Color = VaultColors.AccentTint
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background),
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

private fun groupedShape(index: Int, count: Int, radius: Dp = 18.dp): Shape = when {
    count <= 1 -> RoundedCornerShape(radius)
    index == 0 -> RoundedCornerShape(topStart = radius, topEnd = radius)
    index == count - 1 -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    else -> RectangleShape
}

// =====================================================================
//  File, Tag & Security Helpers
// =====================================================================

private fun isNote(file: EncryptedFileEntity) = file.originalFileName.endsWith(".note.txt")
private fun isCredential(file: EncryptedFileEntity) = file.originalFileName.endsWith(".cred.txt")

private fun parseTags(raw: String): List<String> =
    raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }

private fun isPinned(file: EncryptedFileEntity): Boolean =
    parseTags(file.tags).any { it.equals(PINNED_TAG, ignoreCase = true) }

private fun displayNameOf(file: EncryptedFileEntity): String =
    file.originalFileName.removeSuffix(".note.txt").removeSuffix(".cred.txt")

private fun splitFileNameAndExtension(fullName: String): Pair<String, String> {
    return when {
        fullName.endsWith(".note.txt") -> fullName.removeSuffix(".note.txt") to ".note.txt"
        fullName.endsWith(".cred.txt") -> fullName.removeSuffix(".cred.txt") to ".cred.txt"
        else -> {
            val dotIndex = fullName.lastIndexOf('.')
            if (dotIndex > 0 && dotIndex < fullName.length - 1) {
                fullName.substring(0, dotIndex) to fullName.substring(dotIndex)
            } else {
                fullName to ""
            }
        }
    }
}

@Composable
private fun kindLabelOf(file: EncryptedFileEntity): String? = when {
    isNote(file) -> stringResource(R.string.vault_type_secure_note)
    isCredential(file) -> stringResource(R.string.vault_type_credential)
    else -> null
}

@Composable
private fun categoryLabelOf(category: String): String {
    return when (category) {
        "All" -> stringResource(R.string.vault_cat_all)
        "Starred" -> stringResource(R.string.vault_cat_starred)
        "Notes & Keys" -> stringResource(R.string.vault_cat_notes)
        "Images" -> stringResource(R.string.vault_cat_images)
        "Documents" -> stringResource(R.string.vault_cat_documents)
        "Videos" -> stringResource(R.string.vault_cat_videos)
        "Audio" -> stringResource(R.string.vault_cat_audio)
        "Other" -> stringResource(R.string.vault_cat_other)
        else -> category
    }
}

@Composable
private fun sortOptionLabel(option: SortOption): String {
    return when (option) {
        SortOption.DATE_DESC -> stringResource(R.string.sort_newest)
        SortOption.DATE_ASC -> stringResource(R.string.sort_oldest)
        SortOption.NAME_ASC -> stringResource(R.string.sort_name_asc)
        SortOption.NAME_DESC -> stringResource(R.string.sort_name_desc)
        SortOption.SIZE_DESC -> stringResource(R.string.sort_size_desc)
        SortOption.SIZE_ASC -> stringResource(R.string.sort_size_asc)
    }
}

@Composable
private fun passwordStrengthLabel(password: String): Pair<String, Color> {
    val (key, color) = remember(password) { passwordStrengthOf(password) }
    val label = when (key) {
        "Strong" -> stringResource(R.string.setup_strong)
        "Moderate" -> stringResource(R.string.setup_fair)
        else -> stringResource(R.string.setup_weak)
    }
    return label to color
}

private fun matchesCategory(file: EncryptedFileEntity, category: String): Boolean {
    val mime = file.mimeType
    val noteOrCred = isNote(file) || isCredential(file)
    return when (category) {
        "All" -> true
        "Starred" -> isPinned(file)
        "Notes & Keys" -> noteOrCred
        "Images" -> mime?.startsWith("image/") == true
        "Documents" -> !noteOrCred && mime != null &&
                (mime.contains("pdf") || mime.contains("document") || mime.contains("text"))
        "Videos" -> mime?.startsWith("video/") == true
        "Audio" -> mime?.startsWith("audio/") == true
        "Other" -> !noteOrCred && (mime == null || !(
                mime.startsWith("image/") ||
                        mime.startsWith("video/") ||
                        mime.startsWith("audio/") ||
                        mime.contains("pdf") ||
                        mime.contains("document") ||
                        mime.contains("text")
                ))
        else -> true
    }
}

private fun getFileVisuals(mimeType: String?, originalFileName: String = ""): Triple<ImageVector, Color, Color> {
    val ext = originalFileName.substringAfterLast('.', "").lowercase()
    return when {
        originalFileName.endsWith(".cred.txt") ->
            Triple(Icons.Outlined.Key, VaultColors.Accent, VaultColors.AccentTint)
        originalFileName.endsWith(".note.txt") ->
            Triple(Icons.Outlined.Description, ColorNote, ColorNote.copy(alpha = 0.14f))
        mimeType?.startsWith("image/") == true ->
            Triple(Icons.Outlined.Image, ColorImage, ColorImage.copy(alpha = 0.14f))
        mimeType?.startsWith("video/") == true ->
            Triple(Icons.Outlined.Videocam, VaultColors.WarningAmber, VaultColors.WarningAmber.copy(alpha = 0.14f))
        mimeType?.startsWith("audio/") == true ->
            Triple(Icons.Outlined.AudioFile, ColorAudio, ColorAudio.copy(alpha = 0.14f))
        mimeType == "application/pdf" || ext == "pdf" ->
            Triple(Icons.Outlined.PictureAsPdf, ColorDocument, ColorDocument.copy(alpha = 0.14f))
        ext in setOf("zip", "rar", "7z", "tar", "gz") || mimeType?.contains("zip") == true ->
            Triple(Icons.Outlined.FolderZip, VaultColors.TextSecondary, VaultColors.SurfaceRaised)
        mimeType?.contains("pdf") == true || mimeType?.contains("document") == true || mimeType?.contains("text") == true || ext in setOf("doc", "docx", "txt", "md", "odt", "rtf") ->
            Triple(Icons.Outlined.Description, ColorDocument, ColorDocument.copy(alpha = 0.14f))
        else ->
            Triple(Icons.AutoMirrored.Outlined.InsertDriveFile, VaultColors.TextSecondary, VaultColors.SurfaceRaised)
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun generateRandomPassword(
    length: Int,
    includeUpper: Boolean,
    includeDigits: Boolean,
    includeSymbols: Boolean
): String {
    val lower = "abcdefghijkmnopqrstuvwxyz"
    val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    val digits = "23456789"
    val symbols = "!@#$%^&*()-_=+[]{}?"

    val pool = buildString {
        append(lower)
        if (includeUpper) append(upper)
        if (includeDigits) append(digits)
        if (includeSymbols) append(symbols)
    }
    val random = SecureRandom()
    return (1..length)
        .map { pool[random.nextInt(pool.length)] }
        .joinToString("")
}

private fun passwordStrengthOf(password: String): Pair<String, Color> {
    var score = 0
    if (password.length >= 12) score++
    if (password.length >= 16) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when {
        score >= 4 -> "Strong" to VaultColors.Accent
        score >= 2 -> "Moderate" to VaultColors.WarningAmber
        else -> "Weak" to VaultColors.ErrorRed
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
