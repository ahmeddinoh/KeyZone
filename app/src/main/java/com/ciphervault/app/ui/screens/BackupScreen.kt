package com.ciphervault.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.R
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.BackupViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    backupViewModel: BackupViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isTotpConfigured by backupViewModel.isTotpConfigured.collectAsState()
    val qrBitmap by backupViewModel.qrBitmap.collectAsState()
    val otpAuthUri by backupViewModel.otpAuthUri.collectAsState()
    val recoveryCodes by backupViewModel.recoveryCodes.collectAsState()
    val isLoading by backupViewModel.isLoading.collectAsState()
    val message by backupViewModel.message.collectAsState()
    val backupUri by backupViewModel.backupUri.collectAsState()
    val backupStats by backupViewModel.backupStats.collectAsState()

    var verificationCode by remember { mutableStateOf("") }
    var copiedSecretNotice by remember { mutableStateOf(false) }
    var copiedCodesNotice by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }

    // Keeps the last message on screen while the banner fades out.
    var shownMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        backupViewModel.loadBackupStats()
    }

    // The banner floats at the top, so it can't be missed; it clears itself after a few seconds.
    LaunchedEffect(message) {
        message?.let {
            shownMessage = it
            delay(4500)
            backupViewModel.clearMessage()
        }
    }

    // Auto-reset copy notices after 2.5s for clean feedback
    LaunchedEffect(copiedSecretNotice) {
        if (copiedSecretNotice) {
            delay(2500)
            copiedSecretNotice = false
        }
    }
    LaunchedEffect(copiedCodesNotice) {
        if (copiedCodesNotice) {
            delay(2500)
            copiedCodesNotice = false
        }
    }

    // Safe side-effect handler for launching system Share Sheet
    LaunchedEffect(backupUri) {
        backupUri?.let { uri ->
            (context.applicationContext as? CipherVaultApp)?.ignoreNextBackgroundLock = true
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.backup_share_title)))
            backupViewModel.clearBackupUri()
        }
    }

    val fileSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destinationUri ->
        destinationUri?.let { uri ->
            backupViewModel.exportToFile(uri)
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Ground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = topInset + 16.dp,
                bottom = bottomInset + 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // ---- Back, live 2FA status, headline ----
            item {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onBack,
                            shape = CircleShape,
                            color = VaultColors.Surface,
                            border = BorderStroke(1.dp, VaultColors.Hairline),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                    tint = VaultColors.TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = VaultColors.Surface,
                            border = BorderStroke(1.dp, VaultColors.Hairline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isTotpConfigured) VaultColors.Accent
                                            else VaultColors.WarningAmber
                                        )
                                )
                                Text(
                                    text = if (isTotpConfigured) stringResource(R.string.backup_totp_configured)
                                    else stringResource(R.string.backup_totp_unconfigured),
                                    style = VaultType.Meta,
                                    color = VaultColors.TextSecondary
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.backup_portable_vault),
                            style = VaultType.PageTitle,
                            color = VaultColors.TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.backup_2fa_security),
                            style = VaultType.PageTitle.copy(fontStyle = FontStyle.Italic),
                            color = VaultColors.Accent
                        )
                    }
                }
            }

            // ---- Export: the main job of this screen, so it comes first ----
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.backup_export_header))

                    ExportCard(
                        isTotpConfigured = isTotpConfigured,
                        sizeBytes = backupStats?.backupSizeBytes ?: 0L,
                        fileCount = (backupStats?.fileCount ?: 0).toLong(),
                        isLoading = isLoading,
                        onSaveToPhone = {
                            (context.applicationContext as? CipherVaultApp)?.ignoreNextBackgroundLock = true
                            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            fileSaveLauncher.launch("KeyZone_Backup_${sdf.format(Date())}.cvault")
                        },
                        onShare = { backupViewModel.createShareBackup() }
                    )
                }
            }

            // ---- Two-factor: status, enrolment, and turning it off live in one card ----
            item {
                TwoFactorCard(
                    isConfigured = isTotpConfigured,
                    qrBitmap = qrBitmap,
                    otpAuthUri = otpAuthUri,
                    verificationCode = verificationCode,
                    onCodeChange = {
                        verificationCode = it
                        backupViewModel.clearMessage()
                    },
                    copiedSecret = copiedSecretNotice,
                    onCopySecret = { uri ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.backup_totp_uri), uri))
                        copiedSecretNotice = true
                    },
                    onGenerate = { backupViewModel.setupTotp() },
                    onVerify = { backupViewModel.verifyTotpSetup(verificationCode) },
                    onDisable = { showDisableDialog = true }
                )
            }

            // ---- Recovery codes ----
            if (isTotpConfigured && recoveryCodes.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(stringResource(R.string.backup_emergency_header))

                        RecoveryCodesCard(
                            codes = recoveryCodes,
                            copied = copiedCodesNotice,
                            onCopyAll = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val text = recoveryCodes.joinToString("\n")
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(context.getString(R.string.backup_recovery_codes), text)
                                )
                                copiedCodesNotice = true
                            }
                        )
                    }
                }
            }
        }

        // ---- Status banner: floats at the top so feedback is always in view ----
        AnimatedVisibility(
            visible = message != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            val isSuccess = shownMessage.contains("success", ignoreCase = true) ||
                    shownMessage.contains("ready", ignoreCase = true) ||
                    shownMessage.contains("active", ignoreCase = true)
            StatusBanner(text = shownMessage, isSuccess = isSuccess)
        }
    }

    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = VaultColors.SurfaceRaised,
            icon = {
                IconTile(
                    icon = Icons.Outlined.Cancel,
                    size = 52.dp,
                    iconSize = 26.dp,
                    tint = VaultColors.ErrorRed,
                    background = VaultColors.ErrorRed.copy(alpha = 0.12f),
                    shape = CircleShape
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.backup_disable_2fa),
                    style = VaultType.ItemTitle.copy(fontSize = 18.sp),
                    color = VaultColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisableDialog = false
                        backupViewModel.disableTotp()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultColors.ErrorRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.backup_disable_2fa), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }) {
                    Text(stringResource(android.R.string.cancel), color = VaultColors.TextSecondary)
                }
            }
        )
    }
}

