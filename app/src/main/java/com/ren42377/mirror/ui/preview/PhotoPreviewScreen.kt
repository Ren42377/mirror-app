package com.ren42377.mirror.ui.preview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
        Image(
            bitmap = imageBitmap,
            contentDescription = "Captured Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        FilledTonalIconButton(
            onClick = handleClose,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(48.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Photo Preview"
            )
        }
    }
}
