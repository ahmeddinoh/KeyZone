package com.ciphervault.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.AuthViewModel

/**
 * Vault creation screen.
 * User chooses between PIN (numeric) or password (alphanumeric),
 * enters it, confirms it, and derives the master key.
 */
@Composable
fun SetupScreen(
    authViewModel: AuthViewModel,
    onComplete: () -> Unit
) {
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isProcessing by authViewModel.isProcessing.collectAsState()

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
        authViewModel.clearError()
    }

    fun submit() {
        when {
            key.length < minLength -> localError = "Must be at least $minLength characters"
            key != confirmKey -> localError = "${if (isPin) "PINs" else "Passwords"} do not match"
            else -> {
                if (authViewModel.createVault(keyType, key)) {
                    onComplete()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Ground)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // ---- Header ----
        Text(
            text = "Create your",
            style = VaultType.PageTitle,
            color = VaultColors.TextPrimary
        )
        Text(
            text = "master key.",
            style = VaultType.PageTitle.copy(fontStyle = FontStyle.Italic),
            color = VaultColors.Accent
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Choose how you'll unlock your vault. It derives the AES-256 key that wraps everything inside.",
            style = VaultType.Meta,
            color = VaultColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ---- Key type ----
        Text(
            text = "How do you want to unlock it?",
            style = VaultType.Meta,
            color = VaultColors.TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )
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

        Spacer(modifier = Modifier.height(28.dp))

        // ---- Inputs ----
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VaultColors.Accent,
            unfocusedBorderColor = VaultColors.Hairline,
            errorBorderColor = VaultColors.ErrorRed,
            cursorColor = VaultColors.Accent,
            focusedTextColor = VaultColors.TextPrimary,
            unfocusedTextColor = VaultColors.TextPrimary,
            focusedLabelColor = VaultColors.Accent,
            unfocusedLabelColor = VaultColors.TextSecondary,
            errorLabelColor = VaultColors.ErrorRed,
            focusedContainerColor = VaultColors.Surface,
            unfocusedContainerColor = VaultColors.Surface,
            errorContainerColor = VaultColors.Surface,
            focusedSupportingTextColor = VaultColors.TextSecondary,
            unfocusedSupportingTextColor = VaultColors.TextSecondary,
            errorSupportingTextColor = VaultColors.ErrorRed
        )
        val keyboardType = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password
        val transformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation()

        val counter: (@Composable () -> Unit)? = if (isPin) {
            { Text("${key.length} of $maxLength digits") }
        } else {
            null
        }

        OutlinedTextField(
            value = key,
            onValueChange = {
                if (it.length <= maxLength) {
                    key = if (isPin) it.filter { c -> c.isDigit() } else it
                    localError = null
                    authViewModel.clearError()
                }
            },
            label = { Text(if (isPin) "PIN" else "Password") },
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
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        if (!isPin && key.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            StrengthMeter(strength = strengthOf(key))
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
            label = { Text(if (isPin) "Confirm PIN" else "Confirm password") },
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
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        // ---- Errors ----
        AnimatedVisibility(
            visible = visibleError != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(VaultColors.ErrorRed.copy(alpha = 0.1f))
                    .border(1.dp, VaultColors.ErrorRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
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
                    text = visibleError ?: "",
                    color = VaultColors.TextPrimary,
                    style = VaultType.Meta
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---- Notice: read before committing ----
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
            Column {
                Text(
                    text = "This key can't be recovered",
                    style = VaultType.ItemTitle.copy(fontSize = 14.sp),
                    color = VaultColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "It derives your master encryption key using PBKDF2 (150k rounds). If you forget it, your files stay locked.",
                    style = VaultType.Meta,
                    color = VaultColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Create ----
        Button(
            onClick = { submit() },
            enabled = canSubmit,
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
                Text("Creating vault...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Text("Create vault", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// =====================================================================
//  Components
// =====================================================================

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

private enum class Strength(val label: String, val segments: Int) {
    Weak("Weak", 1),
    Fair("Fair", 2),
    Strong("Strong", 3)
}

/** A rough guide based on length and character variety, not a guarantee. */
private fun strengthOf(key: String): Strength {
    var score = 0
    if (key.length >= 8) score++
    if (key.length >= 12) score++
    if (key.any { it.isLetter() } && key.any { it.isDigit() }) score++
    if (key.any { !it.isLetterOrDigit() }) score++
    return when {
        score >= 3 -> Strength.Strong
        score == 2 -> Strength.Fair
        else -> Strength.Weak
    }
}

@Composable
private fun StrengthMeter(strength: Strength) {
    val activeColor = when (strength) {
        Strength.Weak -> VaultColors.ErrorRed
        Strength.Fair -> VaultColors.WarningAmber
        Strength.Strong -> VaultColors.Accent
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
                    targetValue = if (index < strength.segments) activeColor else VaultColors.Hairline,
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
            text = strength.label,
            style = VaultType.Meta,
            color = activeColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}