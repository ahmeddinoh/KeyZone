@file:Suppress("ComposableNaming", "COMPOSABLE_FUNCTION_CALL_WITHOUT_COMPOSE_PLUGIN")
package com.ciphervault.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.AuthViewModel
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay

/**
 * Enhanced Lock Screen with dynamic PIN length, auto-submit,
 * hardware-backed biometric unlock, haptic feedback, rate limiting, and shake error animation.
 *
 * Layout: the brand and the PIN dots (or password field) sit in the middle of the screen,
 * while the keypad (or unlock button) is anchored at the bottom where the thumb rests.
 */
@Composable
fun LockScreen(authViewModel: AuthViewModel) {
    val keyType = authViewModel.keyType
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val isProcessing by authViewModel.isProcessing.collectAsStateWithLifecycle()
    val lockoutSeconds by authViewModel.lockoutSecondsRemaining.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(value = false) }
    var backPressCount by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(backPressCount) {
        if (backPressCount > 0) {
            delay(2000)
            backPressCount = 0
        }
    }

    BackHandler {
        if (backPressCount == 0) {
            backPressCount++
            Toast.makeText(context, "Press back button again to exit", Toast.LENGTH_SHORT).show()
        } else {
            val activity = context as? android.app.Activity
            activity?.finish()
        }
    }

    val isPin = keyType == "pin"
    val isBiometricEnabled = authViewModel.isBiometricEnabled && authViewModel.isBiometricAvailable
    val inputEnabled = lockoutSeconds == 0 && !isProcessing
    val hasError = errorMessage != null && lockoutSeconds == 0

    fun promptBiometric() {
        (context as? FragmentActivity)?.let { activity ->
            authViewModel.unlockWithBiometric(activity)
        }
    }

    fun submitPassword() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!authViewModel.unlock(input)) {
            input = ""
        }
    }

    // Trigger error shake animation whenever an error message appears
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    (-16f) at 50
                    16f at 100
                    (-12f) at 150
                    12f at 200
                    (-6f) at 250
                    6f at 300
                    0f at 350
                },
            )
        }
    }

    // Auto-prompt biometric when the screen resumes
    DisposableEffect(lifecycleOwner, isBiometricEnabled, lockoutSeconds) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isBiometricEnabled && lockoutSeconds == 0) {
                // Introduce a tiny delay so the UI is fully visible before the prompt covers it
                promptBiometric()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Ground)
            .systemBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ---- Brand and entry ----
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(VaultColors.AccentTint)
                        .border(1.dp, VaultColors.Accent.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = VaultColors.Accent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "KeyZone",
                    style = VaultType.PageTitle,
                    color = VaultColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isPin) "Enter your PIN to unlock"
                    else "Enter your password to unlock",
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Only the entry area shakes, not the whole screen.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(shakeOffset.value.dp.roundToPx(), 0) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isPin) {
                        PinDots(enteredCount = input.length, isError = errorMessage != null)
                    } else {
                        OutlinedTextField(
                            value = input,
                            onValueChange = {
                                input = it
                                authViewModel.clearError()
                            },
                            label = { Text("Vault password") },
                            singleLine = true,
                            enabled = lockoutSeconds == 0,
                            isError = hasError,
                            visualTransformation = if (showPassword) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    if ((input.length >= 4) && !isProcessing && (lockoutSeconds == 0)) {
                                        submitPassword()
                                    }
                                }
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Outlined.VisibilityOff
                                        else Icons.Outlined.Visibility,
                                        contentDescription = if (showPassword) "Hide password" else "Show password",
                                        tint = VaultColors.TextSecondary
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
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
                                unfocusedLeadingIconColor = VaultColors.Accent,
                                errorLeadingIconColor = VaultColors.ErrorRed,
                                focusedContainerColor = VaultColors.Surface,
                                unfocusedContainerColor = VaultColors.Surface,
                                errorContainerColor = VaultColors.Surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp)
                        )
                    }

                    // One reserved slot for lockout, progress and errors, so nothing jumps around.
                    Box(
                        modifier = Modifier
                            .padding(top = 18.dp)
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            lockoutSeconds > 0 -> LockoutBanner(seconds = lockoutSeconds)
                            isProcessing -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = VaultColors.Accent,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Unlocking...",
                                    style = VaultType.Meta,
                                    color = VaultColors.TextSecondary
                                )
                            }
                            errorMessage != null -> Text(
                                text = errorMessage ?: "",
                                color = VaultColors.ErrorRed,
                                style = VaultType.Meta,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ---- Keypad or unlock button ----
        if (isPin) {
            PinKeypad(
                enabled = inputEnabled,
                showBiometric = isBiometricEnabled,
                showSubmit = input.length >= 4,
                onDigit = { digit ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    authViewModel.clearError()
                    if (input.length < 6) {
                        input += digit
                    }
                },
                onDelete = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    authViewModel.clearError()
                    if (input.isNotEmpty()) input = input.dropLast(1)
                },
                onBiometric = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    promptBiometric()
                },
                onSubmit = {
                    submitPassword()
                },
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 24.dp)
            ) {
                Button(
                    onClick = { submitPassword() },
                    enabled = (input.length >= 4) && !isProcessing && (lockoutSeconds == 0),
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
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Unlocking...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock vault", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                if (isBiometricEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { promptBiometric() },
                        enabled = inputEnabled,
                        border = BorderStroke(1.dp, VaultColors.Hairline),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = VaultColors.Surface),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            tint = VaultColors.Accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlock with fingerprint",
                            color = VaultColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
//  PIN entry
// =====================================================================

@Composable
private fun PinDots(enteredCount: Int, isError: Boolean) {
    val length = maxOf(6, enteredCount)
    Row(
        modifier = Modifier.semantics { contentDescription = "$enteredCount digits entered" },
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(length) { index ->
            val isFilled = index < enteredCount
            val activeColor = if (isError) VaultColors.ErrorRed else VaultColors.Accent
            val fill by animateColorAsState(
                targetValue = if (isFilled) activeColor else Color.Transparent,
                animationSpec = tween(120),
                label = "pinDotFill$index"
            )
            val outline by animateColorAsState(
                targetValue = if (isFilled) activeColor else VaultColors.Hairline,
                animationSpec = tween(120),
                label = "pinDotOutline$index"
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .border(2.dp, outline, CircleShape)
            )
        }
    }
}

/** Digits are filled circles; delete and biometrics are plain icons so the numbers stand out. */
@Composable
private fun PinKeypad(
    enabled: Boolean,
    showBiometric: Boolean,
    showSubmit: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onBiometric: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(modifier = modifier) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    KeyCell {
                        DigitKey(digit = digit, enabled = enabled, onClick = { onDigit(digit) })
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            KeyCell {
                if (showSubmit) {
                    IconKey(
                        icon = Icons.Outlined.LockOpen,
                        description = "Unlock",
                        tint = VaultColors.Accent,
                        iconSize = 30.dp,
                        enabled = enabled,
                        onClick = onSubmit
                    )
                } else if (showBiometric) {
                    IconKey(
                        icon = Icons.Outlined.Fingerprint,
                        description = "Unlock with biometrics",
                        tint = VaultColors.Accent,
                        iconSize = 30.dp,
                        enabled = enabled,
                        onClick = onBiometric
                    )
                }
            }
            KeyCell {
                DigitKey(digit = "0", enabled = enabled, onClick = { onDigit("0") })
            }
            KeyCell {
                IconKey(
                    icon = Icons.AutoMirrored.Outlined.Backspace,
                    description = "Delete",
                    tint = VaultColors.TextPrimary,
                    iconSize = 24.dp,
                    enabled = enabled,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.KeyCell(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun DigitKey(digit: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(VaultColors.Surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            color = VaultColors.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun IconKey(
    icon: ImageVector,
    description: String,
    tint: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

// =====================================================================
//  Status
// =====================================================================

@Composable
private fun LockoutBanner(seconds: Int) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VaultColors.ErrorRed.copy(alpha = 0.1f))
            .border(1.dp, VaultColors.ErrorRed.copy(alpha = 0.35f), shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.LockClock,
            contentDescription = null,
            tint = VaultColors.ErrorRed,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Too many attempts",
                style = VaultType.Meta,
                color = VaultColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Try again in ${seconds}s",
                style = VaultType.Meta,
                color = VaultColors.TextSecondary
            )
        }
    }
}