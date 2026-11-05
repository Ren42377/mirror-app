package com.ren42377.mirror.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.provider.Settings
import android.util.Range
import android.util.Size
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
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
import androidx.compose.material.icons.filled.Brightness6
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraScreen(
    viewModel: MirrorViewModel,
    onNavigateToPhotoPreview: () -> Unit,
    onNavigateToVideoPreview: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
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

    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var rebindTrigger by remember { mutableIntStateOf(0) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    DisposableEffect(previewView) {
        val scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val camera = activeCamera ?: return false
                    val zoomState = camera.cameraInfo.zoomState.value ?: return false
                    val newRatio = (zoomState.zoomRatio * detector.scaleFactor)
                        .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                    camera.cameraControl.setZoomRatio(newRatio)
                    return true
                }
            }
        )
        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
        onDispose {
            previewView.setOnTouchListener(null)
        }
    }

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

    LaunchedEffect(activeCamera, uiState.exposureIndex) {
        val camera = activeCamera ?: return@LaunchedEffect
        val exposureState = camera.cameraInfo.exposureState
        if (exposureState.isExposureCompensationSupported) {
            val range = exposureState.exposureCompensationRange
            val clamped = uiState.exposureIndex.coerceIn(range.lower, range.upper)
            camera.cameraControl.setExposureCompensationIndex(clamped)
        }
    }

    LaunchedEffect(
        lifecycleOwner,
        uiState.lensFacing,
        uiState.settings.photoResolution,
        uiState.settings.videoQuality,
        rebindTrigger
    ) {
        if (activeRecording != null) return@LaunchedEffect

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
            ?: selectDefaultBinnedPhotoSize(photoSizes)

        val previewUseCase = buildPreviewUseCase(previewView)
        val configuredImageCapture = buildImageCaptureUseCase(targetPhotoSize)

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                configuredImageCapture
            )
            activeCamera = camera
            imageCapture = configuredImageCapture
            configureCameraZoomAndExposure(camera, viewModel, uiState.exposureIndex)
        } catch (_: Exception) {
            runCatching {
                val fallbackImageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewUseCase,
                    fallbackImageCapture
                )
                activeCamera = camera
                imageCapture = fallbackImageCapture
                configureCameraZoomAndExposure(camera, viewModel, uiState.exposureIndex)
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.hasExposureControl) {
                        FilledTonalIconButton(
                            onClick = { viewModel.toggleExposureSlider() },
                            enabled = !uiState.isCapturingPhoto,
                            colors = if (uiState.isExposureSliderVisible || uiState.exposureIndex != 0) {
                                IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                IconButtonDefaults.filledTonalIconButtonColors()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brightness6,
                                contentDescription = "Brightness"
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
            }

            if (uiState.isExposureSliderVisible && uiState.hasExposureControl) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Slider(
                            value = uiState.exposureIndex.toFloat(),
                            onValueChange = { value ->
                                viewModel.setExposureIndex(value.roundToInt())
                            },
                            valueRange = uiState.minExposureIndex.toFloat()..uiState.maxExposureIndex.toFloat(),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        val sign = if (uiState.exposureIndex > 0) "+" else ""
                        Text(
                            text = "$sign${uiState.exposureIndex}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
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
                            coroutineScope.launch {
                                val cameraProvider = getCameraProvider(context)
                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(uiState.lensFacing)
                                    .build()
                                val previewUseCase = buildPreviewUseCase(previewView)
                                val configuredVideoCapture = buildVideoCaptureUseCase(
                                    uiState.effectiveVideoQuality.cameraQuality
                                )
                                val currentZoomRatio = activeCamera?.cameraInfo?.zoomState?.value?.zoomRatio
                                try {
                                    cameraProvider.unbindAll()
                                    val camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        previewUseCase,
                                        configuredVideoCapture
                                    )
                                    activeCamera = camera
                                    configureCameraZoomAndExposure(
                                        camera = camera,
                                        viewModel = viewModel,
                                        targetExposureIndex = uiState.exposureIndex,
                                        preserveZoomRatio = currentZoomRatio
                                    )
                                    startVideoRecording(
                                        context = context,
                                        videoCapture = configuredVideoCapture,
                                        mirrorOutput = uiState.isMirroredOnScreen,
                                        viewModel = viewModel,
                                        onNavigateToVideoPreview = onNavigateToVideoPreview,
                                        onRecordingCreated = { recording ->
                                            activeRecording = recording
                                        },
                                        onRecordingFinished = {
                                            activeRecording = null
                                            rebindTrigger += 1
                                        }
                                    )
                                } catch (_: Exception) {
                                    activeRecording = null
                                    viewModel.onRecordingStopped()
                                    rebindTrigger += 1
                                }
                            }
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

private fun buildPreviewUseCase(previewView: PreviewView): Preview {
    val resolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .build()
    return Preview.Builder()
        .setResolutionSelector(resolutionSelector)
        .build()
        .also {
            it.surfaceProvider = previewView.surfaceProvider
        }
}

private fun buildImageCaptureUseCase(targetSize: Size?): ImageCapture {
    val selectorBuilder = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)

    if (targetSize != null) {
        selectorBuilder.setResolutionStrategy(
            ResolutionStrategy(
                targetSize,
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        )
    }

    return ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setResolutionSelector(selectorBuilder.build())
        .build()
}

private fun buildVideoCaptureUseCase(quality: Quality): VideoCapture<Recorder> {
    val qualitySelector = QualitySelector.from(
        quality,
        FallbackStrategy.higherQualityOrLowerThan(quality)
    )
    val recorder = Recorder.Builder()
        .setQualitySelector(qualitySelector)
        .build()
    return VideoCapture.Builder(recorder)
        .setMirrorMode(MirrorMode.MIRROR_MODE_OFF)
        .build()
}

@OptIn(ExperimentalCamera2Interop::class)
private fun configureCameraZoomAndExposure(
    camera: Camera,
    viewModel: MirrorViewModel,
    targetExposureIndex: Int,
    preserveZoomRatio: Float? = null
) {
    val zoomState = camera.cameraInfo.zoomState.value
    if (preserveZoomRatio != null && zoomState != null) {
        camera.cameraControl.setZoomRatio(
            preserveZoomRatio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        )
    } else if (zoomState != null) {
        camera.cameraControl.setZoomRatio(zoomState.minZoomRatio)
    } else {
        camera.cameraControl.setLinearZoom(0f)
    }

    val exposureState = camera.cameraInfo.exposureState
    if (exposureState.isExposureCompensationSupported) {
        val range = exposureState.exposureCompensationRange
        val appliedIndex = targetExposureIndex.coerceIn(range.lower, range.upper)
        viewModel.updateExposureRange(
            min = range.lower,
            max = range.upper,
            current = appliedIndex
        )
        camera.cameraControl.setExposureCompensationIndex(appliedIndex)
    } else {
        viewModel.updateExposureRange(min = 0, max = 0, current = 0)
    }

    runCatching {
        val camera2Info = Camera2CameraInfo.from(camera.cameraInfo)
        val fpsRanges = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
        )
        val bestLowLightRange = selectBestAdaptiveFpsRange(fpsRanges)
        if (bestLowLightRange != null) {
            val options = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    bestLowLightRange
                )
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
                .build()
            Camera2CameraControl.from(camera.cameraControl).setCaptureRequestOptions(options)
        }
    }
}

private fun selectBestAdaptiveFpsRange(ranges: Array<Range<Int>>?): Range<Int>? {
    if (ranges.isNullOrEmpty()) return null
    val candidates30 = ranges.filter { it.upper == 30 }
    if (candidates30.isNotEmpty()) {
        return candidates30.minByOrNull { it.lower }
    }
    val candidatesAtLeast24 = ranges.filter { it.upper >= 24 }
    if (candidatesAtLeast24.isNotEmpty()) {
        return candidatesAtLeast24.minWithOrNull(compareBy<Range<Int>> { it.lower }.thenBy { it.upper })
    }
    return ranges.minByOrNull { it.lower }
}

private fun selectDefaultBinnedPhotoSize(photoSizes: List<Size>): Size? {
    if (photoSizes.isEmpty()) return null
    val preferredFourByThree = photoSizes.firstOrNull { size ->
        val area = size.width.toLong() * size.height.toLong()
        val ratio = size.width.toDouble() / size.height.toDouble()
        val isFourByThree = kotlin.math.abs(ratio - (4.0 / 3.0)) < 0.06 ||
            kotlin.math.abs(ratio - (3.0 / 4.0)) < 0.06
        isFourByThree && area <= 13_000_000L
    }
    val preferredBinned = photoSizes.firstOrNull { size ->
        val area = size.width.toLong() * size.height.toLong()
        area <= 13_000_000L
    }
    return preferredFourByThree ?: preferredBinned ?: photoSizes.first()
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
