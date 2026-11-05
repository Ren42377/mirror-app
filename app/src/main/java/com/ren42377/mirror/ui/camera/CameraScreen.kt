package com.ren42377.mirror.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.util.Range
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.LightMode
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ren42377.mirror.camera.CameraCapabilities
import com.ren42377.mirror.camera.CameraOptionsController
import com.ren42377.mirror.camera.FaceZoomTracker
import com.ren42377.mirror.camera.FrameRatePlanner
import com.ren42377.mirror.camera.queryCameraCapabilities
import com.ren42377.mirror.camera.sensorPointToDisplayNormalized
import com.ren42377.mirror.data.RecordingCacheManager
import com.ren42377.mirror.ui.MirrorViewModel
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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
    val density = LocalDensity.current
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

    DisposableEffect(context, uiState.isRingLightEnabled) {
        val window = context.findActivity()?.window
        if (window != null) {
            val params = window.attributes
            params.screenBrightness = if (uiState.isRingLightEnabled) {
                1.0f
            } else {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            window.attributes = params
        }
        onDispose {
            val activityWindow = context.findActivity()?.window
            if (activityWindow != null) {
                val params = activityWindow.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activityWindow.attributes = params
            }
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

    var tapFocusOffset by remember { mutableStateOf<Offset?>(null) }
    var tapFocusTimestamp by remember { mutableLongStateOf(0L) }
    var lastAutoMeterTimestamp by remember { mutableLongStateOf(0L) }

    var cameraCapabilities by remember { mutableStateOf(CameraCapabilities.EMPTY) }
    val faceZoomTracker = remember { FaceZoomTracker() }
    val optionsController = remember { CameraOptionsController() }
    var faceZoomFactor by remember { mutableFloatStateOf(1f) }
    var facePanNormX by remember { mutableFloatStateOf(0.5f) }
    var facePanNormY by remember { mutableFloatStateOf(0.5f) }

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

    LaunchedEffect(tapFocusTimestamp) {
        if (tapFocusTimestamp > 0L) {
            delay(1200L)
            tapFocusOffset = null
        }
    }

    LaunchedEffect(
        activeCamera,
        cameraCapabilities,
        uiState.isFaceAutoZoomEnabled,
        uiState.lensFacing
    ) {
        val camera = activeCamera ?: return@LaunchedEffect
        val capabilities = cameraCapabilities
        val activeArray = capabilities.activeArray
        if (!uiState.isFaceAutoZoomEnabled || !capabilities.isFaceTrackingSupported || activeArray == null) {
            faceZoomTracker.reset()
            faceZoomFactor = 1f
            facePanNormX = 0.5f
            facePanNormY = 0.5f
            applyPreviewTransform(
                view = previewView,
                isFlipped = uiState.isFlipped,
                zoom = 1f,
                previewNormX = 0.5f,
                previewNormY = 0.5f
            )
            optionsController.faceDetectMode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF
            optionsController.cropRegion = null
            optionsController.apply()
            viewModel.setFaceDetected(false)
            return@LaunchedEffect
        }

        camera.cameraInfo.zoomState.value?.let { zoomState ->
            camera.cameraControl.setZoomRatio(
                1f.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
            )
        }
        optionsController.faceDetectMode = capabilities.faceDetectMode
        optionsController.cropRegion = null
        optionsController.apply()

        var lastFaceVisible = false
        var lastMeterX = Float.NaN
        var lastMeterY = Float.NaN
        while (true) {
            val now = SystemClock.elapsedRealtime()
            val window = faceZoomTracker.step(now)
            if (window != null) {
                val cropCenter = sensorPointToDisplayNormalized(
                    x = (window.left + window.right) * 0.5f,
                    y = (window.top + window.bottom) * 0.5f,
                    arrayLeft = activeArray.left,
                    arrayTop = activeArray.top,
                    arrayWidth = activeArray.width(),
                    arrayHeight = activeArray.height(),
                    sensorOrientation = capabilities.sensorOrientation
                )
                val isFrontCamera = uiState.lensFacing == CameraSelector.LENS_FACING_FRONT
                val normX = if (isFrontCamera) 1f - cropCenter.x else cropCenter.x
                val normY = cropCenter.y
                faceZoomFactor = window.zoom
                facePanNormX = normX
                facePanNormY = normY
                applyPreviewTransform(
                    view = previewView,
                    isFlipped = uiState.isFlipped,
                    zoom = window.zoom,
                    previewNormX = normX,
                    previewNormY = normY
                )
                if (window.faceVisible != lastFaceVisible) {
                    lastFaceVisible = window.faceVisible
                    viewModel.setFaceDetected(window.faceVisible)
                    if (!window.faceVisible) {
                        lastMeterX = Float.NaN
                        lastMeterY = Float.NaN
                    }
                }
                if (window.faceVisible && now - lastAutoMeterTimestamp > FACE_METER_INTERVAL_MS) {
                    val point = sensorPointToDisplayNormalized(
                        x = window.faceSensorX,
                        y = window.faceSensorY,
                        arrayLeft = activeArray.left,
                        arrayTop = activeArray.top,
                        arrayWidth = activeArray.width(),
                        arrayHeight = activeArray.height(),
                        sensorOrientation = capabilities.sensorOrientation
                    )
                    val moved = lastMeterX.isNaN() ||
                        abs(point.x - lastMeterX) > FACE_METER_MOVE_THRESHOLD ||
                        abs(point.y - lastMeterY) > FACE_METER_MOVE_THRESHOLD
                    if (moved) {
                        lastMeterX = point.x
                        lastMeterY = point.y
                        lastAutoMeterTimestamp = now
                        meterAtNormalizedPoint(camera, point.x, point.y)
                    }
                }
            }
            delay(FACE_ZOOM_STEP_MS)
        }
    }

    DisposableEffect(previewView) {
        val scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (uiState.isFaceAutoZoomEnabled) return false
                    val camera = activeCamera ?: return false
                    val zoomState = camera.cameraInfo.zoomState.value ?: return false
                    val newRatio = (zoomState.zoomRatio * detector.scaleFactor)
                        .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                    camera.cameraControl.setZoomRatio(newRatio)
                    return true
                }
            }
        )
        val tapGestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                    val camera = activeCamera ?: return false
                    val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(
                        point,
                        FocusMeteringAction.FLAG_AF or
                            FocusMeteringAction.FLAG_AE or
                            FocusMeteringAction.FLAG_AWB
                    )
                        .setAutoCancelDuration(4, TimeUnit.SECONDS)
                        .build()
                    camera.cameraControl.startFocusAndMetering(action)
                    tapFocusOffset = Offset(event.x, event.y)
                    tapFocusTimestamp = SystemClock.elapsedRealtime()
                    lastAutoMeterTimestamp = tapFocusTimestamp
                    return true
                }
            }
        )
        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress) {
                tapGestureDetector.onTouchEvent(event)
            }
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

    LaunchedEffect(activeCamera, uiState.isRingLightEnabled, uiState.lensFacing) {
        val camera = activeCamera ?: return@LaunchedEffect
        if (camera.cameraInfo.hasFlashUnit()) {
            val enableTorch = uiState.isRingLightEnabled &&
                uiState.lensFacing == CameraSelector.LENS_FACING_BACK
            camera.cameraControl.enableTorch(enableTorch)
        }
    }

    LaunchedEffect(activeCamera, uiState.effectiveOpticalStabilization) {
        if (activeCamera == null) return@LaunchedEffect
        optionsController.opticalStabilization = uiState.effectiveOpticalStabilization
        optionsController.apply()
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

        val capabilities = matchingCameraInfo?.let { queryCameraCapabilities(it) }
            ?: CameraCapabilities.EMPTY
        cameraCapabilities = capabilities
        viewModel.updateActiveCameraCapabilities(
            capabilities = capabilities,
            lensFacing = resolvedLensFacing
        )
        configureFaceZoomTracker(faceZoomTracker, capabilities)

        val targetPhotoSize = viewModel.uiState.value.effectivePhotoSize
        val isHighResolutionTarget = targetPhotoSize != null &&
            capabilities.highResolutionPhotoSizes.contains(targetPhotoSize)

        optionsController.fpsRange = FrameRatePlanner.selectIdleRange(capabilities.aeFpsRanges)
        optionsController.opticalStabilizationSupported = capabilities.isOpticalStabilizationSupported
        optionsController.opticalStabilization = viewModel.uiState.value.effectiveOpticalStabilization

        val previewUseCase = buildPreviewUseCase(previewView, faceZoomTracker)
        val configuredImageCapture = buildImageCaptureUseCase(targetPhotoSize, isHighResolutionTarget)

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
            configureCameraZoomAndExposure(
                camera = camera,
                viewModel = viewModel,
                targetExposureIndex = uiState.exposureIndex
            )
            optionsController.attach(camera)

            val deliveredSize = configuredImageCapture.resolutionInfo?.resolution
            if (targetPhotoSize != null && deliveredSize != null && deliveredSize != targetPhotoSize) {
                viewModel.markPhotoSizeUnsupported(resolvedLensFacing, targetPhotoSize)
                rebindTrigger += 1
            }
        } catch (_: Exception) {
            runCatching {
                val fallbackPreview = buildPreviewUseCase(previewView, faceZoomTracker)
                val fallbackImageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    fallbackPreview,
                    fallbackImageCapture
                )
                activeCamera = camera
                imageCapture = fallbackImageCapture
                configureCameraZoomAndExposure(
                    camera = camera,
                    viewModel = viewModel,
                    targetExposureIndex = uiState.exposureIndex
                )
                optionsController.attach(camera)
                if (targetPhotoSize != null) {
                    viewModel.markPhotoSizeUnsupported(resolvedLensFacing, targetPhotoSize)
                    rebindTrigger += 1
                }
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
                applyPreviewTransform(
                    view = view,
                    isFlipped = uiState.isFlipped,
                    zoom = faceZoomFactor,
                    previewNormX = facePanNormX,
                    previewNormY = facePanNormY
                )
            }
        )

        val currentTapFocus = tapFocusOffset
        if (currentTapFocus != null) {
            val ringSizeDp = 64.dp
            val halfRingPx = with(density) { (ringSizeDp / 2).roundToPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = currentTapFocus.x.roundToInt() - halfRingPx,
                            y = currentTapFocus.y.roundToInt() - halfRingPx
                        )
                    }
                    .size(ringSizeDp)
                    .border(2.dp, Color(0xFFFFD54F), CircleShape)
            )
        }

        if (uiState.isRingLightEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 28.dp,
                        color = Color(0xFFFFF8E7).copy(alpha = 0.92f),
                        shape = RoundedCornerShape(36.dp)
                    )
            )
        }

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
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        val modeLabel = buildString {
                            append(if (uiState.isMirroredOnScreen) "Mirror View" else "True View")
                            if (uiState.isFaceAutoZoomEnabled) {
                                if (uiState.isFaceDetected) {
                                    append(" | Face Zoom")
                                } else {
                                    append(" | Finding Face")
                                }
                            }
                        }
                        Text(
                            text = modeLabel,
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
                    FilledTonalIconButton(
                        onClick = { viewModel.toggleFaceAutoZoom() },
                        enabled = !uiState.isCapturingPhoto && uiState.isFaceTrackingSupported,
                        colors = if (uiState.isFaceAutoZoomEnabled) {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Face Auto Zoom"
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { viewModel.toggleRingLight() },
                        enabled = !uiState.isCapturingPhoto,
                        colors = if (uiState.isRingLightEnabled) {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = "Ring Light"
                        )
                    }

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
                                contentDescription = "Exposure Brightness"
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Slider(
                            value = uiState.exposureIndex.toFloat(),
                            onValueChange = { value ->
                                viewModel.setExposureIndex(value.roundToInt())
                            },
                            valueRange = uiState.minExposureIndex.toFloat()..uiState.maxExposureIndex.toFloat(),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val sign = if (uiState.exposureIndex > 0) "+" else ""
                        Text(
                            text = "$sign${uiState.exposureIndex}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.End
                        )
                        TextButton(
                            onClick = {
                                viewModel.setExposureIndex(0)
                                activeCamera?.let { cam ->
                                    triggerCenterAutoExposure(cam)
                                }
                            }
                        ) {
                            Text(text = "Auto")
                        }
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
                    val captureZoom = if (uiState.isFaceAutoZoomEnabled) faceZoomFactor else 1f
                    val captureCenterX = if (uiState.isFlipped) 1f - facePanNormX else facePanNormX
                    val captureCenterY = facePanNormY
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
                                        cropZoom = captureZoom,
                                        cropCenterX = captureCenterX,
                                        cropCenterY = captureCenterY,
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
                                val capabilities = cameraCapabilities
                                val requestedFrameRate = uiState.effectiveFrameRate
                                val requestedStabilization = uiState.effectiveVideoStabilization
                                val currentZoomRatio = activeCamera?.cameraInfo?.zoomState?.value?.zoomRatio
                                val attempts = listOf(
                                    RecordingSetup(requestedFrameRate, requestedStabilization),
                                    RecordingSetup(null, false)
                                )

                                var recordingCamera: Camera? = null
                                var recordingUseCase: VideoCapture<Recorder>? = null
                                for (setup in attempts) {
                                    val previewUseCase = buildPreviewUseCase(previewView, faceZoomTracker)
                                    val videoUseCase = buildVideoCaptureUseCase(
                                        quality = uiState.effectiveVideoQuality.cameraQuality,
                                        frameRate = setup.frameRate,
                                        stabilization = setup.stabilization
                                    )
                                    try {
                                        cameraProvider.unbindAll()
                                        val camera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            previewUseCase,
                                            videoUseCase
                                        )
                                        recordingCamera = camera
                                        recordingUseCase = videoUseCase
                                        activeCamera = camera
                                        configureCameraZoomAndExposure(
                                            camera = camera,
                                            viewModel = viewModel,
                                            targetExposureIndex = uiState.exposureIndex,
                                            preserveZoomRatio = currentZoomRatio
                                        )
                                        optionsController.fpsRange = setup.frameRate
                                            ?.let { FrameRatePlanner.selectAeRange(capabilities.aeFpsRanges, it) }
                                            ?: FrameRatePlanner.selectIdleRange(capabilities.aeFpsRanges)
                                        optionsController.attach(camera)
                                        break
                                    } catch (_: Exception) {
                                        recordingCamera = null
                                        recordingUseCase = null
                                    }
                                }

                                val boundVideoCapture = recordingUseCase
                                if (recordingCamera == null || boundVideoCapture == null) {
                                    activeRecording = null
                                    viewModel.onRecordingStopped()
                                    rebindTrigger += 1
                                    return@launch
                                }

                                try {
                                    startVideoRecording(
                                        context = context,
                                        videoCapture = boundVideoCapture,
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
            highResolutionPhotoSizes = uiState.highResolutionPhotoSizes,
            selectedPhotoSize = uiState.effectivePhotoSize,
            videoModes = uiState.videoCapabilities,
            selectedVideoQuality = uiState.effectiveVideoQuality,
            selectedFrameRate = uiState.effectiveFrameRate,
            frameRateOptions = uiState.supportedFrameRates,
            isVideoStabilizationSupported = uiState.isVideoStabilizationSupported,
            isVideoStabilizationEnabled = uiState.effectiveVideoStabilization,
            isOpticalStabilizationSupported = uiState.isOpticalStabilizationSupported,
            isOpticalStabilizationEnabled = uiState.effectiveOpticalStabilization,
            onSelectPhotoSize = { viewModel.selectPhotoResolution(it) },
            onSelectVideoQuality = { viewModel.selectVideoQuality(it) },
            onSelectFrameRate = { viewModel.selectVideoFrameRate(it) },
            onToggleVideoStabilization = { viewModel.setVideoStabilization(it) },
            onToggleOpticalStabilization = { viewModel.setOpticalStabilization(it) },
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

private data class RecordingSetup(
    val frameRate: Int?,
    val stabilization: Boolean
)

private const val FACE_ZOOM_STEP_MS = 16L
private const val FACE_METER_INTERVAL_MS = 2500L
private const val FACE_METER_MOVE_THRESHOLD = 0.1f
private const val MIN_FACE_SCORE = 25

private fun applyPreviewTransform(
    view: PreviewView,
    isFlipped: Boolean,
    zoom: Float,
    previewNormX: Float,
    previewNormY: Float
) {
    val flipSign = if (isFlipped) -1f else 1f
    val activeZoom = zoom.coerceIn(1f, FaceZoomTracker.MAX_FACE_ZOOM)
    view.scaleX = flipSign * activeZoom
    view.scaleY = activeZoom

    val viewWidth = view.width.toFloat()
    val viewHeight = view.height.toFloat()
    if (viewWidth > 0f && viewHeight > 0f && activeZoom > 1.001f) {
        val contentWidth = min(viewWidth, viewHeight * 0.75f)
        val contentHeight = min(viewHeight, viewWidth * (4f / 3f))
        val maxTx = (activeZoom - 1f) * contentWidth * 0.5f
        val maxTy = (activeZoom - 1f) * contentHeight * 0.5f
        val desiredTx = (0.5f - previewNormX) * contentWidth * view.scaleX
        val desiredTy = (0.5f - previewNormY) * contentHeight * activeZoom
        view.translationX = desiredTx.coerceIn(-maxTx, maxTx)
        view.translationY = desiredTy.coerceIn(-maxTy, maxTy)
    } else {
        view.translationX = 0f
        view.translationY = 0f
    }
}

private fun configureFaceZoomTracker(tracker: FaceZoomTracker, capabilities: CameraCapabilities) {
    val activeArray = capabilities.activeArray
    if (activeArray == null) {
        tracker.configure(0, 0, 0, 0, 1f, false)
        return
    }
    tracker.configure(
        left = activeArray.left,
        top = activeArray.top,
        right = activeArray.right,
        bottom = activeArray.bottom,
        maxZoom = max(capabilities.maxDigitalZoom, FaceZoomTracker.MAX_FACE_ZOOM),
        canPan = true
    )
}

@OptIn(ExperimentalCamera2Interop::class)
private fun buildPreviewUseCase(
    previewView: PreviewView,
    faceZoomTracker: FaceZoomTracker
): Preview {
    val resolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .build()
    val builder = Preview.Builder()
        .setResolutionSelector(resolutionSelector)

    Camera2Interop.Extender(builder).setSessionCaptureCallback(
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                val faces = result.get(CaptureResult.STATISTICS_FACES)
                if (faces.isNullOrEmpty()) return
                val primaryFace = faces
                    .filter { it.score >= MIN_FACE_SCORE }
                    .maxByOrNull { it.bounds.width().toLong() * it.bounds.height().toLong() }
                    ?: return
                val bounds = primaryFace.bounds
                faceZoomTracker.onFace(
                    centerX = bounds.exactCenterX(),
                    centerY = bounds.exactCenterY(),
                    faceSize = FaceZoomTracker.faceSizeOf(bounds.width(), bounds.height()),
                    nowMs = SystemClock.elapsedRealtime()
                )
            }
        }
    )

    return builder.build().also {
        it.surfaceProvider = previewView.surfaceProvider
    }
}

private fun buildImageCaptureUseCase(targetSize: Size?, isHighResolution: Boolean): ImageCapture {
    val selectorBuilder = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)

    if (targetSize != null) {
        selectorBuilder.setResolutionStrategy(
            ResolutionStrategy(
                targetSize,
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        )
        if (isHighResolution) {
            selectorBuilder.setAllowedResolutionMode(
                ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
            )
        }
    }

    return ImageCapture.Builder()
        .setCaptureMode(
            if (isHighResolution) {
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            } else {
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            }
        )
        .setResolutionSelector(selectorBuilder.build())
        .build()
}

private fun buildVideoCaptureUseCase(
    quality: Quality,
    frameRate: Int?,
    stabilization: Boolean
): VideoCapture<Recorder> {
    val qualitySelector = QualitySelector.from(
        quality,
        FallbackStrategy.higherQualityOrLowerThan(quality)
    )
    val recorder = Recorder.Builder()
        .setQualitySelector(qualitySelector)
        .build()
    val builder = VideoCapture.Builder(recorder)
        .setMirrorMode(MirrorMode.MIRROR_MODE_OFF)
    if (frameRate != null) {
        builder.setTargetFrameRate(Range(frameRate, frameRate))
    }
    if (stabilization) {
        builder.setVideoStabilizationEnabled(true)
    }
    return builder.build()
}

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

    triggerCenterAutoExposure(camera)
}

private fun triggerCenterAutoExposure(camera: Camera) {
    meterAtNormalizedPoint(
        camera = camera,
        x = 0.5f,
        y = 0.5f,
        flags = FocusMeteringAction.FLAG_AE or
            FocusMeteringAction.FLAG_AF or
            FocusMeteringAction.FLAG_AWB
    )
}

private fun meterAtNormalizedPoint(
    camera: Camera,
    x: Float,
    y: Float,
    flags: Int = FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AF
) {
    runCatching {
        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        val point = factory.createPoint(x.coerceIn(0.1f, 0.9f), y.coerceIn(0.1f, 0.9f))
        val action = FocusMeteringAction.Builder(point, flags)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        camera.cameraControl.startFocusAndMetering(action)
    }
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

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
