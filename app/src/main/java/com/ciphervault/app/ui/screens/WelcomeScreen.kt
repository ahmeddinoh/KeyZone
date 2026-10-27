package com.ciphervault.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.ui.theme.CardBorder
import com.ciphervault.app.ui.theme.CyanAccent
import com.ciphervault.app.ui.theme.DarkBackground
import com.ciphervault.app.ui.theme.DarkCard
import com.ciphervault.app.ui.theme.TextPrimary
import com.ciphervault.app.ui.theme.TextSecondary
import com.ciphervault.app.ui.theme.VaultGreen
import com.ciphervault.app.ui.theme.VaultGreenSubtle
import kotlinx.coroutines.delay

/**
 * First-time welcome screen.
 * The user chooses to either create a new vault or import an existing one.
 */
@Composable
fun WelcomeScreen(
    onCreateVault: () -> Unit,
    onImportVault: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn(initialScale = 0.92f)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 36.dp)
            ) {
                // Shield Logo
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(VaultGreenSubtle)
                        .border(1.5.dp, VaultGreen.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = VaultGreen,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CipherVault",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(VaultGreenSubtle)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "2.0",
                            color = VaultGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Military-grade AES-256-GCM encrypted vault.\nYour PIN or password is the master key.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Create Vault Button
                Button(
                    onClick = onCreateVault,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Create New Vault", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Import Vault Button
                OutlinedButton(
                    onClick = onImportVault,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Import Existing Vault", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Zero-Knowledge Protection: We cannot recover your files if you lose your PIN, password, or 2FA credentials.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
