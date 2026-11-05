package com.ren42377.mirror.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.net.Uri
import android.provider.Settings
import android.util.Size
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ren42377.mirror.data.RecordingCacheManager
import com.ren42377.mirror.model.VideoQualityOption
import com.ren42377.mirror.ui.MirrorViewModel
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun CameraScreen(
    viewModel: MirrorViewModel,
    onNavigateToPhotoPreview: () -> Unit,
    onNavigateToVideoPreview: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val cameraGranted = result[Manifest.permission.CAMERA]
            ?: (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED)
        hasCameraPermission = cameraGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionDeniedContent(
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            },
            onOpenAppSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )
        return
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            var elapsed = 0L
            while (true) {
                delay(1000L)
                elapsed += 1L
                viewModel.onRecordingTick(elapsed)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            activeRecording = null
        }
    }

    LaunchedEffect(
        lifecycleOwner,
        uiState.lensFacing,
        uiState.settings.photoResolution,
        uiState.settings.videoQuality
    ) {
        val cameraProvider = getCameraProvider(context)
        val hasFront = runCatching {
            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        }.getOrDefault(false)
        val hasBack = runCatching {
            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        }.getOrDefault(false)

        viewModel.setLensFacingIfUnavailable(hasFront = hasFront, hasBack = hasBack)

        val resolvedLensFacing = when {
            uiState.lensFacing == CameraSelector.LENS_FACING_FRONT && !hasFront && hasBack ->
                CameraSelector.LENS_FACING_BACK
            uiState.lensFacing == CameraSelector.LENS_FACING_BACK && !hasBack && hasFront ->
                CameraSelector.LENS_FACING_FRONT
            else -> uiState.lensFacing
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(resolvedLensFacing)
            .build()

        val matchingCameraInfo = runCatching {
            cameraSelector.filter(cameraProvider.availableCameraInfos).firstOrNull()
        }.getOrNull()

        val photoSizes = matchingCameraInfo?.let { querySupportedJpegSizes(it) }.orEmpty()
        val videoQualities = matchingCameraInfo?.let { info ->
            QualitySelector.getSupportedQualities(info)
                .mapNotNull { VideoQualityOption.fromQuality(it) }
        }.orEmpty()

        viewModel.updateActiveCameraCapabilities(
            photoSizes = photoSizes,
            videoQualities = videoQualities
        )

        val targetPhotoSize = uiState.settings.parsePhotoSize()
            ?.takeIf { photoSizes.contains(it) }
            ?: photoSizes.firstOrNull()

        val targetVideoQualityOption = VideoQualityOption.fromKey(uiState.settings.videoQuality)
            .takeIf { videoQualities.contains(it) }
            ?: videoQualities.firstOrNull()
            ?: VideoQualityOption.FHD

        val previewUseCase = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val configuredImageCapture = buildImageCaptureUseCase(targetPhotoSize)
        val configuredVideoCapture = buildVideoCaptureUseCase(targetVideoQualityOption.cameraQuality)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                configuredImageCapture,
                configuredVideoCapture
            )
            imageCapture = configuredImageCapture
            videoCapture = configuredVideoCapture
        } catch (_: Exception) {
            runCatching {
                val fallbackImageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewUseCase,
                    fallbackImageCapture,
                    configuredVideoCapture
                )
                imageCapture = fallbackImageCapture
                videoCapture = configuredVideoCapture
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.scaleX = if (uiState.isFlipped) -1f else 1f
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.isRecording) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Red, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatDuration(uiState.recordingSeconds),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (uiState.isMirroredOnScreen) "Mirror View" else "True View",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            FilledTonalIconButton(
                onClick = { viewModel.openSettingsSheet() },
                enabled = !uiState.isRecording && !uiState.isCapturingPhoto
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(108.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { viewModel.toggleFlip() },
                    enabled = !uiState.isRecording && !uiState.isCapturingPhoto,
                    modifier = Modifier.size(48.dp),
                    colors = if (uiState.isFlipped) {
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
                        contentDescription = "Flip Horizontal"
                    )
                }

                if (uiState.hasMultipleCameras) {
                    FilledTonalIconButton(
                        onClick = { viewModel.switchCamera() },
                        enabled = !uiState.isRecording && !uiState.isCapturingPhoto,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera"
                        )
                    }
                }
            }

            FilledIconButton(
                onClick = {
                    val capture = imageCapture ?: return@FilledIconButton
                    if (uiState.isRecording || uiState.isCapturingPhoto) return@FilledIconButton
                    val mirrorOutput = uiState.isMirroredOnScreen
                    viewModel.setCapturingPhoto(true)
                    capture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                try {
                                    val rotation = image.imageInfo.rotationDegrees
                                    val rawBitmap = image.toBitmap()
                                    viewModel.processCapturedPhoto(
                                        sourceBitmap = rawBitmap,
                                        rotationDegrees = rotation,
                                        mirrorHorizontally = mirrorOutput,
                                        onComplete = onNavigateToPhotoPreview
                                    )
                                } catch (_: Exception) {
                                    viewModel.setCapturingPhoto(false)
                                } finally {
                                    image.close()
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                viewModel.setCapturingPhoto(false)
                            }
                        }
                    )
                },
                enabled = !uiState.isRecording && !uiState.isCapturingPhoto,
                modifier = Modifier
                    .size(76.dp)
                    .border(3.dp, Color.White, CircleShape),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Take Photo",
                    modifier = Modifier.size(32.dp)
                )
            }

            Box(
                modifier = Modifier.width(108.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                FilledIconButton(
                    onClick = {
                        if (uiState.isRecording) {
                            activeRecording?.stop()
                            activeRecording = null
                        } else {
                            val currentVideoCapture = videoCapture ?: return@FilledIconButton
                            startVideoRecording(
                                context = context,
                                videoCapture = currentVideoCapture,
                                mirrorOutput = uiState.isMirroredOnScreen,
                                viewModel = viewModel,
                                onNavigateToVideoPreview = onNavigateToVideoPreview,
                                onRecordingCreated = { recording ->
                                    activeRecording = recording
                                },
                                onRecordingFinished = {
                                    activeRecording = null
                                }
                            )
                        }
                    },
                    enabled = !uiState.isCapturingPhoto,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (uiState.isRecording) {
                            Color.Red
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        contentColor = if (uiState.isRecording) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isRecording) {
                            Icons.Default.Stop
                        } else {
                            Icons.Default.FiberManualRecord
                        },
                        contentDescription = if (uiState.isRecording) {
                            "Stop Recording"
                        } else {
                            "Record Video"
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    if (uiState.isSettingsOpen) {
        SettingsSheet(
            supportedPhotoSizes = uiState.supportedPhotoSizes,
            selectedPhotoSize = uiState.effectivePhotoSize,
            supportedVideoQualities = uiState.supportedVideoQualities,
            selectedVideoQuality = uiState.effectiveVideoQuality,
            onSelectPhotoSize = { viewModel.selectPhotoResolution(it) },
            onSelectVideoQuality = { viewModel.selectVideoQuality(it) },
            onDismiss = { viewModel.closeSettingsSheet() }
        )
    }
}

@Composable
private fun CameraPermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Mirror requires camera access to display your live reflection and let you preview photos and videos. Nothing is ever saved to your device storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Grant Camera Permission")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenAppSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Open App Settings")
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun startVideoRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    mirrorOutput: Boolean,
    viewModel: MirrorViewModel,
    onNavigateToVideoPreview: () -> Unit,
    onRecordingCreated: (Recording) -> Unit,
    onRecordingFinished: () -> Unit
) {
    val outputFile = RecordingCacheManager.createTempVideoFile(context)
    val outputOptions = FileOutputOptions.Builder(outputFile).build()
    val hasAudioPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val pendingRecording = videoCapture.output
        .prepareRecording(context, outputOptions)
        .let { pending ->
            if (hasAudioPermission) {
                pending.withAudioEnabled()
            } else {
                pending
            }
        }

    val recording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
        when (event) {
            is VideoRecordEvent.Start -> {
                viewModel.onRecordingStarted()
            }
            is VideoRecordEvent.Finalize -> {
                onRecordingFinished()
                if (!event.hasError() || (outputFile.exists() && outputFile.length() > 0L)) {
                    viewModel.onVideoCaptured(
                        file = outputFile,
                        mirrored = mirrorOutput,
                        onComplete = onNavigateToVideoPreview
                    )
                } else {
                    RecordingCacheManager.deleteRecording(outputFile)
                    viewModel.onRecordingStopped()
                }
            }
        }
    }
    onRecordingCreated(recording)
}

private fun buildImageCaptureUseCase(targetSize: Size?): ImageCapture {
    val builder = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

    if (targetSize != null) {
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    targetSize,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
        builder.setResolutionSelector(resolutionSelector)
    }

    return builder.build()
}

private fun buildVideoCaptureUseCase(quality: Quality): VideoCapture<Recorder> {
    val qualitySelector = QualitySelector.from(
        quality,
        FallbackStrategy.higherQualityOrLowerThan(quality)
    )
    val recorder = Recorder.Builder()
        .setQualitySelector(qualitySelector)
        .build()
    return VideoCapture.withOutput(recorder)
}

@OptIn(ExperimentalCamera2Interop::class)
private fun querySupportedJpegSizes(cameraInfo: androidx.camera.core.CameraInfo): List<Size> {
    return runCatching {
        val characteristics = Camera2CameraInfo.from(cameraInfo)
            .getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        characteristics
            ?.getOutputSizes(ImageFormat.JPEG)
            ?.sortedByDescending { it.width.toLong() * it.height.toLong() }
            .orEmpty()
    }.getOrDefault(emptyList())
}

private suspend fun getCameraProvider(context: Context): ProcessCameraProvider {
    return suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                continuation.resume(future.get())
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