// =====================================================================
//  Cards
// =====================================================================

@Composable
private fun ExportCard(
    isTotpConfigured: Boolean,
    sizeBytes: Long,
    fileCount: Long,
    isLoading: Boolean,
    onSaveToPhone: () -> Unit,
    onShare: () -> Unit
) {
    BackupCard {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(icon = Icons.Outlined.Shield)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.backup_export_title),
                        style = VaultType.ItemTitle.copy(fontSize = 16.sp),
                        color = VaultColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.backup_export_subtitle),
                        style = VaultType.Meta,
                        color = VaultColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isTotpConfigured) stringResource(R.string.backup_export_desc_2fa)
                else stringResource(R.string.backup_export_desc_no2fa),
                style = VaultType.Meta.copy(lineHeight = 19.sp),
                color = VaultColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // What will be in the archive
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = stringResource(R.string.backup_package_size),
                    value = formatFileSize(sizeBytes),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = if (fileCount == 1L) stringResource(R.string.home_file) else stringResource(R.string.home_files),
                    value = fileCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(text = ".cvault")
                Chip(text = stringResource(R.string.backup_aes_gcm))
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onSaveToPhone,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultColors.Accent,
                    contentColor = VaultColors.OnAccent,
                    disabledContainerColor = VaultColors.Accent.copy(alpha = 0.4f),
                    disabledContentColor = VaultColors.OnAccent.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = VaultColors.OnAccent,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.backup_packaging),
                        style = VaultType.Button.copy(fontSize = 16.sp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.backup_save_phone),
                        style = VaultType.Button.copy(fontSize = 16.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onShare,
                enabled = !isLoading,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, VaultColors.Hairline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    tint = VaultColors.Accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.backup_share_apps),
                    style = VaultType.ItemTitle,
                    color = VaultColors.TextPrimary
                )
            }
        }
    }
}

/**
 * One card for everything two-factor: whether it's on, the two enrolment steps while it isn't,
 * and the way to turn it off once it is.
 */
