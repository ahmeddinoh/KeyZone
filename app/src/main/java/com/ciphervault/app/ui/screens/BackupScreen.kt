package com.ciphervault.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.ciphervault.app.CipherVaultApp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(Unit) {
        backupViewModel.loadBackupStats()
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
            context.startActivity(Intent.createChooser(shareIntent, "Share KeyZone Backup"))
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
        // ── Top Bar & Editorial Headline ──
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
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = VaultColors.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Live 2FA Posture Pill
                    Surface(
                        shape = CircleShape,
                        color = VaultColors.Surface,
                        border = BorderStroke(1.dp, VaultColors.Hairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
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
                                text = if (isTotpConfigured) "RFC 6238 TOTP" else "2FA Unconfigured",
                                style = VaultType.Meta,
                                color = VaultColors.TextSecondary
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Portable vault &",
                        style = VaultType.PageTitle,
                        color = VaultColors.TextPrimary
                    )
                    Text(
                        text = "2FA security.",
                        style = VaultType.PageTitle.copy(fontStyle = FontStyle.Italic),
                        color = VaultColors.Accent
                    )
                }
            }
        }

        // ── 2FA Security Posture Hero Card ──
        item {
            val statusTint = if (isTotpConfigured) VaultColors.Accent else VaultColors.WarningAmber
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    1.dp,
                    if (isTotpConfigured) VaultColors.Hairline else VaultColors.WarningAmber.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    statusTint.copy(alpha = 0.14f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusTint.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isTotpConfigured) "2FA PROTECTED" else "SETUP REQUIRED",
                                style = VaultType.Meta,
                                color = statusTint,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(statusTint.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isTotpConfigured) Icons.Outlined.VerifiedUser else Icons.Outlined.Security,
                                contentDescription = null,
                                tint = statusTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isTotpConfigured) "Authenticator App Active"
                        else "Two-Factor Authentication Inactive",
                        style = VaultType.SectionTitle,
                        color = VaultColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isTotpConfigured) {
                            "Your vault backups are sealed with offline RFC 6238 TOTP. Restoring a backup on any device requires your 6-digit Authenticator code."
                        } else {
                            "To protect your exported archives against unauthorized restore, enroll an offline 2FA secret key with an Authenticator App first."
                        },
                        style = VaultType.Meta,
                        color = VaultColors.TextSecondary,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // ═════════════════════════════════════════════════
        //  FLOW 1: 2FA ENROLLMENT
        // ═════════════════════════════════════════════════
        if (!isTotpConfigured) {
            item {
                if (qrBitmap == null) {
                    // Step 1: Generate Secret Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, VaultColors.Hairline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(VaultColors.AccentTint)
                                    .border(1.dp, VaultColors.Hairline, RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode2,
                                    contentDescription = null,
                                    tint = VaultColors.Accent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Enroll Authenticator App",
                                style = VaultType.SectionTitle,
                                color = VaultColors.TextPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Generates a 160-bit cryptographically secure Base32 secret. 100% air-gapped — your keys never leave this device.",
                                style = VaultType.Meta,
                                color = VaultColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { backupViewModel.setupTotp() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VaultColors.Accent,
                                    contentColor = VaultColors.OnAccent
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "Generate 2FA QR Code",
                                    style = VaultType.ItemTitle,
                                    color = VaultColors.OnAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Step 2: Scan QR & Verify 6-Digit Code
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, VaultColors.Hairline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Scan with Authenticator",
                                style = VaultType.SectionTitle,
                                color = VaultColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Open an Authenticator app · Tap + · Scan QR",
                                style = VaultType.Meta,
                                color = VaultColors.TextSecondary
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            // Framed QR Code Container
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
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "TOTP QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Manual Secret URI Copy Pill
                            otpAuthUri?.let { uri ->
                                Surface(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("TOTP Secret URI", uri))
                                        copiedSecretNotice = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = VaultColors.Ground,
                                    border = BorderStroke(1.dp, VaultColors.Hairline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (copiedSecretNotice) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = VaultColors.Accent
                                        )
                                        Text(
                                            text = if (copiedSecretNotice) "Secret URI copied" else "Copy setup URI manually",
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
                                text = "ENTER 6-DIGIT VERIFICATION CODE",
                                style = VaultType.GroupLabel,
                                color = VaultColors.TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 6-Cell Cryptographic OTP Input
                            OtpSixDigitInput(
                                code = verificationCode,
                                onCodeChange = {
                                    verificationCode = it
                                    backupViewModel.clearMessage()
                                }
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Button(
                                onClick = { backupViewModel.verifyTotpSetup(verificationCode) },
                                enabled = verificationCode.length == 6,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VaultColors.Accent,
                                    contentColor = VaultColors.OnAccent,
                                    disabledContainerColor = VaultColors.SurfaceRaised,
                                    disabledContentColor = VaultColors.TextSecondary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "Confirm & Activate 2FA",
                                    style = VaultType.ItemTitle,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ═════════════════════════════════════════════════
        //  FLOW 2: 2FA ACTIVE (EXPORT & RECOVERY CODES)
        // ═════════════════════════════════════════════════
        if (isTotpConfigured) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PORTABLE ARCHIVE EXPORT",
                        style = VaultType.GroupLabel,
                        color = VaultColors.TextSecondary,
                        modifier = Modifier.padding(start = 14.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, VaultColors.Hairline)
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            // Card Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(VaultColors.AccentTint)
                                        .border(1.dp, VaultColors.Hairline, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Shield,
                                        contentDescription = null,
                                        tint = VaultColors.Accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Export Vault Backup",
                                        style = VaultType.ItemTitle,
                                        color = VaultColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Encrypted .cvault container",
                                        style = VaultType.Meta,
                                        color = VaultColors.TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Bundles all encrypted files and your portable 2FA credentials into a single archive for local storage or cross-device transfer.",
                                style = VaultType.Meta,
                                color = VaultColors.TextSecondary,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // ── Backup Details ──
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(VaultColors.Ground)
                                    .border(1.dp, VaultColors.Hairline, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Storage,
                                            contentDescription = null,
                                            tint = VaultColors.Accent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Backup Package Size",
                                            style = VaultType.Meta,
                                            color = VaultColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(VaultColors.AccentTint)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "AES-GCM",
                                            style = VaultType.Meta,
                                            color = VaultColors.Accent,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Backup Output Box
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(VaultColors.Surface)
                                            .border(1.dp, VaultColors.Hairline, RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = ".CVAULT ARCHIVE",
                                            style = VaultType.Meta,
                                            color = VaultColors.TextSecondary,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatFileSize(backupStats?.backupSizeBytes ?: 0L),
                                            style = VaultType.ItemTitle,
                                            color = VaultColors.TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val count = backupStats?.fileCount ?: 0
                                        Text(
                                            text = "$count ${if (count == 1) "file" else "files"}",
                                            style = VaultType.Meta,
                                            color = VaultColors.TextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Primary CTA: Save to Device Storage
                            Button(
                                onClick = {
                                    (context.applicationContext as? CipherVaultApp)?.ignoreNextBackgroundLock = true
                                    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                    val defaultName = "KeyZone_Backup_${sdf.format(Date())}.cvault"
                                    fileSaveLauncher.launch(defaultName)
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VaultColors.Accent,
                                    contentColor = VaultColors.OnAccent
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = VaultColors.OnAccent,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Packaging Archive...",
                                        style = VaultType.ItemTitle,
                                        color = VaultColors.OnAccent
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Save to Phone Files",
                                        style = VaultType.ItemTitle,
                                        color = VaultColors.OnAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Secondary CTA: System Share Sheet
                            OutlinedButton(
                                onClick = { backupViewModel.createShareBackup() },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, VaultColors.Hairline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    tint = VaultColors.Accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Share Archive via Apps...",
                                    style = VaultType.ItemTitle,
                                    color = VaultColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // ── Emergency Recovery Codes Card ──
            if (recoveryCodes.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "EMERGENCY FAILSAFE",
                            style = VaultType.GroupLabel,
                            color = VaultColors.TextSecondary,
                            modifier = Modifier.padding(start = 14.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, VaultColors.WarningAmber.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(22.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(VaultColors.WarningAmber.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Key,
                                            contentDescription = null,
                                            tint = VaultColors.WarningAmber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Recovery Codes",
                                            style = VaultType.ItemTitle,
                                            color = VaultColors.TextPrimary
                                        )
                                        Text(
                                            text = "Single-use 2FA bypass tokens",
                                            style = VaultType.Meta,
                                            color = VaultColors.WarningAmber
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Store these offline in a safe place. If you lose your phone or Authenticator app, each code can unlock a backup restore once.",
                                    style = VaultType.Meta,
                                    color = VaultColors.TextSecondary,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 2-Column Pill Matrix for Recovery Codes
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    recoveryCodes.chunked(2).forEachIndexed { rowIndex, row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            row.forEachIndexed { colIndex, code ->
                                                val codeNum = (rowIndex * 2 + colIndex + 1).toString().padStart(2, '0')
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(VaultColors.Ground)
                                                        .border(1.dp, VaultColors.Hairline, RoundedCornerShape(10.dp))
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = codeNum,
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
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val text = recoveryCodes.joinToString("\n")
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Recovery Codes", text))
                                        copiedCodesNotice = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, VaultColors.Hairline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    Icon(
                                        imageVector = if (copiedCodesNotice) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = VaultColors.WarningAmber
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (copiedCodesNotice) "Copied to Clipboard" else "Copy All Recovery Codes",
                                        style = VaultType.Meta,
                                        color = VaultColors.TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                TextButton(
                    onClick = { backupViewModel.disableTotp() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cancel,
                        contentDescription = null,
                        tint = VaultColors.ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Disable 2FA & Disconnect Authenticator",
                        style = VaultType.ItemTitle,
                        color = VaultColors.ErrorRed
                    )
                }
            }
        }

        // ── Status Toast Banner ──
        item {
            AnimatedVisibility(
                visible = message != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val isSuccess = message?.contains("success", ignoreCase = true) == true ||
                        message?.contains("ready", ignoreCase = true) == true ||
                        message?.contains("active", ignoreCase = true) == true
                val statusColor = if (isSuccess) VaultColors.SuccessGreen else VaultColors.ErrorRed

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultColors.SurfaceRaised),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
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
                            text = message ?: "",
                            color = VaultColors.TextPrimary,
                            style = VaultType.Meta,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpSixDigitInput(
    code: String,
    onCodeChange: (String) -> Unit
) {
    BasicTextField(
        value = code,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() }.take(6)
            onCodeChange(filtered)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(VaultColors.Accent),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val char = code.getOrNull(index)?.toString() ?: ""
                    val isFocused = code.length == index || (code.length == 6 && index == 5)
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VaultColors.Ground)
                            .border(
                                width = if (isFocused) 1.5.dp else 1.dp,
                                color = if (isFocused) VaultColors.Accent
                                else if (char.isNotEmpty()) VaultColors.Accent.copy(alpha = 0.45f)
                                else VaultColors.Hairline,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = VaultColors.TextPrimary
                        )
                    }
                }
            }
        }
    )
}
