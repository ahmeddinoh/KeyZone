package com.ciphervault.app.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.ui.components.rememberNavPillClearance
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.AuthViewModel
import com.ciphervault.app.viewmodel.VaultViewModel
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    vaultViewModel: VaultViewModel? = null,
    onNavigateToBackup: () -> Unit = {},
    onNavigateToChangeKey: () -> Unit = {},
    onNavigateToStorageInsights: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as CipherVaultApp
    val vaultConfig = app.vaultConfig

    var showChangeKeyDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showWipeVaultDialog by remember { mutableStateOf(false) }

    val errorMessage by authViewModel.errorMessage.collectAsState()

    var biometricEnabled by remember { mutableStateOf(vaultConfig.isBiometricEnabled) }
    var autoLockEnabled by remember { mutableStateOf(vaultConfig.autoLockOnBackground) }
    var screenProtectionEnabled by remember { mutableStateOf(vaultConfig.screenProtectionEnabled) }
    var deleteOriginalEnabled by remember { mutableStateOf(vaultConfig.deleteOriginalAfterEncryption) }

    // Calculate active shields for the top posture indicator
    val totalShields = if (authViewModel.isBiometricAvailable) 3 else 2
    val activeShields = (if (biometricEnabled && authViewModel.isBiometricAvailable) 1 else 0) +
            (if (autoLockEnabled) 1 else 0) +
            (if (screenProtectionEnabled) 1 else 0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 22.dp,
            end = 22.dp,
            top = 56.dp,
            bottom = rememberNavPillClearance() + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // ── Top Bar & Editorial Header ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Security Posture Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (activeShields == totalShields) VaultColors.Accent
                                    else VaultColors.WarningAmber
                                )
                        )
                        Text(
                            text = "$activeShields of $totalShields shields active",
                            style = VaultType.Meta,
                            color = VaultColors.TextSecondary
                        )
                    }

                    // Quick Lock Action Pill
                    Surface(
                        onClick = { authViewModel.lock() },
                        shape = CircleShape,
                        color = VaultColors.Surface,
                        border = BorderStroke(1.dp, VaultColors.Hairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Lock Vault Now",
                                tint = VaultColors.WarningAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Lock now",
                                style = VaultType.Meta,
                                color = VaultColors.TextPrimary
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Manage your",
                        style = VaultType.PageTitle,
                        color = VaultColors.TextPrimary
                    )
                    Text(
                        text = "vault settings.",
                        style = VaultType.PageTitle.copy(fontStyle = FontStyle.Italic),
                        color = VaultColors.Accent
                    )
                }
            }
        }

        // ══════════════════════════════════════════════
        //  SECTION 1: Security & Access Control
        // ══════════════════════════════════════════════
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("SECURITY & ACCESS CONTROL")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, VaultColors.Hairline)
                ) {
                    Column {
                        if (authViewModel.isBiometricAvailable) {
                            SettingsToggleRow(
                                icon = Icons.Outlined.Fingerprint,
                                label = "Biometric Unlock",
                                subtitle = "Use fingerprint or face hardware to unlock",
                                checked = biometricEnabled,
                                onCheckedChange = { checked ->
                                    if (authViewModel.setBiometricEnabled(checked)) {
                                        biometricEnabled = checked
                                    }
                                }
                            )
                            InsetDivider()
                        }

                        SettingsToggleRow(
                            icon = Icons.Outlined.Timer,
                            label = "Auto-Lock on Exit",
                            subtitle = "Immediately seal vault when app goes to background",
                            checked = autoLockEnabled,
                            onCheckedChange = { checked ->
                                autoLockEnabled = checked
                                vaultConfig.autoLockOnBackground = checked
                            }
                        )
                        InsetDivider()

                        SettingsToggleRow(
                            icon = Icons.Outlined.VisibilityOff,
                            label = "Anti-Screenshot",
                            subtitle = "Block screen capture and recent-apps previews",
                            checked = screenProtectionEnabled,
                            onCheckedChange = { checked ->
                                screenProtectionEnabled = checked
                                vaultConfig.screenProtectionEnabled = checked
                                (context as? Activity)?.let { activity ->
                                    if (checked) {
                                        activity.window.setFlags(
                                            WindowManager.LayoutParams.FLAG_SECURE,
                                            WindowManager.LayoutParams.FLAG_SECURE
                                        )
                                    } else {
                                        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                    }
                                }
                            }
                        )
                        InsetDivider()

                        SettingsValueRow(
                            icon = Icons.Outlined.CloudUpload,
                            label = "Backup & 2FA",
                            subtitle = "Encrypted snapshots & recovery verification",
                            onClick = onNavigateToBackup,
                            showChevron = true
                        )
                        InsetDivider()

                        val keyLabel = if (authViewModel.keyType == "pin") "PIN" else "Password"
                        SettingsValueRow(
                            icon = Icons.Outlined.LockReset,
                            label = "Change $keyLabel",
                            subtitle = "Re-wrap master key with a new $keyLabel",
                            badgeText = keyLabel.uppercase(),
                            onClick = onNavigateToChangeKey,
                            showChevron = true
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════
        //  SECTION 2: Storage & Data Management
        // ══════════════════════════════════════════════
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("STORAGE & DATA")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, VaultColors.Hairline)
                ) {
                    Column {
                        SettingsValueRow(
                            icon = Icons.Outlined.PieChart,
                            label = "Storage Insights",
                            subtitle = "View storage distribution and largest files",
                            onClick = onNavigateToStorageInsights,
                            showChevron = true
                        )

                        HorizontalDivider(color = VaultColors.Hairline)

                        SettingsToggleRow(
                            icon = Icons.Outlined.DeleteSweep,
                            label = "Shred Source by Default",
                            subtitle = "Delete original unencrypted file after import",
                            checked = deleteOriginalEnabled,
                            onCheckedChange = { checked ->
                                deleteOriginalEnabled = checked
                                vaultConfig.deleteOriginalAfterEncryption = checked
                            }
                        )
                        InsetDivider()

                        SettingsValueRow(
                            icon = Icons.Outlined.ClearAll,
                            label = "Clear Decrypted Cache",
                            subtitle = "Purge temporary viewing files from local memory",
                            onClick = { showClearCacheDialog = true },
                            showChevron = false
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════
        //  SECTION 3: Danger Zone
        // ══════════════════════════════════════════════
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("DANGER ZONE")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultColors.Surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, VaultColors.Hairline)
                ) {
                    Column {
                        SettingsValueRow(
                            icon = Icons.Outlined.Lock,
                            label = "Lock Vault Now",
                            subtitle = "Clear active session keys and return to lockscreen",
                            onClick = { authViewModel.lock() },
                            showChevron = false,
                            iconTint = VaultColors.WarningAmber,
                            textTint = VaultColors.WarningAmber
                        )
                        InsetDivider()
                        SettingsValueRow(
                            icon = Icons.Outlined.DeleteForever,
                            label = "Reset & Destroy Vault",
                            subtitle = "Permanently wipe all encrypted files and master keys",
                            onClick = { showWipeVaultDialog = true },
                            showChevron = false,
                            iconTint = VaultColors.ErrorRed,
                            textTint = VaultColors.ErrorRed
                        )
                    }
                }
            }
        }
    }

    // ── Modals & Dialogs ──

    if (showChangeKeyDialog) {
        ChangeKeyDialog(
            keyType = authViewModel.keyType,
            onDismiss = {
                showChangeKeyDialog = false
                authViewModel.clearError()
            },
            onConfirm = { currentKey, newKey ->
                if (authViewModel.changeKey(currentKey, authViewModel.keyType, newKey)) {
                    showChangeKeyDialog = false
                }
            },
            errorMessage = errorMessage
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = VaultColors.SurfaceRaised,
            titleContentColor = VaultColors.TextPrimary,
            textContentColor = VaultColors.TextSecondary,
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VaultColors.WarningAmber.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ClearAll,
                        contentDescription = null,
                        tint = VaultColors.WarningAmber
                    )
                }
            },
            title = { Text("Clear Decrypted Cache?", style = VaultType.SectionTitle) },
            text = {
                Text(
                    "This removes all temporarily decrypted preview files from device cache. Your encrypted vault items remain untouched.",
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vaultViewModel?.clearCache()
                        showClearCacheDialog = false
                    }
                ) {
                    Text("Clear Cache", color = VaultColors.WarningAmber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = VaultColors.TextSecondary)
                }
            }
        )
    }

    if (showWipeVaultDialog) {
        var countdown by remember { mutableStateOf(5) }
        LaunchedEffect(Unit) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }

        AlertDialog(
            onDismissRequest = { showWipeVaultDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = VaultColors.SurfaceRaised,
            titleContentColor = VaultColors.ErrorRed,
            textContentColor = VaultColors.TextSecondary,
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VaultColors.ErrorRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = VaultColors.ErrorRed
                    )
                }
            },
            title = { Text("Destroy Vault?", style = VaultType.SectionTitle) },
            text = {
                Text(
                    "WARNING: This permanently destroys ALL encrypted files, Master Keys, biometric credentials, and database records. This cannot be undone.",
                    style = VaultType.Meta,
                    color = VaultColors.TextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipeVaultDialog = false
                        authViewModel.wipeVaultAndReset()
                    },
                    enabled = countdown == 0
                ) {
                    Text(
                        text = if (countdown > 0) "Destroy Vault (${countdown}s)" else "Destroy Vault",
                        color = if (countdown > 0) VaultColors.TextSecondary else VaultColors.ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeVaultDialog = false }) {
                    Text("Cancel", color = VaultColors.TextSecondary)
                }
            }
        )
    }
}