@Composable
private fun TwoFactorCard(
    isConfigured: Boolean,
    qrBitmap: android.graphics.Bitmap?,
    otpAuthUri: String?,
    verificationCode: String,
    onCodeChange: (String) -> Unit,
    copiedSecret: Boolean,
    onCopySecret: (String) -> Unit,
    onGenerate: () -> Unit,
    onVerify: () -> Unit,
    onDisable: () -> Unit
) {
    val statusTint = if (isConfigured) VaultColors.Accent else VaultColors.WarningAmber

    BackupCard(
        borderColor = if (isConfigured) VaultColors.Hairline else VaultColors.WarningAmber.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Status
            Row(verticalAlignment = Alignment.Top) {
                IconTile(
                    icon = if (isConfigured) Icons.Outlined.VerifiedUser else Icons.Outlined.Security,
                    tint = statusTint,
                    background = statusTint.copy(alpha = 0.14f)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isConfigured) stringResource(R.string.backup_auth_active)
                        else stringResource(R.string.backup_auth_inactive),
                        style = VaultType.ItemTitle.copy(fontSize = 16.sp),
                        color = VaultColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isConfigured) stringResource(R.string.backup_desc_configured)
                        else stringResource(R.string.backup_desc_unconfigured),
                        style = VaultType.Meta.copy(lineHeight = 19.sp),
                        color = VaultColors.TextSecondary
                    )
                }
            }

            if (!isConfigured) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = VaultColors.Hairline)
                Spacer(modifier = Modifier.height(20.dp))

                if (qrBitmap == null) {
                    // Step 1: start enrolment
                    Text(
                        text = stringResource(R.string.backup_enroll_auth),
                        style = VaultType.ItemTitle,
                        color = VaultColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.backup_enroll_desc),
                        style = VaultType.Meta.copy(lineHeight = 19.sp),
                        color = VaultColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onGenerate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultColors.Accent,
                            contentColor = VaultColors.OnAccent
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.backup_generate_qr),
                            style = VaultType.Button.copy(fontSize = 16.sp)
                        )
                    }
                } else {
                    // Step 2: scan, then confirm with a code
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.backup_scan_auth),
                            style = VaultType.ItemTitle,
                            color = VaultColors.TextPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.backup_scan_desc),
                            style = VaultType.Meta.copy(lineHeight = 19.sp),
                            color = VaultColors.TextSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // White frame: scanners need a light quiet zone around the code.
                        Box(
                            modifier = Modifier
                                .size(216.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(2.dp, VaultColors.Accent, RoundedCornerShape(20.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.backup_qr_desc),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        otpAuthUri?.let { uri ->
                            Surface(
                                onClick = { onCopySecret(uri) },
                                shape = CircleShape,
                                color = VaultColors.Ground,
                                border = BorderStroke(1.dp, VaultColors.Hairline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (copiedSecret) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = VaultColors.Accent
                                    )
                                    Text(
                                        text = if (copiedSecret) stringResource(R.string.backup_secret_copied)
                                        else stringResource(R.string.backup_copy_manual),
                                        style = VaultType.Meta,
                                        color = VaultColors.TextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))
                        HorizontalDivider(color = VaultColors.Hairline)
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = stringResource(R.string.backup_enter_code).uppercase(),
                            style = VaultType.GroupLabel,
                            color = VaultColors.TextSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OtpSixDigitInput(code = verificationCode, onCodeChange = onCodeChange)

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = onVerify,
                            enabled = verificationCode.length == 6,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VaultColors.Accent,
                                contentColor = VaultColors.OnAccent,
                                disabledContainerColor = VaultColors.SurfaceRaised,
                                disabledContentColor = VaultColors.TextSecondary
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.backup_confirm_activate),
                                style = VaultType.Button.copy(fontSize = 16.sp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = VaultColors.Hairline)
                TextButton(
                    onClick = onDisable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cancel,
                        contentDescription = null,
                        tint = VaultColors.ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.backup_disable_2fa),
                        style = VaultType.ItemTitle,
                        color = VaultColors.ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun RecoveryCodesCard(
    codes: List<String>,
    copied: Boolean,
    onCopyAll: () -> Unit
) {
    BackupCard(borderColor = VaultColors.WarningAmber.copy(alpha = 0.35f)) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    icon = Icons.Outlined.Key,
                    tint = VaultColors.WarningAmber,
                    background = VaultColors.WarningAmber.copy(alpha = 0.14f)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.backup_recovery_codes),
                        style = VaultType.ItemTitle.copy(fontSize = 16.sp),
                        color = VaultColors.TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.backup_recovery_subtitle),
                        style = VaultType.Meta,
                        color = VaultColors.WarningAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.backup_recovery_desc),
                style = VaultType.Meta.copy(lineHeight = 19.sp),
                color = VaultColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Two columns of numbered codes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                codes.chunked(2).forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEachIndexed { colIndex, code ->
                            val codeNumber = (rowIndex * 2 + colIndex + 1).toString().padStart(2, '0')
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VaultColors.Ground)
                                    .border(1.dp, VaultColors.Hairline, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = codeNumber,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = VaultColors.TextSecondary
                                )
                                Text(
                                    text = code,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = VaultColors.TextPrimary
                                )
                            }
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCopyAll,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, VaultColors.Hairline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = VaultColors.WarningAmber
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (copied) stringResource(R.string.backup_codes_copied)
                    else stringResource(R.string.backup_copy_all_codes),
                    style = VaultType.ItemTitle,
                    color = VaultColors.TextPrimary
                )
            }
        }
    }
}

