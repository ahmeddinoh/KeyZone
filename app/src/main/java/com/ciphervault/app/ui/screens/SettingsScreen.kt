package com.ciphervault.app.ui.screens

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.ui.theme.BlueAccent
import com.ciphervault.app.ui.theme.CardBorder
import com.ciphervault.app.ui.theme.CardBorderSubtle
import com.ciphervault.app.ui.theme.CyanAccent
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
import com.ciphervault.app.ui.theme.WarningAmber
import com.ciphervault.app.ui.theme.WarningSubtle
import com.ciphervault.app.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import com.ciphervault.app.viewmodel.VaultViewModel

/**
 * Modern Settings screen with categorized sections:
 * Security & Access Control, Storage & Data Management, and Danger Zone.
 */
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    vaultViewModel: VaultViewModel? = null,
    onNavigateToBackup: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as CipherVaultApp
    val vaultConfig = app.vaultConfig

    var showChangeKeyDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showWipeVaultDialog by remember { mutableStateOf(false) }

    val errorMessage by authViewModel.errorMessage.collectAsState()
    val totalVaultSize by (vaultViewModel?.totalSize ?: remember { MutableStateFlow(0L) }).collectAsState()
    val fileCount by (vaultViewModel?.fileCount ?: remember { MutableStateFlow(0) }).collectAsState()

    var biometricEnabled by remember { mutableStateOf(vaultConfig.isBiometricEnabled) }
    var autoLockEnabled by remember { mutableStateOf(vaultConfig.autoLockOnBackground) }
    var screenProtectionEnabled by remember { mutableStateOf(vaultConfig.screenProtectionEnabled) }
    var deleteOriginalEnabled by remember { mutableStateOf(vaultConfig.deleteOriginalAfterEncryption) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Security policies & storage management",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = VaultGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ══════════════════════════════════════════════
        //  SECTION 1: Security & Access Control
        // ══════════════════════════════════════════════
        item { SectionHeader("Security & Access Control") }

        // Biometric Unlock Switch
        if (authViewModel.isBiometricAvailable) {
            item {
                SettingsSwitchItem(
                    icon = Icons.Filled.Fingerprint,
                    iconColor = CyanAccent,
                    title = "Biometric Unlock",
                    subtitle = "Unlock vault via Fingerprint or Face ID",
                    checked = biometricEnabled,
                    onCheckedChange = { checked ->
                        if (authViewModel.setBiometricEnabled(checked)) {
                            biometricEnabled = checked
                        }
                    }
                )
            }
        }

        // Auto-Lock on Background Switch
        item {
            SettingsSwitchItem(
                icon = Icons.Filled.Timer,
                iconColor = VaultGreen,
                title = "Auto-Lock on Exit",
                subtitle = "Instantly wipes keys from RAM when app is backgrounded",
                checked = autoLockEnabled,
                onCheckedChange = { checked ->
                    autoLockEnabled = checked
                    vaultConfig.autoLockOnBackground = checked
                }
            )
        }

        // Anti-Screenshot / FLAG_SECURE Switch
        item {
            SettingsSwitchItem(
                icon = Icons.Filled.VisibilityOff,
                iconColor = WarningAmber,
                title = "Anti-Screenshot Protection",
                subtitle = "Blocks screenshots and masks app switcher preview",
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
        }

        // Backup & 2FA
        item {
            SettingsItem(
                icon = Icons.Filled.CloudUpload,
                iconColor = VaultGreen,
                title = "Backup & 2FA",
                subtitle = "Google Authenticator enrollment & file export",
                onClick = onNavigateToBackup,
                showChevron = true
            )
        }

        // Change PIN/Password
        item {
            SettingsItem(
                icon = Icons.Filled.LockReset,
                iconColor = CyanAccent,
                title = "Change ${if (authViewModel.keyType == "pin") "PIN" else "Password"}",
                subtitle = "Re-wraps master key with new PBKDF2 passphrase",
                onClick = { showChangeKeyDialog = true },
                showChevron = true
            )
        }

        // ══════════════════════════════════════════════
        //  SECTION 2: Storage Management
        // ══════════════════════════════════════════════
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader("Storage & Data")
        }

        // Delete Original Source Files Default
        item {
            SettingsSwitchItem(
                icon = Icons.Filled.DeleteSweep,
                iconColor = WarningAmber,
                title = "Delete Source Files by Default",
                subtitle = "Requests SAF deletion of unencrypted files after vaulting",
                checked = deleteOriginalEnabled,
                onCheckedChange = { checked ->
                    deleteOriginalEnabled = checked
                    vaultConfig.deleteOriginalAfterEncryption = checked
                }
            )
        }

        // Storage Breakdown Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorderSubtle, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vault Storage Payload", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(formatFileSize(totalVaultSize), style = MaterialTheme.typography.titleSmall, color = VaultGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$fileCount secured items with V2 pre-encryption compression",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Clear Decrypted Cache
        item {
            SettingsItem(
                icon = Icons.Filled.ClearAll,
                iconColor = WarningAmber,
                title = "Clear Decrypted Cache",
                subtitle = "Purges temporary preview files from disk",
                onClick = { showClearCacheDialog = true }
            )
        }

        // ══════════════════════════════════════════════
        //  SECTION 3: Danger Zone
        // ══════════════════════════════════════════════
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader("Danger Zone")
        }

        // Lock Vault Now
        item {
            SettingsItem(
                icon = Icons.Filled.Lock,
                iconColor = WarningAmber,
                title = "Lock Vault Now",
                subtitle = "Wipes keys from RAM and returns to lock screen",
                onClick = { authViewModel.lock() }
            )
        }

        // Factory Reset / Self-Destruct
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = ErrorSubtle.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                onClick = { showWipeVaultDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.DeleteForever, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reset & Destroy Vault", style = MaterialTheme.typography.bodyMedium, color = ErrorRed, fontWeight = FontWeight.Bold)
                        Text("Irrevocably deletes all ciphertext files and resets app", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }

        // ── About Section ──
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader("Security Standards")
        }

        item {
            SettingsItem(
                icon = Icons.Filled.Shield,
                iconColor = VaultGreen,
                title = "Encryption Standard",
                subtitle = "AES-256-GCM • PBKDF2 (150,000 iterations)",
                onClick = { }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Filled.Security,
                iconColor = CyanAccent,
                title = "2FA Compliance",
                subtitle = "RFC 6238 TOTP • Air-Gapped / Offline",
                onClick = { }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Filled.Info,
                iconColor = BlueAccent,
                title = "CipherVault Architecture",
                subtitle = "v2.2.0 • Hardware Keystore + Zero Knowledge",
                onClick = { }
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    // Change Key Dialog
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

    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = DarkCardElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Clear Decrypted Cache") },
            text = { Text("Remove all temporarily decrypted files from cache? Your encrypted vault files remain completely untouched.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vaultViewModel?.clearCache()
                        showClearCacheDialog = false
                    }
                ) {
                    Text("Clear Cache", color = WarningAmber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Wipe Vault Dialog
    if (showWipeVaultDialog) {
        AlertDialog(
            onDismissRequest = { showWipeVaultDialog = false },
            containerColor = DarkCardElevated,
            titleContentColor = ErrorRed,
            textContentColor = TextSecondary,
            title = { Text("Wipe Vault & Reset App?") },
            text = {
                Text(
                    "WARNING: This will permanently delete ALL encrypted files, Master Keys, biometric credentials, and local database records. This action CANNOT be undone.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipeVaultDialog = false
                        authViewModel.wipeVaultAndReset()
                    }
                ) {
                    Text("Destroy Vault", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeVaultDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = VaultGreen,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            if (showChevron) {
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
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
    var localError by remember { mutableStateOf<String?>(null) }
    val label = if (keyType == "pin") "PIN" else "Password"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Change $label") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentKey,
                    onValueChange = { currentKey = it },
                    label = { Text("Current $label") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = if (keyType == "pin") KeyboardType.NumberPassword else KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VaultGreen, unfocusedBorderColor = CardBorder),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { newKey = it },
                    label = { Text("New $label") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = if (keyType == "pin") KeyboardType.NumberPassword else KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VaultGreen, unfocusedBorderColor = CardBorder),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmNewKey,
                    onValueChange = { confirmNewKey = it },
                    label = { Text("Confirm New $label") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = if (keyType == "pin") KeyboardType.NumberPassword else KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VaultGreen, unfocusedBorderColor = CardBorder),
                    modifier = Modifier.fillMaxWidth()
                )
                val err = localError ?: errorMessage
                if (err != null) {
                    Text(err, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newKey != confirmNewKey) {
                        localError = "$label do not match"
                    } else if (newKey.length < 4) {
                        localError = "$label must be at least 4 characters"
                    } else {
                        localError = null
                        onConfirm(currentKey, newKey)
                    }
                }
            ) {
                Text("Change", color = VaultGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
