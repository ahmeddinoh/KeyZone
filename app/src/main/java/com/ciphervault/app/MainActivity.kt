package com.ciphervault.app

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import com.ciphervault.app.ui.navigation.CipherVaultNavHost
import com.ciphervault.app.ui.theme.CipherVaultTheme

class MainActivity : AppCompatActivity() {

    private val app get() = application as CipherVaultApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val uris = mutableListOf<Uri>()
        if (intent.action == Intent.ACTION_SEND) {
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { uris.add(it) }
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.forEach { uri ->
                uris.add(uri)
            }
        }
        if (uris.isNotEmpty()) {
            app.pendingSharedUris.value = uris
        }
    }

    override fun onResume() {
        super.onResume()
        applyScreenSecurity()
    }

    override fun onStart() {
        super.onStart()
        if (app.vaultConfig.autoLockOnBackground && app.vaultConfig.isInitialized) {
            val delayMillis = app.vaultConfig.autoLockDelayMillis
            if (delayMillis > 0L && app.backgroundTimestamp > 0L) {
                val elapsed = System.currentTimeMillis() - app.backgroundTimestamp
                if (elapsed >= delayMillis) {
                    app.lockVault()
                }
                app.backgroundTimestamp = 0L
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations) {
            return
        }
        if (app.ignoreNextBackgroundLock) {
            app.ignoreNextBackgroundLock = false
            return
        }
        // Auto-lock vault when placed in background if enabled in settings
        if (app.vaultConfig.autoLockOnBackground && app.vaultConfig.isInitialized) {
            val delayMillis = app.vaultConfig.autoLockDelayMillis
            if (delayMillis == 0L) {
                app.lockVault()
            } else {
                app.backgroundTimestamp = System.currentTimeMillis()
            }
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
