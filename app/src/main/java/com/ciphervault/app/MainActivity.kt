package com.ciphervault.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.ciphervault.app.ui.navigation.CipherVaultNavHost
import com.ciphervault.app.ui.theme.CipherVaultTheme

class MainActivity : FragmentActivity() {

    private val app get() = application as CipherVaultApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyScreenSecurity()
        enableEdgeToEdge()
        setContent {
            CipherVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CipherVaultNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyScreenSecurity()
    }

    override fun onStop() {
        super.onStop()
        // Auto-lock vault when placed in background if enabled in settings
        if (app.vaultConfig.autoLockOnBackground && app.vaultConfig.isInitialized) {
            app.lockVault()
        }
    }

    private fun applyScreenSecurity() {
        if (app.vaultConfig.screenProtectionEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
