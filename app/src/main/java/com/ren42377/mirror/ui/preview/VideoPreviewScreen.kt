package com.ren42377.mirror.ui.preview

import android.net.Uri
import android.view.LayoutInflater
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
import com.ren42377.mirror.model.CaptureResult
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

    LaunchedEffect(exoPlayer, isPlaying, isDraggingSlider) {
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
            delay(50L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(
                    R.layout.exo_texture_player_view,
                    null,
                    false
                ) as PlayerView).apply {
                    player = exoPlayer
                    scaleX = if (isMirroredPlayback) -1f else 1f
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                if (playerView.player !== exoPlayer) {
                    playerView.player = exoPlayer
                }
                playerView.scaleX = if (isMirroredPlayback) -1f else 1f
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

private fun formatMillis(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
