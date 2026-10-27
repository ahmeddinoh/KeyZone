package com.ciphervault.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.ciphervault.app.ui.theme.TextPrimary
import com.ciphervault.app.ui.theme.TextSecondary
import com.ciphervault.app.ui.theme.VaultGreen
import com.ciphervault.app.ui.theme.VaultGreenSubtle
import com.ciphervault.app.viewmodel.ImportViewModel

/**
 * Screen guiding the user through importing a vault:
 * Step 1: Select .cvault backup file
 * Step 2: Enter PIN/Password (the key)
 * Step 3: Enter 6-digit TOTP code (or recovery code)
 */
@Composable
fun ImportScreen(
    importViewModel: ImportViewModel,
    onBack: () -> Unit,
    onImportSuccess: () -> Unit
) {
    val step by importViewModel.step.collectAsState()
    val backupInfo by importViewModel.backupInfo.collectAsState()
    val isLoading by importViewModel.isLoading.collectAsState()
    val errorMessage by importViewModel.errorMessage.collectAsState()

    var keyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var totpInput by remember { mutableStateOf("") }
    var useRecoveryCode by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importViewModel.onFileSelected(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top row
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
                IconButton(onClick = {
                    if (step == ImportViewModel.ImportStep.SELECT_FILE) onBack()
                    else importViewModel.reset()
                }) {
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
                    text = "Import Vault",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Restore .cvault backup archive",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step progress indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepChip(number = 1, label = "File", isActive = step == ImportViewModel.ImportStep.SELECT_FILE)
            Spacer(modifier = Modifier.width(8.dp))
            StepChip(number = 2, label = "Key", isActive = step == ImportViewModel.ImportStep.ENTER_KEY)
            Spacer(modifier = Modifier.width(8.dp))
            StepChip(number = 3, label = "2FA", isActive = step == ImportViewModel.ImportStep.ENTER_TOTP)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Dynamic content based on step
        when (step) {
            ImportViewModel.ImportStep.SELECT_FILE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(CyanSubtle)
                            .border(1.dp, CyanAccent.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Select Backup File",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Choose a CipherVault backup (.cvault) from your device storage or downloads.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, CardBorder, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(20.dp),
                        onClick = { filePicker.launch(arrayOf("*/*")) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Browse Files",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to choose .cvault archive",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            ImportViewModel.ImportStep.ENTER_KEY -> {
                val isPin = backupInfo?.keyType == "pin"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(VaultGreenSubtle)
                            .border(1.dp, VaultGreen.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPin) Icons.Filled.Dialpad else Icons.Filled.Password,
                            contentDescription = null,
                            tint = VaultGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (isPin) "Enter Vault PIN" else "Enter Vault Password",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "This backup is protected by a ${if (isPin) "numeric PIN" else "password"}. Enter the key that was used when creating this vault.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    backupInfo?.let { info ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Backup Archive Verified:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• ${info.fileCount} encrypted file(s) included\n• Two-Factor Authentication: ${if (info.hasTotp) "Enabled (Required)" else "None"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = if (isPin) it.filter { c -> c.isDigit() } else it
                            importViewModel.clearError()
                        },
                        label = { Text(if (isPin) "Vault PIN" else "Vault Password") },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    imageVector = if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultGreen,
                            unfocusedBorderColor = CardBorder,
                            cursorColor = VaultGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { importViewModel.submitKey(keyInput) },
                        enabled = keyInput.length >= 4 && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text("Verify Key & Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            ImportViewModel.ImportStep.ENTER_TOTP -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(CyanSubtle)
                            .border(1.dp, CyanAccent.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (useRecoveryCode) "Enter Recovery Code" else "Two-Factor Verification",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (useRecoveryCode) {
                            "Enter one of your 8 emergency recovery codes (e.g. XXXXX-XXXXX)."
                        } else {
                            "Open Google Authenticator and enter the 6-digit code for CipherVault."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    OutlinedTextField(
                        value = totpInput,
                        onValueChange = {
                            val filtered = if (!useRecoveryCode) it.filter { c -> c.isDigit() }.take(6) else it.take(15)
                            totpInput = filtered
                            importViewModel.clearError()
                        },
                        label = { Text(if (useRecoveryCode) "Recovery Code" else "6-Digit Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (useRecoveryCode) KeyboardType.Ascii else KeyboardType.Number
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = CardBorder,
                            cursorColor = CyanAccent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = {
                        useRecoveryCode = !useRecoveryCode
                        totpInput = ""
                        importViewModel.clearError()
                    }) {
                        Text(
                            text = if (useRecoveryCode) "Switch back to 6-digit Authenticator code"
                            else "Lost your phone? Use an emergency recovery code",
                            color = CyanAccent,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            importViewModel.submitTotp(totpInput) {
                                onImportSuccess()
                            }
                        },
                        enabled = (if (useRecoveryCode) totpInput.length >= 8 else totpInput.length == 6) && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text("Validate & Open Vault", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            ImportViewModel.ImportStep.SUCCESS -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(VaultGreenSubtle)
                            .border(1.5.dp, VaultGreen.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = VaultGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Vault Restored Successfully",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Your encrypted files and keys have been restored to this device.\n\nOn future runs, you will only need your PIN/password to unlock. The Authenticator 2FA code is not needed for daily use.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    Button(
                        onClick = onImportSuccess,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Open Vault Dashboard", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Error message display
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StepChip(number: Int, label: String, isActive: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) VaultGreenSubtle else DarkCard)
            .border(1.dp, if (isActive) VaultGreen.copy(alpha = 0.5f) else CardBorderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isActive) VaultGreen else TextSecondary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.Black else TextPrimary
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) VaultGreen else TextSecondary
        )
    }
}
