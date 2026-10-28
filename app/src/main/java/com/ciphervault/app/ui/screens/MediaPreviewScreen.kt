package com.ciphervault.app.ui.screens

import android.content.Intent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DataSource
import androidx.media3.ui.PlayerView
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.crypto.CipherVaultDataSource
import com.ciphervault.app.ui.theme.DarkBackground
import com.ciphervault.app.ui.theme.TextPrimary
import com.ciphervault.app.ui.theme.VaultGreen
import com.ciphervault.app.viewmodel.VaultViewModel
import java.io.File

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun MediaPreviewScreen(
    vaultViewModel: VaultViewModel,
    fileId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as CipherVaultApp
    
    val files by vaultViewModel.files.collectAsState()
    val file = files.find { it.id == fileId }
    val decryptedUri by vaultViewModel.decryptedUri.collectAsState()

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Initialize ExoPlayer with our custom CipherVaultDataSource
    LaunchedEffect(file) {
        if (file != null) {
            val masterKey = app.sessionMasterKey
            if (masterKey != null) {
                val encryptedFile = File(app.fileRepository.vaultDir, file.encryptedFileName)
                
                val dataSourceFactory = DataSource.Factory {
                    CipherVaultDataSource(encryptedFile, masterKey)
                }
                
                val mediaSourceFactory = DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(dataSourceFactory)
                
                val player = ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build()
                
                player.setMediaItem(MediaItem.fromUri("ciphervault://media"))
                player.prepare()
                player.playWhenReady = true
                
                exoPlayer = player
                isLoading = false
            } else {
                onBack()
            }
        } else {
            onBack()
        }
    }

    // Handle external app intent
    LaunchedEffect(decryptedUri) {
        decryptedUri?.let { uri ->
            exoPlayer?.pause()
            app.ignoreNextBackgroundLock = true
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, file?.mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
            vaultViewModel.clearDecryptedUri()
        }
    }

    // Release player and securely cleanup external cache file when this screen unmounts
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
            decryptedUri?.let { vaultViewModel.deleteDecryptedCacheFile(it, context) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = file?.originalFileName ?: "Media Preview",
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { file?.let { vaultViewModel.decryptFile(it) } }) {
                        Icon(imageVector = Icons.Filled.OpenInNew, contentDescription = "Open in External App", tint = VaultGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = VaultGreen)
            } else if (exoPlayer != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                        }
                    }
                )
            }
        }
    }
}
