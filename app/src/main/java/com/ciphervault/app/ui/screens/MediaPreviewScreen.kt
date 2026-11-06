package com.ciphervault.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.R
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.VaultViewModel
import kotlinx.coroutines.delay

private const val SeekStepMs = 10_000L
private val PlaybackSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/**
 * Secure media player for decrypted video and audio.
 *
 * Custom controls on top of Media3: tap to show or hide, double-tap either side to skip 10s,
 * scrubbing, playback speed, loop, mute, fill-screen, buffering and error states.
 */
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
    val mediaPreviewUri by vaultViewModel.mediaPreviewUri.collectAsState()
    val isLoading by vaultViewModel.isLoading.collectAsState()

    val isAudio = file?.mimeType?.startsWith("audio/") == true

    val activity = remember(context) { context.findActivity() }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Hide the system bars in landscape so the picture gets the whole screen.
    LaunchedEffect(isLandscape) {
        activity?.let { act ->
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            if (isLandscape) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // One player for the lifetime of the screen; media items are swapped into it.
    val player = remember { ExoPlayer.Builder(context).build() }

    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var hasError by remember { mutableStateOf(false) }
    var positionMs by rememberSaveable { mutableStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    // Kept across an activity recreation, so rotating doesn't reset the playback.
    var resumePlaying by rememberSaveable { mutableStateOf(true) }

    var speed by rememberSaveable { mutableStateOf(1f) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var isLooping by rememberSaveable { mutableStateOf(false) }
    var fillScreen by rememberSaveable { mutableStateOf(false) }

    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var speedMenuOpen by remember { mutableStateOf(false) }

    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    var flashSide by remember { mutableIntStateOf(0) } // -1 left, 1 right, 0 none
    var flashTick by remember { mutableIntStateOf(0) }

    val ready = mediaPreviewUri != null && !hasError
    val loading = isLoading || (mediaPreviewUri == null && !hasError)
    val isBuffering = playbackState == Player.STATE_BUFFERING
    val isEnded = playbackState == Player.STATE_ENDED
    val overlayVisible = isAudio || controlsVisible || !isPlaying || hasError || loading

    fun toggleOrientation() {
        interactionTick++
        activity?.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    fun togglePlay() {
        interactionTick++
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
            player.play()
        }
    }

    fun seekBy(deltaMs: Long) {
        interactionTick++
        val upper = if (durationMs > 0) durationMs else Long.MAX_VALUE
        val target = (player.currentPosition + deltaMs).coerceIn(0L, upper)
        player.seekTo(target)
        positionMs = target
    }

    // Decrypt the file for preview
    LaunchedEffect(file) {
        if (file != null) {
            // After a rotation the decrypted copy still exists, so don't decrypt it again.
            if (vaultViewModel.mediaPreviewUri.value == null) {
                vaultViewModel.prepareMediaPreview(file)
            }
        } else {
            onBack()
        }
    }

    // Hand the decrypted media to the player once it's ready
    LaunchedEffect(mediaPreviewUri) {
        mediaPreviewUri?.let { uri ->
            hasError = false
            player.setMediaItem(MediaItem.fromUri(uri), positionMs.coerceAtLeast(0L))
            // Restore what the person had set, which also covers an activity recreation.
            player.setPlaybackSpeed(speed)
            player.volume = if (isMuted) 0f else 1f
            player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            player.prepare()
            player.playWhenReady = resumePlaying
        }
    }

    // Handle external app intent
    LaunchedEffect(decryptedUri) {
        decryptedUri?.let { uri ->
            player.pause()
            app.ignoreNextBackgroundLock = true
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, file?.mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.preview_open_with)))
            vaultViewModel.clearDecryptedUri()
        }
    }

    // Mirror the player's state into Compose
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                resumePlaying = playWhenReady
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Keep the progress bar moving while playing
    LaunchedEffect(isPlaying, scrubbing) {
        while (isPlaying && !scrubbing) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            delay(250)
        }
    }

    // Hide the controls a few seconds after the last interaction while playing
    LaunchedEffect(controlsVisible, isPlaying, scrubbing, speedMenuOpen, interactionTick) {
        if (controlsVisible && isPlaying && !scrubbing && !speedMenuOpen) {
            delay(3500)
            controlsVisible = false
        }
    }

    // Fade the double-tap seek hint away
    LaunchedEffect(flashTick) {
        if (flashTick > 0) {
            delay(550)
            flashSide = 0
        }
    }

    // Pause whenever the app leaves the foreground
    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // A rotation pauses the activity too, but playback should carry on through it.
            if (event == Lifecycle.Event.ON_PAUSE && activity?.isChangingConfigurations != true) {
                player.pause()
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    // Release player and securely cleanup external cache file when this screen unmounts
    DisposableEffect(Unit) {
        onDispose {
            player.release()
            // Rotating recreates the activity: keep the decrypted copy and the orientation lock.
            val rotating = activity?.isChangingConfigurations == true
            if (!rotating) {
                decryptedUri?.let { vaultViewModel.deleteDecryptedCacheFile(it, context) }
                mediaPreviewUri?.let { vaultViewModel.deleteDecryptedCacheFile(it, context) }
                vaultViewModel.clearMediaPreviewUri()
                activity?.let { act ->
                    act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    WindowCompat.getInsetsController(act.window, act.window.decorView)
                        .show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAudio) VaultColors.Ground else Color.Black)
    ) {
        // ---- Picture ----
        if (isAudio) {
            AudioArtwork(modifier = Modifier.align(Alignment.Center))
        } else if (mediaPreviewUri != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        keepScreenOn = true
                    }
                },
                update = { view ->
                    view.resizeMode = if (fillScreen) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }
            )
        }

        // ---- Gestures: tap toggles the controls, double-tap skips ----
        if (!isAudio) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(ready) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                                interactionTick++
                            },
                            onDoubleTap = { offset ->
                                if (ready) {
                                    if (offset.x < size.width / 2f) {
                                        seekBy(-SeekStepMs)
                                        flashSide = -1
                                    } else {
                                        seekBy(SeekStepMs)
                                        flashSide = 1
                                    }
                                    flashTick++
                                }
                            }
                        )
                    }
            )

            AnimatedVisibility(
                visible = flashSide == -1,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 40.dp),
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                SeekFlash(icon = Icons.Outlined.Replay10, label = stringResource(R.string.preview_10_seconds_back))
            }
            AnimatedVisibility(
                visible = flashSide == 1,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 40.dp),
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                SeekFlash(icon = Icons.Outlined.Forward10, label = stringResource(R.string.preview_10_seconds_forward))
            }
        }

        // ---- Status: decrypting or failed ----
        if (loading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = VaultColors.Accent)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.preview_decrypting_file),
                    style = VaultType.Meta,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        if (hasError) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = VaultColors.ErrorRed,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.preview_couldnt_play_file),
                    style = VaultType.ItemTitle,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.preview_file_unsupported),
                    style = VaultType.Meta,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { file?.let { vaultViewModel.decryptFile(it) } },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultColors.Accent,
                        contentColor = Color.Black
                    )
                ) {
                    Text(stringResource(R.string.preview_open_in_another), fontWeight = FontWeight.Bold)
                }
            }
        }

        // ---- Controls ----
        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize().displayCutoutPadding()) {

                // Top bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))
                        )
                        .statusBarsPadding()
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundControl(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        description = stringResource(R.string.preview_back),
                        size = 44.dp,
                        iconSize = 22.dp,
                        onClick = onBack
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file?.originalFileName ?: stringResource(R.string.preview_media_preview),
                            style = VaultType.ItemTitle.copy(fontSize = 16.sp),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (file != null) {
                            Text(
                                text = "${if (isAudio) stringResource(R.string.preview_audio) else stringResource(R.string.preview_video)}, ${formatFileSize(file.originalSize)}",
                                style = VaultType.Meta,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = { file?.let { vaultViewModel.decryptFile(it) } }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.preview_open_in_another),
                            tint = VaultColors.Accent
                        )
                    }
                }

                if (ready) {
                    // Centre transport for video
                    if (!isAudio) {
                        TransportRow(
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            isEnded = isEnded,
                            onBack10 = { seekBy(-SeekStepMs) },
                            onToggle = { togglePlay() },
                            onForward10 = { seekBy(SeekStepMs) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Bottom bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))
                            )
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 8.dp)
                    ) {
                        if (isAudio) {
                            TransportRow(
                                isPlaying = isPlaying,
                                isBuffering = isBuffering,
                                isEnded = isEnded,
                                onBack10 = { seekBy(-SeekStepMs) },
                                onToggle = { togglePlay() },
                                onForward10 = { seekBy(SeekStepMs) },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        val fraction = when {
                            scrubbing -> scrubFraction
                            durationMs > 0 -> positionMs.toFloat() / durationMs
                            else -> 0f
                        }.coerceIn(0f, 1f)

                        Slider(
                            value = fraction,
                            onValueChange = {
                                scrubbing = true
                                scrubFraction = it
                                interactionTick++
                            },
                            onValueChangeFinished = {
                                val target = (scrubFraction * durationMs).toLong()
                                player.seekTo(target)
                                positionMs = target
                                scrubbing = false
                            },
                            enabled = durationMs > 0,
                            colors = SliderDefaults.colors(
                                thumbColor = VaultColors.Accent,
                                activeTrackColor = VaultColors.Accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                                disabledThumbColor = Color.White.copy(alpha = 0.3f),
                                disabledActiveTrackColor = Color.White.copy(alpha = 0.3f),
                                disabledInactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val shownPosition = if (scrubbing) (scrubFraction * durationMs).toLong() else positionMs
                            Text(
                                text = formatPlaybackTime(shownPosition),
                                style = VaultType.Meta,
                                color = Color.White
                            )
                            Text(
                                text = formatPlaybackTime(durationMs),
                                style = VaultType.Meta,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speed
                            Box {
                                Text(
                                    text = formatSpeed(speed),
                                    style = VaultType.Meta,
                                    fontWeight = FontWeight.Bold,
                                    color = if (speed != 1f) Color.Black else Color.White,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (speed != 1f) VaultColors.Accent else Color.White.copy(alpha = 0.16f)
                                        )
                                        .clickable(
                                            role = Role.Button,
                                            onClickLabel = stringResource(R.string.preview_change_playback_speed)
                                        ) {
                                            speedMenuOpen = true
                                            interactionTick++
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                                DropdownMenu(
                                    expanded = speedMenuOpen,
                                    onDismissRequest = { speedMenuOpen = false },
                                    modifier = Modifier.background(VaultColors.SurfaceRaised)
                                ) {
                                    PlaybackSpeeds.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = formatSpeed(option),
                                                    color = if (option == speed) VaultColors.Accent else VaultColors.TextPrimary,
                                                    fontWeight = if (option == speed) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                speed = option
                                                player.setPlaybackSpeed(option)
                                                speedMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            ToggleIcon(
                                icon = if (isLooping) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                                description = if (isLooping) stringResource(R.string.preview_turn_loop_off) else stringResource(R.string.preview_loop_this_file),
                                active = isLooping,
                                onClick = {
                                    isLooping = !isLooping
                                    player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                                    interactionTick++
                                }
                            )
                            ToggleIcon(
                                icon = if (isMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                description = if (isMuted) stringResource(R.string.preview_unmute) else stringResource(R.string.preview_mute),
                                active = isMuted,
                                onClick = {
                                    isMuted = !isMuted
                                    player.volume = if (isMuted) 0f else 1f
                                    interactionTick++
                                }
                            )
                            if (!isAudio) {
                                ToggleIcon(
                                    icon = Icons.Outlined.ScreenRotation,
                                    description = if (isLandscape) stringResource(R.string.preview_rotate_to_portrait) else stringResource(R.string.preview_rotate_to_landscape),
                                    active = isLandscape,
                                    onClick = { toggleOrientation() }
                                )
                                ToggleIcon(
                                    icon = if (fillScreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                    description = if (fillScreen) stringResource(R.string.preview_fit_to_screen) else stringResource(R.string.preview_fill_screen),
                                    active = fillScreen,
                                    onClick = {
                                        fillScreen = !fillScreen
                                        interactionTick++
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
//  Pieces
// =====================================================================

@Composable
private fun TransportRow(
    isPlaying: Boolean,
    isBuffering: Boolean,
    isEnded: Boolean,
    onBack10: () -> Unit,
    onToggle: () -> Unit,
    onForward10: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundControl(
            icon = Icons.Outlined.Replay10,
            description = stringResource(R.string.preview_back_10_seconds),
            size = 56.dp,
            iconSize = 28.dp,
            onClick = onBack10
        )

        val label = when {
            isEnded -> stringResource(R.string.preview_replay)
            isPlaying -> stringResource(R.string.preview_pause)
            else -> stringResource(R.string.preview_play)
        }
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(VaultColors.Accent)
                .clickable(role = Role.Button, onClickLabel = label, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.Black,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = when {
                        isEnded -> Icons.Outlined.Replay
                        isPlaying -> Icons.Outlined.Pause
                        else -> Icons.Outlined.PlayArrow
                    },
                    contentDescription = label,
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        RoundControl(
            icon = Icons.Outlined.Forward10,
            description = stringResource(R.string.preview_forward_10_seconds),
            size = 56.dp,
            iconSize = 28.dp,
            onClick = onForward10
        )
    }
}

@Composable
private fun RoundControl(
    icon: ImageVector,
    description: String,
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun ToggleIcon(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) VaultColors.Accent else Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Feedback shown on the left or right edge after a double-tap skip. */
@Composable
private fun SeekFlash(icon: ImageVector, label: String) {
    Column(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = stringResource(R.string.preview_10s),
            style = VaultType.Meta,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Audio has no picture, so give it a calm focal point instead of a black screen. */
@Composable
private fun AudioArtwork(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(212.dp)
            .clip(CircleShape)
            .border(1.dp, VaultColors.Accent.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(VaultColors.AccentTint)
                .border(1.dp, VaultColors.Accent.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AudioFile,
                contentDescription = null,
                tint = VaultColors.Accent,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

// =====================================================================
//  Formatting
// =====================================================================

private fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}