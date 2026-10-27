package com.ciphervault.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
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
import com.ciphervault.app.ui.theme.ErrorRed
import com.ciphervault.app.ui.theme.TextPrimary
import com.ciphervault.app.ui.theme.TextSecondary
import com.ciphervault.app.ui.theme.VaultGreen
import com.ciphervault.app.ui.theme.VaultGreenSubtle
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(VaultGreenSubtle)
                .border(1.dp, VaultGreen.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = VaultGreen,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create Your Master Key",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Select your protection method. This derives your AES-256 wrapping key.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Key Type Selection ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KeyTypeOption(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Dialpad,
                title = "Numeric PIN",
                description = "4–6 digits code",
                isSelected = isPin,
                onClick = {
                    keyType = "pin"
                    key = ""
                    confirmKey = ""
                    localError = null
                    authViewModel.clearError()
                }
            )
            KeyTypeOption(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Password,
                title = "Password",
                description = "Alphanumeric passphrase",
                isSelected = !isPin,
                onClick = {
                    keyType = "password"
                    key = ""
                    confirmKey = ""
                    localError = null
                    authViewModel.clearError()
                }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Input Fields ──
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VaultGreen,
            unfocusedBorderColor = CardBorder,
            cursorColor = VaultGreen,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = VaultGreen,
            unfocusedLabelColor = TextSecondary,
            focusedContainerColor = DarkCard,
            unfocusedContainerColor = DarkCard
        )

        OutlinedTextField(
            value = key,
            onValueChange = {
                if (it.length <= maxLength) {
                    key = if (isPin) it.filter { c -> c.isDigit() } else it
                    localError = null
                    authViewModel.clearError()
                }
            },
            label = { Text(if (isPin) "Enter PIN (4–6 digits)" else "Enter Password") },
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
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = confirmKey,
            onValueChange = {
                if (it.length <= maxLength) {
                    confirmKey = if (isPin) it.filter { c -> c.isDigit() } else it
                    localError = null
                }
            },
            label = { Text(if (isPin) "Confirm PIN" else "Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password
            ),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Errors
        AnimatedVisibility(
            visible = (localError ?: errorMessage) != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = localError ?: errorMessage ?: "",
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Create Button
        Button(
            onClick = {
                when {
                    key.length < minLength -> localError = "Must be at least $minLength characters"
                    key != confirmKey -> localError = "${if (isPin) "PINs" else "Passwords"} do not match"
                    else -> {
                        if (authViewModel.createVault(keyType, key)) {
                            onComplete()
                        }
                    }
                }
            },
            enabled = key.length >= minLength && confirmKey.isNotEmpty() && !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = VaultGreen,
                contentColor = Color.Black,
                disabledContainerColor = VaultGreen.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text("Initialize Vault", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Notice: Your key derives the master encryption key using PBKDF2 (150k rounds). Keep it safe — it cannot be recovered if forgotten.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))
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
    Card(
        modifier = modifier
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) VaultGreen else CardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) VaultGreenSubtle else DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) VaultGreen.copy(alpha = 0.2f) else CyanSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) VaultGreen else CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}
