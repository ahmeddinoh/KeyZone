package com.ciphervault.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.ui.theme.VaultColors
import com.ciphervault.app.ui.theme.VaultType
import com.ciphervault.app.viewmodel.VaultViewModel
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val MaxZoom = 5f
private const val DoubleTapZoom = 2.5f

/**
 * Secure image viewer: pinch to zoom around your fingers, double-tap to zoom in and out,
 * pan within the picture's edges, rotate in quarter turns, and tap to go immersive.
 */
@Composable
fun ImagePreviewScreen(
    vaultViewModel: VaultViewModel,
    fileId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as CipherVaultApp
    val scope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() }

    val files by vaultViewModel.files.collectAsState()
    val file = files.find { it.id == fileId }

    var imageData by remember { mutableStateOf<ByteArray?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val decryptedUri by vaultViewModel.decryptedUri.collectAsState()
    var imagePixels by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Pan and zoom state. Translation is applied after scale and rotation, so it is in screen pixels.
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var quarterTurns by remember { mutableIntStateOf(0) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var zoomJob by remember { mutableStateOf<Job?>(null) }

    var overlayVisible by remember { mutableStateOf(true) }

    val rotation by animateFloatAsState(
        targetValue = quarterTurns * 90f,
        animationSpec = tween(300),
        label = "imageRotation"
    )

    // A rotated picture swaps width and height, so shrink it just enough to stay on screen.
    fun fitFactor(): Float {
        val w = containerSize.width.toFloat()
        val h = containerSize.height.toFloat()
        return if (quarterTurns % 2 != 0 && w > 0f && h > 0f) min(w / h, h / w) else 1f
    }
    val fit by animateFloatAsState(
        targetValue = fitFactor(),
        animationSpec = tween(300),
        label = "imageFit"
    )

    // How far the picture may be dragged before its edge would leave the screen edge.
    fun maxPan(totalScale: Float): Offset {
        val w = containerSize.width.toFloat()
        val h = containerSize.height.toFloat()
        val odd = quarterTurns % 2 != 0
        val extentW = (if (odd) h else w) * totalScale
        val extentH = (if (odd) w else h) * totalScale
        return Offset(max(0f, (extentW - w) / 2f), max(0f, (extentH - h) / 2f))
    }

    fun transformBy(centroid: Offset, pan: Offset, zoom: Float) {
        zoomJob?.cancel()
        val newScale = (scale * zoom).coerceIn(1f, MaxZoom)
        if (newScale <= 1f) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            return
        }
        val z = newScale / scale
        // Keep the point under the fingers where it is while zooming.
        val cx = centroid.x - containerSize.width / 2f
        val cy = centroid.y - containerSize.height / 2f
        val limit = maxPan(newScale * fitFactor())
        offsetX = (cx + pan.x - (cx - offsetX) * z).coerceIn(-limit.x, limit.x)
        offsetY = (cy + pan.y - (cy - offsetY) * z).coerceIn(-limit.y, limit.y)
        scale = newScale
    }

    fun animateZoomTo(targetScale: Float, focus: Offset?) {
        zoomJob?.cancel()
        val startScale = scale
        val startX = offsetX
        val startY = offsetY
        var endX = 0f
        var endY = 0f
        if (targetScale > 1f) {
            val z = targetScale / startScale
            val cx = (focus?.x ?: (containerSize.width / 2f)) - containerSize.width / 2f
            val cy = (focus?.y ?: (containerSize.height / 2f)) - containerSize.height / 2f
            val limit = maxPan(targetScale * fitFactor())
            endX = (cx - (cx - startX) * z).coerceIn(-limit.x, limit.x)
            endY = (cy - (cy - startY) * z).coerceIn(-limit.y, limit.y)
        }
        zoomJob = scope.launch {
            animate(0f, 1f, animationSpec = tween(260)) { t, _ ->
                scale = lerp(startScale, targetScale, t)
                offsetX = lerp(startX, endX, t)
                offsetY = lerp(startY, endY, t)
            }
        }
    }

    fun rotateBy(turns: Int) {
        quarterTurns += turns
        animateZoomTo(1f, null)
    }

    LaunchedEffect(file) {
        if (file != null) {
            imageData = vaultViewModel.decryptToByteArray(file)
            isLoading = false
        } else {
            onBack()
        }
    }

    // Handle external app intent
    LaunchedEffect(decryptedUri) {
        decryptedUri?.let { uri ->
            app.ignoreNextBackgroundLock = true
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, file?.mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
            vaultViewModel.clearDecryptedUri()
        }
    }

    // Immersive: the system bars go away together with the controls.
    LaunchedEffect(overlayVisible) {
        activity?.let { act ->
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            if (overlayVisible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Securely cleanup external cache file when this screen unmounts
    DisposableEffect(Unit) {
        onDispose {
            decryptedUri?.let { vaultViewModel.deleteDecryptedCacheFile(it, context) }
            activity?.let { act ->
                WindowCompat.getInsetsController(act.window, act.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Built once per image, so zooming doesn't rebuild the request on every frame.
    val imageRequest = remember(imageData) {
        imageData?.let { bytes ->
            ImageRequest.Builder(context)
                .data(ByteBuffer.wrap(bytes))
                // Decode larger than the screen so zoomed-in pictures stay sharp.
                .size(4096)
                .crossfade(true)
                .build()
        }
    }

    val ready = !isLoading && imageRequest != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ---- Picture and gestures ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .pointerInput(ready) {
                    detectTapGestures(
                        onTap = { overlayVisible = !overlayVisible },
                        onDoubleTap = { tap ->
                            if (ready) {
                                if (scale > 1.05f) animateZoomTo(1f, null)
                                else animateZoomTo(DoubleTapZoom, tap)
                            }
                        }
                    )
                }
                .pointerInput(ready) {
                    if (ready) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            transformBy(centroid, pan, zoom)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (ready) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = file?.originalFileName,
                    contentScale = ContentScale.Fit,
                    onSuccess = { state ->
                        val size = state.painter.intrinsicSize
                        if (size.width > 0f && size.height > 0f) {
                            imagePixels = size.width.toInt() to size.height.toInt()
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Read in the draw phase so gestures don't recompose the screen.
                            val total = scale * fit
                            scaleX = total
                            scaleY = total
                            translationX = offsetX
                            translationY = offsetY
                            rotationZ = rotation
                        }
                )
            }
        }

        // ---- Status ----
        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = VaultColors.Accent)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Decrypting your image...",
                    style = VaultType.Meta,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        } else if (imageRequest == null) {
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
                    text = "Couldn't open this image",
                    style = VaultType.ItemTitle,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "It may be damaged or in a format that isn't supported here. Try another app.",
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
                    Text("Open in another app", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ---- Top bar ----
        AnimatedVisibility(
            visible = overlayVisible || isLoading || imageRequest == null,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))
                    )
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(role = Role.Button, onClickLabel = "Go back", onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file?.originalFileName ?: "Image preview",
                        style = VaultType.ItemTitle.copy(fontSize = 16.sp),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (file != null) {
                        val details = listOfNotNull(
                            imagePixels?.let { "${it.first} \u00D7 ${it.second}" },
                            formatFileSize(file.originalSize)
                        ).joinToString(", ")
                        Text(
                            text = details,
                            style = VaultType.Meta,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                IconButton(onClick = { file?.let { vaultViewModel.decryptFile(it) } }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open in another app",
                        tint = VaultColors.Accent
                    )
                }
            }
        }

        // ---- Bottom tools ----
        AnimatedVisibility(
            visible = overlayVisible && ready,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = { rotateBy(-1) }) {
                    Icon(
                        imageVector = Icons.Outlined.RotateLeft,
                        contentDescription = "Rotate left",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { rotateBy(1) }) {
                    Icon(
                        imageVector = Icons.Outlined.RotateRight,
                        contentDescription = "Rotate right",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )

                val zoomed = scale > 1.02f
                Text(
                    text = if (zoomed) "%.1fx".format(scale) else "Fit",
                    style = VaultType.Meta,
                    fontWeight = FontWeight.Bold,
                    color = if (zoomed) VaultColors.Accent else Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = if (zoomed) "Fit to screen" else "Zoom in"
                        ) {
                            if (zoomed) animateZoomTo(1f, null) else animateZoomTo(DoubleTapZoom, null)
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}