// ── Reusable Row & Section Components ──

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = VaultType.GroupLabel,
        color = VaultColors.TextSecondary,
        modifier = Modifier.padding(start = 14.dp)
    )
}

@Composable
private fun InsetDivider() {
    HorizontalDivider(
        color = VaultColors.Hairline,
        thickness = 1.dp,
        modifier = Modifier.padding(start = 68.dp, end = 16.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(VaultColors.AccentTint)
                .border(1.dp, VaultColors.Hairline, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VaultColors.Accent,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = VaultType.ItemTitle,
                color = VaultColors.TextPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VaultColors.OnAccent,
                checkedTrackColor = VaultColors.Accent,
                uncheckedThumbColor = VaultColors.TextSecondary,
                uncheckedTrackColor = VaultColors.Ground,
                uncheckedBorderColor = VaultColors.Hairline
            ),
            modifier = Modifier.scale(0.88f)
        )
    }
}

@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    badgeText: String = "",
    onClick: () -> Unit,
    showChevron: Boolean = false,
    iconTint: Color = VaultColors.Accent,
    textTint: Color = VaultColors.TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconTint.copy(alpha = 0.12f))
                .border(1.dp, VaultColors.Hairline, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = VaultType.ItemTitle,
                color = textTint
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary
                )
            }
        }

        if (badgeText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(VaultColors.AccentTint)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    style = VaultType.Meta,
                    color = VaultColors.Accent
                )
            }
        }

        if (showChevron) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = VaultColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ChangeKeyDialog(
    keyType: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    errorMessage: String?
) {
    var currentKey by remember { mutableStateOf("") }
    var newKey by remember { mutableStateOf("") }
    var confirmNewKey by remember { mutableStateOf("") }
    var showKeys by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val label = if (keyType == "pin") "PIN" else "Password"
    val keyboardType = if (keyType == "pin") KeyboardType.NumberPassword else KeyboardType.Password
    val visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VaultColors.Accent,
        unfocusedBorderColor = VaultColors.Hairline,
        focusedTextColor = VaultColors.TextPrimary,
        unfocusedTextColor = VaultColors.TextPrimary,
        cursorColor = VaultColors.Accent
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = VaultColors.SurfaceRaised,
        titleContentColor = VaultColors.TextPrimary,
        textContentColor = VaultColors.TextSecondary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Change $label", style = VaultType.SectionTitle)
                IconButton(
                    onClick = { showKeys = !showKeys },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (showKeys) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = "Toggle visibility",
                        tint = VaultColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentKey,
                    onValueChange = { currentKey = it; localError = null },
                    label = { Text("Current $label", color = VaultColors.TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { newKey = it; localError = null },
                    label = { Text("New $label", color = VaultColors.TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmNewKey,
                    onValueChange = { confirmNewKey = it; localError = null },
                    label = { Text("Confirm New $label", color = VaultColors.TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                val err = localError ?: errorMessage
                if (err != null) {
                    Text(
                        text = err,
                        color = VaultColors.ErrorRed,
                        style = VaultType.Meta
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newKey != confirmNewKey) {
                        localError = "${label}s do not match"
                    } else if (newKey.length < 4) {
                        localError = "$label must be at least 4 characters"
                    } else {
                        localError = null
                        onConfirm(currentKey, newKey)
                    }
                }
            ) {
                Text("Update $label", color = VaultColors.Accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultColors.TextSecondary)
            }
        }
    )
}