// =====================================================================
//  Inputs and feedback
// =====================================================================

/**
 * Six boxes that fill the available width. The real text field sits invisibly on top of them,
 * so tapping anywhere on the boxes opens the number keyboard.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun OtpSixDigitInput(
    code: String,
    onCodeChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        BasicTextField(
            value = code,
            onValueChange = { input ->
                onCodeChange(input.filter { it.isDigit() }.take(6))
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(6) { index ->
                            val char = code.getOrNull(index)?.toString() ?: ""
                            val isActive = focused && (code.length == index || (code.length == 6 && index == 5))
                            val shape = RoundedCornerShape(14.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(shape)
                                    .background(VaultColors.Ground)
                                    .border(
                                        width = if (isActive) 1.5.dp else 1.dp,
                                        color = when {
                                            isActive -> VaultColors.Accent
                                            char.isNotEmpty() -> VaultColors.Accent.copy(alpha = 0.45f)
                                            else -> VaultColors.Hairline
                                        },
                                        shape = shape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = VaultColors.TextPrimary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusRequester.requestFocus()
                                keyboard?.show()
                            }
                    ) {
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
private fun StatusBanner(text: String, isSuccess: Boolean) {
    val statusColor = if (isSuccess) VaultColors.SuccessGreen else VaultColors.ErrorRed
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VaultColors.SurfaceRaised)
            .border(1.dp, statusColor.copy(alpha = 0.5f), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = VaultColors.TextPrimary,
            style = VaultType.Meta,
            modifier = Modifier.weight(1f)
        )
    }
}

// =====================================================================
//  Building blocks
// =====================================================================

@Composable
private fun BackupCard(
    modifier: Modifier = Modifier,
    borderColor: Color = VaultColors.Hairline,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = VaultType.GroupLabel,
        color = VaultColors.TextSecondary,
        modifier = Modifier.padding(start = 6.dp)
    )
}

@Composable
private fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = VaultColors.Accent,
    background: Color = VaultColors.AccentTint,
    shape: Shape = RoundedCornerShape(14.dp)
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

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(VaultColors.Ground)
            .border(1.dp, VaultColors.Hairline, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = value,
            style = VaultType.ItemTitle.copy(fontSize = 18.sp),
            color = VaultColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = VaultType.Meta,
            color = VaultColors.TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text = text,
        style = VaultType.Meta.copy(fontSize = 12.sp),
        fontWeight = FontWeight.SemiBold,
        color = VaultColors.Accent,
        modifier = Modifier
            .clip(CircleShape)
            .background(VaultColors.AccentTint)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}