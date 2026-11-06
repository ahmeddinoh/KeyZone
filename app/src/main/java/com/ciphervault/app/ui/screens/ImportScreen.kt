package com.ciphervault.app.ui.screens

import com.ciphervault.app.CipherVaultApp
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.ciphervault.app.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
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
    val context = androidx.compose.ui.platform.LocalContext.current
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

    val stepIndex = when (step) {
        ImportViewModel.ImportStep.SELECT_FILE -> 0
        ImportViewModel.ImportStep.ENTER_KEY -> 1
        ImportViewModel.ImportStep.ENTER_TOTP -> 2
        ImportViewModel.ImportStep.SUCCESS -> 3
    }
    // Once we know the backup has no 2FA, there is no third step to show.
    val stepLabels = if (backupInfo?.hasTotp != false) listOf(stringResource(R.string.import_step_file), stringResource(R.string.import_step_key), stringResource(R.string.import_step_2fa)) else listOf(stringResource(R.string.import_step_file), stringResource(R.string.import_step_key))
    val isSuccess = step == ImportViewModel.ImportStep.SUCCESS

    val canSubmitKey = keyInput.length >= 4 && !isLoading
    val canSubmitTotp = (if (useRecoveryCode) totpInput.length >= 8 else totpInput.length == 6) && !isLoading

    androidx.activity.compose.BackHandler(enabled = !isSuccess) {
        if (step == ImportViewModel.ImportStep.SELECT_FILE) {
            onBack()
        } else {
            importViewModel.reset()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Ground)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // ---- Back ----
        Box(modifier = Modifier.height(44.dp)) {
            if (!isSuccess) {
                IconButton(
                    onClick = {
                        if (step == ImportViewModel.ImportStep.SELECT_FILE) onBack()
                        else importViewModel.reset()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VaultColors.SurfaceRaised)
                        .border(1.dp, VaultColors.Hairline, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.setup_back),
                        tint = VaultColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (!isSuccess) {
            Spacer(modifier = Modifier.height(20.dp))

            // ---- Header ----
            Text(
                text = stringResource(R.string.import_title_1),
                style = VaultType.PageTitle,
                color = VaultColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.import_title_2),
                style = VaultType.PageTitle.copy(fontStyle = FontStyle.Italic),
                color = VaultColors.Accent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.import_subtitle),
                style = VaultType.Meta,
                color = VaultColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            StepIndicator(labels = stepLabels, current = stepIndex)

            Spacer(modifier = Modifier.height(28.dp))
        }

        // ---- Step content ----
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn(tween(240, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
            modifier = Modifier.fillMaxWidth(),
            label = "importStep"
        ) { currentStep ->
            when (currentStep) {
                ImportViewModel.ImportStep.SELECT_FILE -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        StepHeader(
                            title = stringResource(R.string.import_select_backup),
                            description = stringResource(R.string.import_select_backup_desc)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        BackupDropZone(onClick = {
                            (context.applicationContext as? CipherVaultApp)?.ignoreNextBackgroundLock = true
                            filePicker.launch(arrayOf("*/*"))
                        })
                        ErrorPanel(message = errorMessage)
                    }
                }

                ImportViewModel.ImportStep.ENTER_KEY -> {
                    val isPin = backupInfo?.keyType == "pin"
                    Column(modifier = Modifier.fillMaxWidth()) {
                        StepHeader(
                            title = if (isPin) stringResource(R.string.import_enter_pin) else stringResource(R.string.import_enter_password),
                            description = stringResource(R.string.import_backup_protected, if (isPin) stringResource(R.string.import_numeric_pin) else stringResource(R.string.import_password)),
                        )

                        backupInfo?.let { info ->
                            Spacer(modifier = Modifier.height(20.dp))
                            BackupSummary(
                                fileCount = info.fileCount.toInt(),
                                hasTotp = info.hasTotp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = {
                                keyInput = if (isPin) it.filter { c -> c.isDigit() } else it
                                importViewModel.clearError()
                            },
                            label = { Text(if (isPin) stringResource(R.string.import_vault_pin) else stringResource(R.string.import_vault_password)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isPin) Icons.Outlined.Dialpad else Icons.Outlined.Password,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (canSubmitKey) importViewModel.submitKey(keyInput) }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(
                                        imageVector = if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (showKey) stringResource(R.string.import_hide_key) else stringResource(R.string.import_show_key),
                                        tint = VaultColors.TextSecondary
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = importFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        ErrorPanel(message = errorMessage)

                        Spacer(modifier = Modifier.height(24.dp))

                        PrimaryAction(
                            label = stringResource(R.string.import_verify_key),
                            enabled = canSubmitKey,
                            loading = isLoading,
                            onClick = { importViewModel.submitKey(keyInput) }
                        )
                    }
                }

                ImportViewModel.ImportStep.ENTER_TOTP -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        StepHeader(
                            title = if (useRecoveryCode) stringResource(R.string.import_enter_recovery) else stringResource(R.string.import_two_factor),
                            description = if (useRecoveryCode) {
                                stringResource(R.string.import_recovery_desc)
                            } else {
                                stringResource(R.string.import_auth_desc)
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = totpInput,
                            onValueChange = {
                                val filtered = if (!useRecoveryCode) it.filter { c -> c.isDigit() }.take(6) else it.take(15)
                                totpInput = filtered
                                importViewModel.clearError()
                            },
                            label = { Text(if (useRecoveryCode) stringResource(R.string.import_recovery_code) else stringResource(R.string.import_6digit_code)) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = if (useRecoveryCode) 16.sp else 24.sp,
                                letterSpacing = if (useRecoveryCode) 1.sp else 8.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (useRecoveryCode) KeyboardType.Ascii else KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (canSubmitTotp) importViewModel.submitTotp(totpInput) { onImportSuccess() }
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = importFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextButton(
                            onClick = {
                                useRecoveryCode = !useRecoveryCode
                                totpInput = ""
                                importViewModel.clearError()
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = if (useRecoveryCode) stringResource(R.string.import_switch_to_auth)
                                else stringResource(R.string.import_use_recovery),
                                color = VaultColors.Accent,
                                style = VaultType.Meta
                            )
                        }

                        ErrorPanel(message = errorMessage)

                        Spacer(modifier = Modifier.height(20.dp))

                        PrimaryAction(
                            label = stringResource(R.string.import_validate_open),
                            enabled = canSubmitTotp,
                            loading = isLoading,
                            onClick = { importViewModel.submitTotp(totpInput) { onImportSuccess() } }
                        )
                    }
                }

                ImportViewModel.ImportStep.SUCCESS -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))

                        SuccessEmblem()

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = stringResource(R.string.import_restored),
                            style = VaultType.PageTitle,
                            color = VaultColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.import_restored_desc),
                            style = VaultType.Meta,
                            color = VaultColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val noteShape = RoundedCornerShape(16.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(noteShape)
                                .background(VaultColors.SurfaceRaised)
                                .border(1.dp, VaultColors.Hairline, noteShape)
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = VaultColors.Accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (backupInfo?.hasTotp == true) {
                                    stringResource(R.string.import_2fa_not_needed)
                                } else {
                                    stringResource(R.string.import_can_enable_2fa)
                                },
                                style = VaultType.Meta,
                                color = VaultColors.TextSecondary,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        PrimaryAction(
                            label = stringResource(R.string.import_open_vault),
                            enabled = true,
                            loading = false,
                            onClick = onImportSuccess
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
//  Step pieces
// =====================================================================

@Composable
private fun StepHeader(title: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = VaultType.ItemTitle.copy(fontSize = 20.sp),
            color = VaultColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = VaultType.Meta,
            color = VaultColors.TextSecondary,
            lineHeight = 20.sp
        )
    }
}

/** The steps really are a sequence, so they're numbered and joined by a line that fills as you go. */
@Composable
private fun StepIndicator(labels: List<String>, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            StepNode(number = index + 1, label = label, done = index < current, active = index == current)
            if (index < labels.lastIndex) {
                val lineColor by animateColorAsState(
                    targetValue = if (index < current) VaultColors.Accent else VaultColors.Hairline,
                    label = "stepLine$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(lineColor)
                )
            }
        }
    }
}

@Composable
private fun StepNode(number: Int, label: String, done: Boolean, active: Boolean) {
    val fill by animateColorAsState(
        targetValue = when {
            done -> VaultColors.Accent
            active -> VaultColors.AccentTint
            else -> VaultColors.Surface
        },
        label = "stepFill$number"
    )
    val borderColor = when {
        done || active -> VaultColors.Accent
        else -> VaultColors.Hairline
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(fill)
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = stringResource(R.string.import_step_done, label),
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = number.toString(),
                    style = VaultType.Meta,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) VaultColors.Accent else VaultColors.TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = VaultType.Meta,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) VaultColors.TextPrimary else VaultColors.TextSecondary
        )
    }
}

@Composable
private fun BackupDropZone(onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VaultColors.AccentTint.copy(alpha = 0.55f))
            .dashedBorder(VaultColors.Accent.copy(alpha = 0.5f), cornerRadius = 24.dp)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.import_choose_backup), onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(
            icon = Icons.Outlined.CloudDownload,
            size = 72.dp,
            iconSize = 32.dp,
            background = VaultColors.Ground.copy(alpha = 0.55f)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.import_tap_choose),
            style = VaultType.ItemTitle,
            color = VaultColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
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
                text = stringResource(R.string.import_browse_files),
                style = VaultType.ItemTitle.copy(fontSize = 14.sp),
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BackupSummary(fileCount: Int, hasTotp: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VaultColors.SurfaceRaised)
            .border(1.dp, VaultColors.Hairline, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = VaultColors.Accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.import_verified),
                style = VaultType.Meta,
                color = VaultColors.Accent,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileCount.toString(),
                    style = VaultType.ItemTitle.copy(fontSize = 22.sp),
                    color = VaultColors.TextPrimary
                )
                Text(
                    text = if (fileCount == 1) stringResource(R.string.import_encrypted_file) else stringResource(R.string.import_encrypted_files),
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(VaultColors.Hairline)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = if (hasTotp) stringResource(R.string.import_required) else stringResource(R.string.import_off),
                    style = VaultType.ItemTitle.copy(fontSize = 22.sp),
                    color = if (hasTotp) VaultColors.Accent else VaultColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.import_2fa_title),
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SuccessEmblem() {
    Box(
        modifier = Modifier.size(148.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .border(1.dp, VaultColors.Accent.copy(alpha = 0.15f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(VaultColors.AccentTint)
                .border(1.dp, VaultColors.Accent.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = VaultColors.Accent,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

// =====================================================================
//  Shared pieces
// =====================================================================

@Composable
private fun PrimaryAction(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = VaultColors.Accent,
            contentColor = Color.Black,
            disabledContainerColor = VaultColors.Accent.copy(alpha = 0.3f),
            disabledContentColor = Color.Black.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.Black,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun ErrorPanel(message: String?) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val shape = RoundedCornerShape(14.dp)
        Row(
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(VaultColors.ErrorRed.copy(alpha = 0.1f))
                .border(1.dp, VaultColors.ErrorRed.copy(alpha = 0.35f), shape)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = VaultColors.ErrorRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message ?: "",
                color = VaultColors.TextPrimary,
                style = VaultType.Meta
            )
        }
    }
}

@Composable
private fun importFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VaultColors.Accent,
    unfocusedBorderColor = VaultColors.Hairline,
    cursorColor = VaultColors.Accent,
    focusedTextColor = VaultColors.TextPrimary,
    unfocusedTextColor = VaultColors.TextPrimary,
    focusedLabelColor = VaultColors.Accent,
    unfocusedLabelColor = VaultColors.TextSecondary,
    focusedLeadingIconColor = VaultColors.Accent,
    unfocusedLeadingIconColor = VaultColors.TextSecondary,
    focusedContainerColor = VaultColors.Surface,
    unfocusedContainerColor = VaultColors.Surface
)

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