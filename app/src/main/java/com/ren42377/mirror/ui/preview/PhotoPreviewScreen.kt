package com.ren42377.mirror.ui.preview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ren42377.mirror.model.CaptureResult
import com.ren42377.mirror.ui.MirrorViewModel

@Composable
fun PhotoPreviewScreen(
    viewModel: MirrorViewModel,
    onClose: () -> Unit
) {
    val captureResult by viewModel.captureResult.collectAsStateWithLifecycle()
    val photoResult = captureResult as? CaptureResult.Photo
    var isClosing by remember { mutableStateOf(false) }
    var isFlippedInPreview by remember { mutableStateOf(false) }
    var isCompareMode by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val handleClose = {
        if (!isClosing) {
            isClosing = true
            viewModel.clearCaptureResult()
            onClose()
        }
    }

    BackHandler(onBack = handleClose)

    LaunchedEffect(photoResult) {
        if (photoResult == null && !isClosing) {
            isClosing = true
            onClose()
        }
    }

    if (photoResult == null) {
        return
    }

    val imageBitmap = remember(photoResult.bitmap) {
        photoResult.bitmap.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isCompareMode) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Captured View",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Captured View",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Flipped View",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = -1f
                            }
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Flipped Comparison",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        } else {
            val bitmapRatio = (photoResult.bitmap.width.toFloat() /
                photoResult.bitmap.height.coerceAtLeast(1).toFloat()).coerceIn(0.4f, 2.5f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmapRatio)
                    .align(Alignment.Center)
                    .clipToBounds()
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Captured Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (zoomScale > 1.05f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        zoomScale = 2.2f
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (zoomScale * zoom).coerceIn(1f, 4f)
                                zoomScale = newScale
                                if (newScale <= 1.01f) {
                                    panOffset = Offset.Zero
                                } else {
                                    val maxX = (size.width * (newScale - 1f)) / 2f
                                    val maxY = (size.height * (newScale - 1f)) / 2f
                                    panOffset = Offset(
                                        x = (panOffset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (panOffset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                }
                            }
                        }
                        .graphicsLayer {
                            val flipMultiplier = if (isFlippedInPreview) -1f else 1f
                            scaleX = zoomScale * flipMultiplier
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        }
                )
            }
        }

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
                    contentDescription = "Close Photo Preview"
                )
            }

            if (!isCompareMode) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = if (isFlippedInPreview) "Flipped View" else "Captured View",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        isCompareMode = false
                        isFlippedInPreview = !isFlippedInPreview
                    },
                    modifier = Modifier.size(48.dp),
                    colors = if (isFlippedInPreview && !isCompareMode) {
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
                        contentDescription = "Flip Photo"
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        isCompareMode = !isCompareMode
                        zoomScale = 1f
                        panOffset = Offset.Zero
                    },
                    modifier = Modifier.size(48.dp),
                    colors = if (isCompareMode) {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        IconButtonDefaults.filledTonalIconButtonColors()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = "Compare Views"
                    )
                }
            }
        }
    }
}
