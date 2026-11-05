package com.ren42377.mirror.ui.preview

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ren42377.mirror.R
import com.ren42377.mirror.camera.FaceZoomTracker
import com.ren42377.mirror.model.CaptureResult
import com.ren42377.mirror.model.FaceZoomSample
import com.ren42377.mirror.ui.MirrorViewModel
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun VideoPreviewScreen(
    viewModel: MirrorViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val captureResult by viewModel.captureResult.collectAsStateWithLifecycle()
    val videoResult = captureResult as? CaptureResult.Video
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(videoResult) {
        if (videoResult == null && !isClosing) {
            isClosing = true
            onClose()
        }
    }

    if (videoResult == null) {
        return
    }

    var isMirroredPlayback by remember(videoResult) { mutableStateOf(videoResult.mirrored) }
    var isPlaying by remember { mutableStateOf(true) }
    var isEnded by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPositionMs by remember { mutableFloatStateOf(0f) }

    val exoPlayer = remember(videoResult.file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(videoResult.file)))
            prepare()
            playWhenReady = true
        }
    }

    val handleClose = {
        if (!isClosing) {
            isClosing = true
            exoPlayer.stop()
            viewModel.clearCaptureResult()
            onClose()
        }
    }

    BackHandler(onBack = handleClose)

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val total = exoPlayer.duration
                    if (total > 0L) {
                        durationMs = total
                    }
                    isEnded = false
                } else if (playbackState == Player.STATE_ENDED) {
                    isEnded = true
                    isPlaying = false
                    if (durationMs > 0L) {
                        currentPositionMs = durationMs
                        if (!isDraggingSlider) {
                            sliderPositionMs = durationMs.toFloat()
                        }
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val playerView = remember(context) {
        (LayoutInflater.from(context).inflate(
            R.layout.exo_texture_player_view,
            null,
            false
        ) as PlayerView).apply {
            clipChildren = true
            clipToPadding = true
            findViewById<ViewGroup>(androidx.media3.ui.R.id.exo_content_frame)?.apply {
                outlineProvider = ViewOutlineProvider.BOUNDS
                clipToOutline = true
                clipChildren = true
                clipToPadding = true
            }
        }
    }

    LaunchedEffect(exoPlayer, isPlaying, isDraggingSlider, isMirroredPlayback, videoResult.zoomSamples) {
        while (true) {
            val total = exoPlayer.duration
            if (total > 0L) {
                durationMs = total
            }
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            currentPositionMs = pos
            if (!isDraggingSlider) {
                sliderPositionMs = pos.toFloat()
            }
            val sampleTimeMs = if (isDraggingSlider) sliderPositionMs.toLong() else pos
            val zoomSample = sampleFaceZoomAt(videoResult.zoomSamples, sampleTimeMs)
            applyVideoZoomTransform(playerView, isMirroredPlayback, zoomSample)
            delay(16L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = {
                playerView.apply {
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (view.player !== exoPlayer) {
                    view.player = exoPlayer
                }
                val sampleTimeMs = if (isDraggingSlider) sliderPositionMs.toLong() else currentPositionMs
                val zoomSample = sampleFaceZoomAt(videoResult.zoomSamples, sampleTimeMs)
                applyVideoZoomTransform(view, isMirroredPlayback, zoomSample)
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = handleClose,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Video Preview"
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = if (isMirroredPlayback) "Mirror View" else "True View",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            FilledTonalIconButton(
                onClick = { isMirroredPlayback = !isMirroredPlayback },
                modifier = Modifier.size(48.dp),
                colors = if (isMirroredPlayback != videoResult.mirrored) {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    IconButtonDefaults.filledTonalIconButtonColors()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Flip,
                    contentDescription = "Flip Video"
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                val safeDuration = durationMs.coerceAtLeast(1L).toFloat()
                Slider(
                    value = sliderPositionMs.coerceIn(0f, safeDuration),
                    onValueChange = { newValue ->
                        isDraggingSlider = true
                        sliderPositionMs = newValue
                        currentPositionMs = newValue.toLong()
                    },
                    onValueChangeFinished = {
                        val targetMs = sliderPositionMs.toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                        exoPlayer.seekTo(targetMs)
                        if (isEnded && targetMs < durationMs) {
                            isEnded = false
                        }
                        isDraggingSlider = false
                    },
                    valueRange = 0f..safeDuration,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                when {
                                    isEnded -> {
                                        isEnded = false
                                        exoPlayer.seekTo(0L)
                                        exoPlayer.play()
                                    }
                                    exoPlayer.isPlaying -> {
                                        exoPlayer.pause()
                                    }
                                    else -> {
                                        if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                            exoPlayer.seekTo(0L)
                                        }
                                        exoPlayer.play()
                                    }
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isEnded -> Icons.Default.Replay
                                    isPlaying -> Icons.Default.Pause
                                    else -> Icons.Default.PlayArrow
                                },
                                contentDescription = when {
                                    isEnded -> "Replay Video"
                                    isPlaying -> "Pause Video"
                                    else -> "Play Video"
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = if (isEnded) "Replay" else if (isPlaying) "Playing" else "Paused",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val displayedCurrentMs = if (isDraggingSlider) {
                        sliderPositionMs.toLong()
                    } else {
                        currentPositionMs
                    }
                    Text(
                        text = "${formatMillis(displayedCurrentMs)} / ${formatMillis(durationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun sampleFaceZoomAt(
    samples: List<FaceZoomSample>,
    positionMs: Long
): FaceZoomSample {
    if (samples.isEmpty()) {
        return FaceZoomSample(positionMs, 1f, 0.5f, 0.5f)
    }
    if (positionMs <= samples.first().timeMs) {
        return samples.first()
    }
    if (positionMs >= samples.last().timeMs) {
        return samples.last()
    }
    val idx = samples.binarySearchBy(positionMs) { it.timeMs }
    if (idx >= 0) {
        return samples[idx]
    }
    val insertIndex = (-idx - 1).coerceIn(1, samples.lastIndex)
    val s0 = samples[insertIndex - 1]
    val s1 = samples[insertIndex]
    val span = (s1.timeMs - s0.timeMs).coerceAtLeast(1L).toFloat()
    val t = ((positionMs - s0.timeMs).toFloat() / span).coerceIn(0f, 1f)
    return FaceZoomSample(
        timeMs = positionMs,
        zoom = s0.zoom + (s1.zoom - s0.zoom) * t,
        normX = s0.normX + (s1.normX - s0.normX) * t,
        normY = s0.normY + (s1.normY - s0.normY) * t
    )
}

private fun applyVideoZoomTransform(
    playerView: PlayerView,
    isMirrored: Boolean,
    sample: FaceZoomSample
) {
    val contentFrame = playerView.findViewById<ViewGroup>(androidx.media3.ui.R.id.exo_content_frame)
    contentFrame?.let { frame ->
        if (!frame.clipToOutline) {
            frame.outlineProvider = ViewOutlineProvider.BOUNDS
            frame.clipToOutline = true
            frame.clipChildren = true
            frame.clipToPadding = true
        }
    }
    val targetView = playerView.videoSurfaceView ?: return
    playerView.scaleX = 1f
    playerView.scaleY = 1f
    playerView.translationX = 0f
    playerView.translationY = 0f

    val flipSign = if (isMirrored) -1f else 1f
    val activeZoom = sample.zoom.coerceIn(1f, FaceZoomTracker.MAX_FACE_ZOOM)
    targetView.scaleX = flipSign * activeZoom
    targetView.scaleY = activeZoom

    val w = targetView.width.toFloat()
    val h = targetView.height.toFloat()
    if (w > 0f && h > 0f && activeZoom > 1.001f) {
        val videoAspect = (w / h).coerceIn(0.4f, 1f)
        val sensorAspect = 0.75f
        val mappedNormX = if (videoAspect < sensorAspect) {
            ((sample.normX - 0.5f) * (sensorAspect / videoAspect) + 0.5f).coerceIn(0f, 1f)
        } else {
            sample.normX.coerceIn(0f, 1f)
        }
        val mappedNormY = if (videoAspect > sensorAspect) {
            ((sample.normY - 0.5f) * (videoAspect / sensorAspect) + 0.5f).coerceIn(0f, 1f)
        } else {
            sample.normY.coerceIn(0f, 1f)
        }
        val maxTx = (activeZoom - 1f) * w * 0.5f
        val maxTy = (activeZoom - 1f) * h * 0.5f
        val desiredTx = (0.5f - mappedNormX) * w * targetView.scaleX
        val desiredTy = (0.5f - mappedNormY) * h * activeZoom
        targetView.translationX = desiredTx.coerceIn(-maxTx, maxTx)
        targetView.translationY = desiredTy.coerceIn(-maxTy, maxTy)
    } else {
        targetView.translationX = 0f
        targetView.translationY = 0f
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
