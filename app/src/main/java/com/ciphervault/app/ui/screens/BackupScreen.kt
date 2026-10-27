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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.ciphervault.app.ui.theme.SuccessGreen
import com.ciphervault.app.ui.theme.TextPrimary
import com.ciphervault.app.ui.theme.TextSecondary
import com.ciphervault.app.ui.theme.TextTertiary
import com.ciphervault.app.ui.theme.VaultGreen
import com.ciphervault.app.ui.theme.VaultGreenGlow
import com.ciphervault.app.ui.theme.VaultGreenSubtle
import com.ciphervault.app.ui.theme.WarningAmber
import com.ciphervault.app.ui.theme.WarningSubtle
import com.ciphervault.app.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern, polished Backup & 2FA Security screen.
 * Offers direct export to phone storage, app sharing, and offline TOTP enrollment.
 */
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

    LaunchedEffect(Unit) {
        backupViewModel.loadBackupStats()
    }

    var verificationCode by remember { mutableStateOf("") }
    var copiedSecretNotice by remember { mutableStateOf(false) }
    var copiedCodesNotice by remember { mutableStateOf(false) }

    // Launcher for exporting directly to phone storage
    val fileSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destinationUri ->
        destinationUri?.let { uri ->
            backupViewModel.exportToFile(uri)
        }
    }

    // Share sheet handler when shareable backup URI is generated
    backupUri?.let { uri ->
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share CipherVault Backup"))
        backupViewModel.clearBackupUri()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // ── Top Bar ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, CardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Backup & 2FA",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cross-device migration & authentication",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 2FA Security Status Hero Card ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isTotpConfigured) VaultGreen.copy(alpha = 0.4f) else WarningAmber.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(18.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isTotpConfigured) VaultGreenSubtle else DarkCard
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isTotpConfigured) VaultGreen.copy(alpha = 0.2f)
                                else WarningAmber.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isTotpConfigured) "2FA PROTECTED" else "SETUP REQUIRED",
                            color = if (isTotpConfigured) VaultGreen else WarningAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Icon(
                        imageVector = if (isTotpConfigured) Icons.Filled.CheckCircle else Icons.Filled.Security,
                        contentDescription = null,
                        tint = if (isTotpConfigured) VaultGreen else WarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isTotpConfigured) "Google Authenticator 2FA Active"
                    else "Two-Factor Authentication Inactive",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isTotpConfigured) {
                        "Your vault backups are protected by offline RFC 6238 TOTP. Importing your data on any device will require your Authenticator code."
                    } else {
                        "To protect your vault backups against unauthorized restore, you must first enroll your offline 2FA secret key with Google Authenticator."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ═════════════════════════════════════════════════
        //  FLOW 1: 2FA NOT YET CONFIGURED (ENROLLMENT)
        // ═════════════════════════════════════════════════
        if (!isTotpConfigured) {
            if (qrBitmap == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(18.dp)
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
                                .clip(CircleShape)
                                .background(CyanSubtle)
                                .border(1.dp, CyanAccent.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Enroll in Google Authenticator",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Generates a 160-bit cryptographically secure Base32 secret. 100% offline — your keys never leave this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { backupViewModel.setupTotp() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Generate 2FA QR Code", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            } else {
                // QR Display & 6-digit confirmation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scan with Google Authenticator",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Open Google Authenticator > Tap + > Scan QR code",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // QR Code container
                        Box(
                            modifier = Modifier
                                .size(230.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(3.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "TOTP QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Manual URI copy
                        otpAuthUri?.let { uri ->
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("TOTP Secret URI", uri))
                                    copiedSecretNotice = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = CyanAccent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (copiedSecretNotice) "URI Copied to Clipboard!" else "Copy Secret URI Manually",
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Enter 6-Digit Authenticator Code",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = {
                                verificationCode = it.filter { c -> c.isDigit() }.take(6)
                                backupViewModel.clearMessage()
                            },
                            placeholder = { Text("000000", color = TextTertiary, textAlign = TextAlign.Center) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = CardBorder,
                                cursorColor = CyanAccent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            ),
                            modifier = Modifier.width(180.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { backupViewModel.verifyTotpSetup(verificationCode) },
                            enabled = verificationCode.length == 6,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Confirm & Activate 2FA", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // ═════════════════════════════════════════════════
        //  FLOW 2: 2FA ACTIVE (EXPORT TO STORAGE & SHARE)
        // ═════════════════════════════════════════════════
        if (isTotpConfigured) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(VaultGreenSubtle)
                                .border(1.dp, VaultGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = VaultGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Export Vault Backup",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Encrypted .cvault archive",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Your backup includes all encrypted files and your portable 2FA credentials. It can be safely saved to your phone storage or shared with other devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Compression & Storage Comparison Card ──
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyanAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Title + Deflater Level 9 Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Storage,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Compression & Optimization",
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
                                        text = "DEFLATE MAX (LVL 9)",
                                        color = CyanAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Side-by-side comparison: Original Raw vs Compressed Package
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Left Box: Original Vault Payload
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkBackground)
                                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "ORIGINAL RAW",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatFileSize(backupStats?.rawVaultSizeBytes ?: 0L),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${backupStats?.fileCount ?: 0} file(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                // Arrow
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = "compressed to",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )

                                // Right Box: Compressed .cvault Archive
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(VaultGreenSubtle)
                                        .border(1.dp, VaultGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "COMPRESSED BACKUP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VaultGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatFileSize(backupStats?.compressedBackupSizeBytes ?: 0L),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = VaultGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val savings = backupStats?.savingsPercentage ?: 0
                                    Text(
                                        text = if (savings > 0) "$savings% space saved" else "AES-GCM Optimized",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VaultGreen.copy(alpha = 0.85f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            val spaceSaved = backupStats?.spaceSavedBytes ?: 0L
                            if (spaceSaved > 0L) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = VaultGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reduces backup size by ${formatFileSize(spaceSaved)} using DEFLATE level 9 and compact manifest encoding.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VaultGreen,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── ACTION 1: Save Directly to Phone Files ──
                    Button(
                        onClick = {
                            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            val defaultName = "CipherVault_Backup_${sdf.format(Date())}.cvault"
                            fileSaveLauncher.launch(defaultName)
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Exporting Backup...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Save to Phone Files", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── ACTION 2: Share via System Share Sheet ──
                    OutlinedButton(
                        onClick = { backupViewModel.createShareBackup() },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Archive via Apps...", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Recovery Codes Section
            if (recoveryCodes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarningAmber.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Emergency Recovery Codes",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarningAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Save these single-use recovery codes in a secure place. If you ever lose your phone or Google Authenticator app, you can use one code to import your vault.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Monospace codes container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBackground)
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            recoveryCodes.chunked(2).forEachIndexed { rowIndex, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    row.forEachIndexed { colIndex, code ->
                                        val codeIndex = rowIndex * 2 + colIndex + 1
                                        Text(
                                            text = "$codeIndex. $code",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val text = recoveryCodes.joinToString("\n")
                                clipboard.setPrimaryClip(ClipData.newPlainText("Recovery Codes", text))
                                copiedCodesNotice = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = WarningAmber
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (copiedCodesNotice) "Codes Copied to Clipboard!" else "Copy All Recovery Codes",
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Notification Banner
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val isSuccess = message?.contains("success", ignoreCase = true) == true ||
                    message?.contains("ready", ignoreCase = true) == true
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .border(
                        1.dp,
                        if (isSuccess) VaultGreen.copy(alpha = 0.5f) else ErrorRed.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuccess) VaultGreenSubtle else ErrorSubtle
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Security,
                        contentDescription = null,
                        tint = if (isSuccess) VaultGreen else ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = message ?: "",
                        color = if (isSuccess) VaultGreen else ErrorRed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
