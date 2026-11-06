package com.ciphervault.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.ciphervault.app.R
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.AuthViewModel

enum class ChangeKeyStep {
    VERIFY_CURRENT,
    SET_NEW
}

@Composable
fun ChangeKeyScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    var step by remember { mutableStateOf(ChangeKeyStep.VERIFY_CURRENT) }
    var currentKey by remember { mutableStateOf("") }

    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isProcessing by authViewModel.isProcessing.collectAsState()
    var localError by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val validKeyError = stringResource(R.string.changekey_err_valid_key)

    // From the second step, back returns to the first step instead of leaving the screen.
    fun goBack() {
        if (step == ChangeKeyStep.SET_NEW) {
            step = ChangeKeyStep.VERIFY_CURRENT
            localError = null
            authViewModel.clearError()
        } else {
            onBack()
        }
    }

    BackHandler(enabled = step == ChangeKeyStep.SET_NEW) { goBack() }

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
        IconButton(
            onClick = { goBack() },
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

        Spacer(modifier = Modifier.height(20.dp))

        // ---- Header ----
        Text(
            text = stringResource(R.string.changekey_title_1),
            style = VaultType.PageTitle,
            color = VaultColors.TextPrimary
        )
        Text(
            text = stringResource(R.string.changekey_title_2),
            style = VaultType.PageTitle.copy(fontStyle = FontStyle.Italic),
            color = VaultColors.Accent
        )

        Spacer(modifier = Modifier.height(24.dp))

        ChangeKeyStepIndicator(
            labels = listOf(stringResource(R.string.changekey_step_current), stringResource(R.string.changekey_step_new)),
            current = if (step == ChangeKeyStep.VERIFY_CURRENT) 0 else 1
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ---- Step content ----
        AnimatedContent(
            targetState = step,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = { fadeIn(tween(240, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
            label = "ChangeKeyStep"
        ) { currentStep ->
            when (currentStep) {
                ChangeKeyStep.VERIFY_CURRENT -> {
                    VerifyCurrentKeyStep(
                        currentKeyType = authViewModel.keyType,
                        initialKey = currentKey,
                        isProcessing = isProcessing,
                        errorMessage = localError ?: errorMessage,
                        onInputChanged = {
                            localError = null
                            authViewModel.clearError()
                        },
                        onVerify = { key ->
                            if (key.length >= 4) {
                                currentKey = key
                                localError = null
                                authViewModel.clearError()
                                step = ChangeKeyStep.SET_NEW
                            } else {
                                localError = validKeyError
                            }
                        }
                    )
                }
                ChangeKeyStep.SET_NEW -> {
                    SetNewKeyStep(
                        isProcessing = isProcessing,
                        errorMessage = localError ?: errorMessage,
                        onInputChanged = { authViewModel.clearError() },
                        onEditCurrent = { goBack() },
                        onConfirm = { newKeyType, newKey ->
                            if (authViewModel.changeKey(currentKey, newKeyType, newKey)) {
                                onBack() // Successfully changed
                            } else {
                                // Error displayed via errorMessage
                            }
                        }
                    )
                }
            }
        }
    }
}

// =====================================================================
//  Step 1: current key
// =====================================================================

@Composable
private fun VerifyCurrentKeyStep(
    currentKeyType: String,
    initialKey: String,
    isProcessing: Boolean,
    errorMessage: String?,
    onInputChanged: () -> Unit,
    onVerify: (String) -> Unit
) {
    var key by remember { mutableStateOf(initialKey) }
    var showKey by remember { mutableStateOf(false) }

    val isPin = currentKeyType == "pin"
    val label = if (isPin) stringResource(R.string.setup_pin_label) else stringResource(R.string.setup_password_label)
    val canContinue = key.isNotEmpty() && !isProcessing

    Column(modifier = Modifier.fillMaxWidth()) {
        StepHeader(
            title = stringResource(R.string.changekey_enter_current, label),
            description = stringResource(R.string.changekey_verify_desc)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = key,
            onValueChange = {
                key = if (isPin) it.filter { c -> c.isDigit() } else it
                onInputChanged()
            },
            label = { Text(stringResource(R.string.changekey_current_label, label)) },
            leadingIcon = {
                Icon(
                    imageVector = if (isPin) Icons.Outlined.Dialpad else Icons.Outlined.Password,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            isError = errorMessage != null,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (canContinue) onVerify(key) }),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(
                        imageVector = if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showKey) stringResource(R.string.changekey_hide, label) else stringResource(R.string.changekey_show, label),
                        tint = VaultColors.TextSecondary
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = keyFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        ErrorPanel(message = errorMessage)

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryAction(
            label = stringResource(R.string.changekey_continue),
            enabled = canContinue,
            loading = false,
            onClick = { onVerify(key) }
        )
    }
}

// =====================================================================
//  Step 2: new key
// =====================================================================

@Composable
private fun SetNewKeyStep(
    isProcessing: Boolean,
    errorMessage: String?,
    onInputChanged: () -> Unit,
    onEditCurrent: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var keyType by remember { mutableStateOf("pin") }
    var key by remember { mutableStateOf("") }
    var confirmKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val isPin = keyType == "pin"
    val minLength = 4
    val maxLength = if (isPin) 6 else 64

    val keysMatch = confirmKey.isNotEmpty() && key == confirmKey
    val showMismatch = confirmKey.isNotEmpty() && confirmKey.length >= key.length && key != confirmKey
    val canSubmit = key.length >= minLength && confirmKey.isNotEmpty() && !isProcessing
    val visibleError = localError ?: errorMessage

    fun switchKeyType(type: String) {
        if (keyType == type) return
        keyType = type
        key = ""
        confirmKey = ""
        localError = null
        onInputChanged()
    }

    fun submit() {
        when {
            key.length < minLength -> localError = "Must be at least $minLength characters"
            key != confirmKey -> localError = "${if (isPin) "PINs" else "Passwords"} do not match"
            else -> onConfirm(keyType, key)
        }
    }

    val keyboardType = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password
    val transformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation()

    Column(modifier = Modifier.fillMaxWidth()) {
        StepHeader(
            title = "Choose your new key",
            description = "You'll use it to unlock the vault from now on."
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KeyTypeOption(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Dialpad,
                title = "Numeric PIN",
                description = "4 to 6 digits",
                isSelected = isPin,
                onClick = { switchKeyType("pin") }
            )
            KeyTypeOption(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Password,
                title = "Password",
                description = "Any characters, up to 64",
                isSelected = !isPin,
                onClick = { switchKeyType("password") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val counter: (@Composable () -> Unit)? = if (isPin) {
            { Text("${key.length}/$maxLength") }
        } else {
            null
        }

        OutlinedTextField(
            value = key,
            onValueChange = {
                if (it.length <= maxLength) {
                    key = if (isPin) it.filter { c -> c.isDigit() } else it
                    localError = null
                    onInputChanged()
                }
            },
            label = { Text(if (isPin) "New PIN" else "New password") },
            supportingText = counter,
            singleLine = true,
            visualTransformation = transformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(
                        imageVector = if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showKey) "Hide entries" else "Show entries",
                        tint = VaultColors.TextSecondary
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = keyFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        if (!isPin && key.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            StrengthMeter(score = strengthScore(key))
        }

        Spacer(modifier = Modifier.height(14.dp))

        val mismatchHint: (@Composable () -> Unit)? = if (showMismatch) {
            { Text("These don't match yet") }
        } else {
            null
        }

        OutlinedTextField(
            value = confirmKey,
            onValueChange = {
                if (it.length <= maxLength) {
                    confirmKey = if (isPin) it.filter { c -> c.isDigit() } else it
                    localError = null
                }
            },
            label = { Text(if (isPin) "Confirm new PIN" else "Confirm new password") },
            isError = showMismatch,
            supportingText = mismatchHint,
            singleLine = true,
            visualTransformation = transformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) submit() }),
            trailingIcon = {
                if (keysMatch) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Entries match",
                        tint = VaultColors.Accent,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = keyFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        ErrorPanel(message = visibleError)

        Spacer(modifier = Modifier.height(20.dp))

        val noticeShape = RoundedCornerShape(16.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(noticeShape)
                .background(VaultColors.WarningAmber.copy(alpha = 0.08f))
                .border(1.dp, VaultColors.WarningAmber.copy(alpha = 0.35f), noticeShape)
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = VaultColors.WarningAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "The new key can't be recovered if you forget it, so keep it somewhere safe.",
                style = VaultType.Meta,
                color = VaultColors.TextSecondary,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryAction(
            label = "Change key",
            enabled = canSubmit,
            loading = isProcessing,
            onClick = { submit() }
        )

        // A wrong current key is only reported here, so give a way back to fix it.
        TextButton(
            onClick = onEditCurrent,
            enabled = !isProcessing,
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = "Re-enter current key",
                color = VaultColors.Accent,
                style = VaultType.Meta
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// =====================================================================
//  Components
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

@Composable
private fun ChangeKeyStepIndicator(labels: List<String>, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            val done = index < current
            val active = index == current
            val fill by animateColorAsState(
                targetValue = when {
                    done -> VaultColors.Accent
                    active -> VaultColors.AccentTint
                    else -> VaultColors.Surface
                },
                label = "stepFill$index"
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(fill)
                        .border(
                            1.dp,
                            if (done || active) VaultColors.Accent else VaultColors.Hairline,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "$label done",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = (index + 1).toString(),
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

            if (index < labels.lastIndex) {
                val lineColor by animateColorAsState(
                    targetValue = if (done) VaultColors.Accent else VaultColors.Hairline,
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
private fun KeyTypeOption(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) VaultColors.Accent else VaultColors.Hairline,
        label = "keyTypeBorder"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) VaultColors.AccentTint else VaultColors.Surface,
        label = "keyTypeContainer"
    )

    Column(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, shape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(16.dp)
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
                    .background(if (isSelected) VaultColors.Accent.copy(alpha = 0.2f) else VaultColors.AccentTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VaultColors.Accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) VaultColors.Accent else VaultColors.Hairline,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = VaultType.ItemTitle.copy(fontSize = 15.sp),
            color = VaultColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = VaultType.Meta,
            color = VaultColors.TextSecondary
        )
    }
}

/** A rough guide based on length and character variety, from 1 (weak) to 3 (strong). */
private fun strengthScore(key: String): Int {
    var score = 0
    if (key.length >= 8) score++
    if (key.length >= 12) score++
    if (key.any { it.isLetter() } && key.any { it.isDigit() }) score++
    if (key.any { !it.isLetterOrDigit() }) score++
    return when {
        score >= 3 -> 3
        score == 2 -> 2
        else -> 1
    }
}

@Composable
private fun StrengthMeter(score: Int) {
    val activeColor = when (score) {
        1 -> VaultColors.ErrorRed
        2 -> VaultColors.WarningAmber
        else -> VaultColors.Accent
    }
    val label = when (score) {
        1 -> "Weak"
        2 -> "Fair"
        else -> "Strong"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { index ->
                val segmentColor by animateColorAsState(
                    targetValue = if (index < score) activeColor else VaultColors.Hairline,
                    label = "strengthSegment$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(segmentColor)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = VaultType.Meta,
            color = activeColor,
            fontWeight = FontWeight.SemiBold
        )
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
private fun keyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VaultColors.Accent,
    unfocusedBorderColor = VaultColors.Hairline,
    errorBorderColor = VaultColors.ErrorRed,
    cursorColor = VaultColors.Accent,
    focusedTextColor = VaultColors.TextPrimary,
    unfocusedTextColor = VaultColors.TextPrimary,
    focusedLabelColor = VaultColors.Accent,
    unfocusedLabelColor = VaultColors.TextSecondary,
    errorLabelColor = VaultColors.ErrorRed,
    focusedLeadingIconColor = VaultColors.Accent,
    unfocusedLeadingIconColor = VaultColors.TextSecondary,
    focusedContainerColor = VaultColors.Surface,
    unfocusedContainerColor = VaultColors.Surface,
    errorContainerColor = VaultColors.Surface,
    focusedSupportingTextColor = VaultColors.TextSecondary,
    unfocusedSupportingTextColor = VaultColors.TextSecondary,
    errorSupportingTextColor = VaultColors.ErrorRed
)