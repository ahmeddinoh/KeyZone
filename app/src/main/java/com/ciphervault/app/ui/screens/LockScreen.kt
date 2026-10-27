package com.ciphervault.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.ciphervault.app.ui.theme.CardBorder
import com.ciphervault.app.ui.theme.CardBorderSubtle
import com.ciphervault.app.ui.theme.CyanAccent
import com.ciphervault.app.ui.theme.DarkBackground
import com.ciphervault.app.ui.theme.DarkCard
import com.ciphervault.app.ui.theme.DarkSurfaceVariant
import com.ciphervault.app.ui.theme.ErrorRed
import com.ciphervault.app.ui.theme.ErrorSubtle
import com.ciphervault.app.ui.theme.TextPrimary
import com.ciphervault.app.ui.theme.TextSecondary
import com.ciphervault.app.ui.theme.VaultGreen
import com.ciphervault.app.ui.theme.VaultGreenSubtle
import com.ciphervault.app.viewmodel.AuthViewModel

/**
 * Enhanced Lock Screen with dynamic PIN length, auto-submit,
 * hardware-backed biometric unlock, haptic feedback, rate limiting, and shake error animation.
 */
@Composable
fun LockScreen(authViewModel: AuthViewModel) {
    val keyType = authViewModel.keyType
    val pinLength = authViewModel.pinLength
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isProcessing by authViewModel.isProcessing.collectAsState()
    val lockoutSeconds by authViewModel.lockoutSecondsRemaining.collectAsState()

    var input by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }

    val isPin = keyType == "pin"
    val isBiometricEnabled = authViewModel.isBiometricEnabled && authViewModel.isBiometricAvailable

    // Trigger error shake animation whenever an error message appears
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    -16f at 50
                    16f at 100
                    -12f at 150
                    12f at 200
                    -6f at 250
                    6f at 300
                    0f at 350
                }
            )
        }
    }

    // Auto-prompt biometric on screen launch if enabled and not locked out
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && lockoutSeconds == 0) {
            (context as? FragmentActivity)?.let { activity ->
                authViewModel.unlockWithBiometric(activity)
            }
        }
    }

    // Auto-submit when user finishes entering PIN
    LaunchedEffect(input) {
        if (isPin && input.length == pinLength && lockoutSeconds == 0) {
            if (!authViewModel.unlock(input)) {
                input = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .offset(x = shakeOffset.value.dp)
        ) {
            // Shield logo
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(VaultGreenSubtle)
                    .border(1.5.dp, VaultGreen.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "CipherVault",
                    tint = VaultGreen,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CipherVault",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isPin) "Enter your $pinLength-digit PIN to derive key"
                else "Enter your password to derive key",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Rate limiting lockout banner
            if (lockoutSeconds > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = ErrorSubtle),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockClock,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Rate limited: try again in ${lockoutSeconds}s",
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (isPin) {
                // PIN dots display dynamically based on pinLength
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pinLength) { index ->
                        val isFilled = index < input.length
                        val dotColor = if (errorMessage != null) ErrorRed else VaultGreen
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) dotColor else Color.Transparent)
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled) dotColor else CardBorder,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            } else {
                // Password Text Field
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        authViewModel.clearError()
                    },
                    label = { Text("Vault Password") },
                    singleLine = true,
                    enabled = lockoutSeconds == 0,
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = "Toggle password visibility",
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
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Error display
            AnimatedVisibility(
                visible = errorMessage != null && lockoutSeconds == 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            if (errorMessage == null || lockoutSeconds > 0) {
                Spacer(modifier = Modifier.height(18.dp))
            }

            if (isPin) {
                // Custom numeric keypad with integrated Biometrics button in row 4
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    if (isBiometricEnabled) {
                                        FilledTonalButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                (context as? FragmentActivity)?.let { activity ->
                                                    authViewModel.unlockWithBiometric(activity)
                                                }
                                            },
                                            enabled = lockoutSeconds == 0 && !isProcessing,
                                            modifier = Modifier
                                                .size(68.dp)
                                                .border(1.dp, CyanAccent.copy(alpha = 0.4f), CircleShape),
                                            shape = CircleShape,
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = DarkCard
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Fingerprint,
                                                contentDescription = "Unlock with Biometrics",
                                                tint = CyanAccent,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(68.dp))
                                    }
                                }
                                "DEL" -> {
                                    FilledTonalButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            authViewModel.clearError()
                                            if (input.isNotEmpty()) input = input.dropLast(1)
                                        },
                                        enabled = lockoutSeconds == 0 && !isProcessing,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .border(1.dp, CardBorderSubtle, CircleShape),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = DarkCard
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                else -> {
                                    FilledTonalButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            authViewModel.clearError()
                                            if (input.length < pinLength) {
                                                input += key
                                            }
                                        },
                                        enabled = lockoutSeconds == 0 && !isProcessing,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .border(1.dp, CardBorderSubtle, CircleShape),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = DarkCard,
                                            contentColor = TextPrimary
                                        )
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Password unlock button
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!authViewModel.unlock(input)) {
                            input = ""
                        }
                    },
                    enabled = input.length >= 4 && !isProcessing && lockoutSeconds == 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text("Unlock Vault", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                if (isBiometricEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = {
                            (context as? FragmentActivity)?.let { activity ->
                                authViewModel.unlockWithBiometric(activity)
                            }
                        },
                        enabled = lockoutSeconds == 0 && !isProcessing,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock with Fingerprint", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
