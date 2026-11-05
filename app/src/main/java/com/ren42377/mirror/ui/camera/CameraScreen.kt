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
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.ren42377.mirror.model.AspectRatioOption
import com.ren42377.mirror.model.FaceZoomSample
import com.ren42377.mirror.ui.MirrorViewModel
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
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

    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1f) }
    var isZoomDialExpanded by remember { mutableStateOf(false) }
    var lastZoomTouchTimestamp by remember { mutableLongStateOf(0L) }
    var lensSwitchRotationCount by remember { mutableIntStateOf(0) }

    var tapFocusOffset by remember { mutableStateOf<Offset?>(null) }
    var tapFocusTimestamp by remember { mutableLongStateOf(0L) }
    var lastAutoMeterTimestamp by remember { mutableLongStateOf(0L) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    var cameraCapabilities by remember { mutableStateOf(CameraCapabilities.EMPTY) }
    val faceZoomTracker = remember { FaceZoomTracker() }
    val optionsController = remember { CameraOptionsController() }
    var faceZoomFactor by remember { mutableFloatStateOf(1f) }
    var facePanNormX by remember { mutableFloatStateOf(0.5f) }
    var facePanNormY by remember { mutableFloatStateOf(0.5f) }
    val recordingZoomSamples = remember { mutableListOf<FaceZoomSample>() }
    var recordingStartElapsedMs by remember { mutableLongStateOf(0L) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val previewContainer = remember(previewView) {
        FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            outlineProvider = ViewOutlineProvider.BOUNDS
            clipToOutline = true
            clipChildren = true
            clipToPadding = true
            addView(previewView)
        }
    }

    val animatedPreviewScaleX by animateFloatAsState(
        targetValue = if (uiState.isFlipped) -1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "previewFlipScaleX"
    )

    LaunchedEffect(tapFocusTimestamp) {
        if (tapFocusTimestamp > 0L) {
            delay(1200L)
            tapFocusOffset = null
        }
    }

    LaunchedEffect(isZoomDialExpanded, lastZoomTouchTimestamp, uiState.isFaceAutoZoomEnabled) {
        if (uiState.isFaceAutoZoomEnabled) {
            isZoomDialExpanded = false
            return@LaunchedEffect
        }
        if (isZoomDialExpanded && lastZoomTouchTimestamp > 0L) {
            delay(2600L)
            if (SystemClock.elapsedRealtime() - lastZoomTouchTimestamp >= 2500L) {
                isZoomDialExpanded = false
            }
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
                flipScaleX = animatedPreviewScaleX,
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
            val resetZoom = 1f.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
            camera.cameraControl.setZoomRatio(resetZoom)
            currentZoomRatio = resetZoom
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
                    flipScaleX = animatedPreviewScaleX,
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

    LaunchedEffect(uiState.lensFacing) {
        faceZoomTracker.reset()
    }

    LaunchedEffect(uiState.aspectRatio) {
        tapFocusOffset = null
        previewView.post {
            previewView.requestLayout()
            applyPreviewTransform(
                view = previewView,
                flipScaleX = animatedPreviewScaleX,
                zoom = faceZoomFactor,
                previewNormX = facePanNormX,
                previewNormY = facePanNormY
            )
        }
    }

    DisposableEffect(previewView) {
        val scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (uiState.isFaceAutoZoomEnabled) {
                        val updatedZoom = faceZoomTracker.onUserScale(detector.scaleFactor)
                        faceZoomFactor = updatedZoom
                        applyPreviewTransform(
                            view = previewView,
                            flipScaleX = animatedPreviewScaleX,
                            zoom = updatedZoom,
                            previewNormX = facePanNormX,
                            previewNormY = facePanNormY
                        )
                        return true
                    }
                    val camera = activeCamera ?: return false
                    val zoomState = camera.cameraInfo.zoomState.value ?: return false
                    val newRatio = (zoomState.zoomRatio * detector.scaleFactor)
                        .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                    camera.cameraControl.setZoomRatio(newRatio)
                    currentZoomRatio = newRatio
                    minZoomRatio = zoomState.minZoomRatio
                    maxZoomRatio = zoomState.maxZoomRatio
                    isZoomDialExpanded = true
                    lastZoomTouchTimestamp = SystemClock.elapsedRealtime()
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
                    isZoomDialExpanded = false
                    return true
                }
            }
        )
        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress && event.pointerCount == 1) {
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

    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            val startMs = if (recordingStartElapsedMs > 0L) {
                recordingStartElapsedMs
            } else {
                SystemClock.elapsedRealtime().also { recordingStartElapsedMs = it }
            }
            while (true) {
                val elapsedMs = (SystemClock.elapsedRealtime() - startMs).coerceAtLeast(0L)
                val isFront = uiState.lensFacing == CameraSelector.LENS_FACING_FRONT
                val unmirroredX = if (uiState.isFaceAutoZoomEnabled) {
                    if (isFront) 1f - facePanNormX else facePanNormX
                } else {
                    0.5f
                }
                val unmirroredY = if (uiState.isFaceAutoZoomEnabled) facePanNormY else 0.5f
                val zoom = if (uiState.isFaceAutoZoomEnabled) faceZoomFactor else 1f
                synchronized(recordingZoomSamples) {
                    recordingZoomSamples.add(
                        FaceZoomSample(
                            timeMs = elapsedMs,
                            zoom = zoom,
                            normX = unmirroredX,
                            normY = unmirroredY
                        )
                    )
                }
                delay(FACE_ZOOM_STEP_MS)
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
            val appliedZoom = configureCameraZoomAndExposure(
                camera = camera,
                viewModel = viewModel,
                targetExposureIndex = uiState.exposureIndex
            )
            currentZoomRatio = appliedZoom.current
            minZoomRatio = appliedZoom.min
            maxZoomRatio = appliedZoom.max
            isZoomDialExpanded = false
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
                val appliedZoom = configureCameraZoomAndExposure(
                    camera = camera,
                    viewModel = viewModel,
                    targetExposureIndex = uiState.exposureIndex
                )
                currentZoomRatio = appliedZoom.current
                minZoomRatio = appliedZoom.min
                maxZoomRatio = appliedZoom.max
                isZoomDialExpanded = false
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
            .onSizeChanged { rootSize = it }
    ) {
        val currentAspectRatio = uiState.aspectRatio
        val viewfinderModifier = if (currentAspectRatio == AspectRatioOption.FULLSCREEN) {
            Modifier
                .fillMaxSize()
                .clipToBounds()
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(currentAspectRatio.portraitRatio ?: (3f / 4f))
                .align(Alignment.Center)
                .clipToBounds()
        }

        Box(
            modifier = viewfinderModifier
        ) {
            AndroidView(
                factory = { previewContainer },
                modifier = Modifier.fillMaxSize(),
                update = {
                    applyPreviewTransform(
                        view = previewView,
                        flipScaleX = animatedPreviewScaleX,
                        zoom = faceZoomFactor,
                        previewNormX = facePanNormX,
                        previewNormY = facePanNormY
                    )
                }
            )

            val currentTapFocus = tapFocusOffset
            val focusScale by animateFloatAsState(
                targetValue = if (currentTapFocus != null) 1f else 1.35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "tapFocusScale"
            )
            if (currentTapFocus != null) {
                val ringSizeDp = 68.dp
                val halfRingPx = with(density) { (ringSizeDp / 2).roundToPx() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = currentTapFocus.x.roundToInt() - halfRingPx,
                                y = currentTapFocus.y.roundToInt() - halfRingPx
                            )
                        }
                        .size(ringSizeDp)
                        .scale(focusScale)
                        .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }

        val ringLightBorderWidth by animateDpAsState(
            targetValue = if (uiState.isRingLightEnabled) 32.dp else 0.dp,
            animationSpec = tween(durationMillis = 250),
            label = "ringLightWidth"
        )
        if (ringLightBorderWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = ringLightBorderWidth,
                        color = Color(0xFFFFFAF0).copy(alpha = 0.94f),
                        shape = RoundedCornerShape(40.dp)
                    )
            )
        }

        AnimatedVisibility(
            visible = uiState.isCapturingPhoto,
            enter = fadeIn(animationSpec = tween(80)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isRecording) {
                    val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(650),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "recordingDotAlpha"
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        MaterialTheme.colorScheme.error.copy(alpha = dotAlpha),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatDuration(uiState.recordingSeconds),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = {
                            if (!uiState.isCapturingPhoto) {
                                viewModel.toggleFlip()
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                        tonalElevation = 4.dp,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isMirroredOnScreen) "Mirror View" else "True View",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                    tonalElevation = 4.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopBarAspectRatioButton(
                            label = uiState.aspectRatio.label,
                            active = uiState.isAspectRatioBarVisible,
                            enabled = !uiState.isRecording && !uiState.isCapturingPhoto,
                            onClick = {
                                isZoomDialExpanded = false
                                viewModel.toggleAspectRatioBar()
                            }
                        )

                        TopBarToggleIcon(
                            icon = Icons.Default.Face,
                            contentDescription = "Face Auto Zoom",
                            active = uiState.isFaceAutoZoomEnabled,
                            enabled = !uiState.isCapturingPhoto && uiState.isFaceTrackingSupported,
                            onClick = { viewModel.toggleFaceAutoZoom() }
                        )

                        TopBarToggleIcon(
                            icon = Icons.Default.LightMode,
                            contentDescription = "Ring Light",
                            active = uiState.isRingLightEnabled,
                            enabled = !uiState.isCapturingPhoto,
                            onClick = { viewModel.toggleRingLight() }
                        )

                        if (uiState.hasExposureControl) {
                            TopBarToggleIcon(
                                icon = Icons.Default.Brightness6,
                                contentDescription = "Exposure Brightness",
                                active = uiState.isExposureSliderVisible || uiState.exposureIndex != 0,
                                enabled = !uiState.isCapturingPhoto,
                                onClick = { viewModel.toggleExposureSlider() }
                            )
                        }

                        TopBarToggleIcon(
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings",
                            active = uiState.isSettingsOpen,
                            enabled = !uiState.isRecording && !uiState.isCapturingPhoto,
                            onClick = {
                                isZoomDialExpanded = false
                                viewModel.openSettingsSheet()
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.isAspectRatioBarVisible && !uiState.isRecording,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                AspectRatioSelectorBar(
                    selectedRatio = uiState.aspectRatio,
                    onSelectRatio = { option ->
                        viewModel.selectAspectRatio(option)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }

            AnimatedVisibility(
                visible = uiState.isExposureSliderVisible && uiState.hasExposureControl,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    tonalElevation = 6.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
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
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${sign}${uiState.exposureIndex} EV",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = {
                                viewModel.setExposureIndex(0)
                                activeCamera?.let { cam ->
                                    triggerCenterAutoExposure(cam)
                                }
                            }
                        ) {
                            Text(
                                text = "Auto",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isFaceAutoZoomEnabled) {
                Surface(
                    onClick = { viewModel.toggleFaceAutoZoom() },
                    shape = CircleShape,
                    color = if (uiState.isFaceDetected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
                    },
                    tonalElevation = 4.dp,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = if (uiState.isFaceDetected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isFaceDetected) {
                                "Face Zoom Locked"
                            } else {
                                "Finding Face..."
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            color = if (uiState.isFaceDetected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            } else if (maxZoomRatio > 1.05f) {
                ExpandableZoomDialControl(
                    currentZoom = currentZoomRatio,
                    minZoom = minZoomRatio,
                    maxZoom = maxZoomRatio,
                    expanded = isZoomDialExpanded,
                    onExpand = {
                        isZoomDialExpanded = true
                        lastZoomTouchTimestamp = SystemClock.elapsedRealtime()
                    },
                    onCollapse = {
                        isZoomDialExpanded = false
                    },
                    onZoomChange = { newZoom ->
                        val camera = activeCamera ?: return@ExpandableZoomDialControl
                        val clamped = newZoom.coerceIn(minZoomRatio, maxZoomRatio)
                        camera.cameraControl.setZoomRatio(clamped)
                        currentZoomRatio = clamped
                        lastZoomTouchTimestamp = SystemClock.elapsedRealtime()
                    },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                tonalElevation = 6.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val flipContainer by animateColorAsState(
                            targetValue = if (uiState.isFlipped) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            label = "flipDockContainer"
                        )
                        val flipContent by animateColorAsState(
                            targetValue = if (uiState.isFlipped) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            label = "flipDockContent"
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FilledTonalIconButton(
                                onClick = { viewModel.toggleFlip() },
                                enabled = !uiState.isRecording && !uiState.isCapturingPhoto,
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = flipContainer,
                                    contentColor = flipContent
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flip,
                                    contentDescription = "Flip Horizontal"
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Flip",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (uiState.hasMultipleCameras) {
                            val lensRotation by animateFloatAsState(
                                targetValue = lensSwitchRotationCount * 180f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "lensSwitchRotation"
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FilledTonalIconButton(
                                    onClick = {
                                        lensSwitchRotationCount += 1
                                        viewModel.switchCamera()
                                    },
                                    enabled = !uiState.isRecording && !uiState.isCapturingPhoto,
                                    modifier = Modifier.size(48.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cameraswitch,
                                        contentDescription = "Switch Camera",
                                        modifier = Modifier.graphicsLayer {
                                            rotationZ = lensRotation
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Lens",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    val shutterScale by animateFloatAsState(
                        targetValue = if (uiState.isCapturingPhoto) 0.88f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "shutterScale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(78.dp)
                            .scale(shutterScale)
                            .border(
                                width = 3.dp,
                                color = if (!uiState.isRecording) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape
                            )
                            .padding(6.dp)
                    ) {
                        FilledIconButton(
                            onClick = {
                                val capture = imageCapture ?: return@FilledIconButton
                                if (uiState.isRecording || uiState.isCapturingPhoto) return@FilledIconButton
                                val mirrorOutput = uiState.isMirroredOnScreen
                                val captureZoom = if (uiState.isFaceAutoZoomEnabled) faceZoomFactor else 1f
                                val captureCenterX = if (uiState.isFlipped) 1f - facePanNormX else facePanNormX
                                val captureCenterY = facePanNormY
                                val captureAspectOption = uiState.aspectRatio
                                val captureTargetRatio = if (previewView.width > 0 && previewView.height > 0) {
                                    previewView.width.toFloat() / previewView.height.toFloat()
                                } else {
                                    captureAspectOption.resolvePortraitRatio(rootSize.width, rootSize.height)
                                }
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
                                                    targetAspectRatio = captureTargetRatio,
                                                    aspectRatioOption = captureAspectOption,
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
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take Photo",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        val recordCorner by animateDpAsState(
                            targetValue = if (uiState.isRecording) 18.dp else 28.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "recordCornerShape"
                        )
                        val recordContainer by animateColorAsState(
                            targetValue = if (uiState.isRecording) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            label = "recordContainerColor"
                        )
                        val recordContent by animateColorAsState(
                            targetValue = if (uiState.isRecording) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            label = "recordContentColor"
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                            val recordingAspectOption = uiState.aspectRatio
                                            val preferredVideoAspect = if (
                                                recordingAspectOption == AspectRatioOption.RATIO_16_9 ||
                                                recordingAspectOption == AspectRatioOption.FULLSCREEN
                                            ) {
                                                AspectRatio.RATIO_16_9
                                            } else {
                                                AspectRatio.RATIO_4_3
                                            }
                                            val preservedZoomRatio = activeCamera?.cameraInfo?.zoomState?.value?.zoomRatio
                                            val attempts = buildList {
                                                add(
                                                    RecordingSetup(
                                                        aspectRatio = preferredVideoAspect,
                                                        frameRate = requestedFrameRate,
                                                        stabilization = requestedStabilization
                                                    )
                                                )
                                                add(
                                                    RecordingSetup(
                                                        aspectRatio = preferredVideoAspect,
                                                        frameRate = null,
                                                        stabilization = false
                                                    )
                                                )
                                                if (preferredVideoAspect != AspectRatio.RATIO_16_9) {
                                                    add(
                                                        RecordingSetup(
                                                            aspectRatio = AspectRatio.RATIO_16_9,
                                                            frameRate = null,
                                                            stabilization = false
                                                        )
                                                    )
                                                }
                                            }

                                            var recordingCamera: Camera? = null
                                            var recordingUseCase: VideoCapture<Recorder>? = null
                                            for (setup in attempts) {
                                                val previewUseCase = buildPreviewUseCase(
                                                    previewView = previewView,
                                                    faceZoomTracker = faceZoomTracker,
                                                    aspectRatio = setup.aspectRatio
                                                )
                                                val videoUseCase = buildVideoCaptureUseCase(
                                                    quality = uiState.effectiveVideoQuality.cameraQuality,
                                                    aspectRatio = setup.aspectRatio,
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
                                                    val appliedZoom = configureCameraZoomAndExposure(
                                                        camera = camera,
                                                        viewModel = viewModel,
                                                        targetExposureIndex = uiState.exposureIndex,
                                                        preserveZoomRatio = preservedZoomRatio
                                                    )
                                                    currentZoomRatio = appliedZoom.current
                                                    minZoomRatio = appliedZoom.min
                                                    maxZoomRatio = appliedZoom.max
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
                                                    aspectRatio = recordingAspectOption,
                                                    viewModel = viewModel,
                                                    onNavigateToVideoPreview = onNavigateToVideoPreview,
                                                    onRecordingStarted = {
                                                        recordingStartElapsedMs = SystemClock.elapsedRealtime()
                                                        val isFront = uiState.lensFacing == CameraSelector.LENS_FACING_FRONT
                                                        val initialX = if (uiState.isFaceAutoZoomEnabled) {
                                                            if (isFront) 1f - facePanNormX else facePanNormX
                                                        } else {
                                                            0.5f
                                                        }
                                                        val initialY = if (uiState.isFaceAutoZoomEnabled) facePanNormY else 0.5f
                                                        val initialZoom = if (uiState.isFaceAutoZoomEnabled) faceZoomFactor else 1f
                                                        synchronized(recordingZoomSamples) {
                                                            recordingZoomSamples.clear()
                                                            recordingZoomSamples.add(
                                                                FaceZoomSample(
                                                                    timeMs = 0L,
                                                                    zoom = initialZoom,
                                                                    normX = initialX,
                                                                    normY = initialY
                                                                )
                                                            )
                                                        }
                                                    },
                                                    provideZoomSamples = {
                                                        synchronized(recordingZoomSamples) {
                                                            recordingZoomSamples.toList()
                                                        }
                                                    },
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
                                modifier = Modifier.size(52.dp),
                                shape = RoundedCornerShape(recordCorner),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = recordContainer,
                                    contentColor = recordContent
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
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.isRecording) "Stop" else "Video",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isRecording) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isSettingsOpen) {
        SettingsSheet(
            selectedAspectRatio = uiState.aspectRatio,
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
            onSelectAspectRatio = { viewModel.selectAspectRatio(it) },
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
private fun TopBarAspectRatioButton(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        },
        label = "aspectRatioButtonContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        label = "aspectRatioButtonContent"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) containerColor else Color.Transparent,
        modifier = Modifier
            .height(40.dp)
            .defaultMinSize(minWidth = 44.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    contentColor
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun AspectRatioSelectorBar(
    selectedRatio: AspectRatioOption,
    onSelectRatio: (AspectRatioOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 6.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AspectRatioOption.entries.forEach { option ->
                val isSelected = option == selectedRatio
                val chipContainer by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    label = "aspectRatioChipContainer"
                )
                val chipContent by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    label = "aspectRatioChipContent"
                )

                Surface(
                    onClick = { onSelectRatio(option) },
                    shape = RoundedCornerShape(18.dp),
                    color = chipContainer,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = chipContent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarToggleIcon(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        label = "topBarIconContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "topBarIconContent"
    )

    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Single zoom button ("1x") that expands into a curved tick-mark ruler dial when pressed or dragged.
 */
@Composable
private fun ExpandableZoomDialControl(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    expanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestZoom by rememberUpdatedState(currentZoom)
    val latestOnZoomChange by rememberUpdatedState(onZoomChange)
    val latestOnExpand by rememberUpdatedState(onExpand)

    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.92f))
                .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.92f))
        },
        label = "zoomDialTransition",
        modifier = modifier
    ) { isExpanded ->
        if (!isExpanded) {
            val formattedZoom = formatCompactZoom(currentZoom)
            Surface(
                onClick = onExpand,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .height(44.dp)
                    .defaultMinSize(minWidth = 52.dp)
                    .pointerInput(minZoom, maxZoom) {
                        detectHorizontalDragGestures(
                            onDragStart = { latestOnExpand() },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val currentFraction = zoomToFraction(latestZoom, minZoom, maxZoom)
                                val deltaFraction = -dragAmount / 480f
                                val nextZoom = fractionToZoom(
                                    (currentFraction + deltaFraction).coerceIn(0f, 1f),
                                    minZoom,
                                    maxZoom
                                )
                                latestOnZoomChange(nextZoom)
                            }
                        )
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = formattedZoom,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        } else {
            ZoomRulerDial(
                currentZoom = currentZoom,
                minZoom = minZoom,
                maxZoom = maxZoom,
                onZoomChange = onZoomChange,
                onCollapse = onCollapse
            )
        }
    }
}

@Composable
private fun ZoomRulerDial(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    onCollapse: () -> Unit
) {
    val density = LocalDensity.current
    val accentColor = MaterialTheme.colorScheme.primary
    val latestZoom by rememberUpdatedState(currentZoom)
    val latestOnZoomChange by rememberUpdatedState(onZoomChange)
    val latestOnCollapse by rememberUpdatedState(onCollapse)

    var dialWidthPx by remember { mutableFloatStateOf(0f) }

    val stopBookmarks = remember(minZoom, maxZoom) {
        val candidates = listOf(1f, 1.5f, 2f, 3f, 5f, 8f, 10f, 15f, maxZoom)
        buildList<Float> {
            candidates.forEach { stop ->
                val clamped = stop.coerceIn(minZoom, maxZoom)
                if (none { existing -> abs(existing - clamped) < 0.22f }) {
                    add(clamped)
                }
            }
        }.sorted()
    }

    val currentFraction = zoomToFraction(currentZoom, minZoom, maxZoom)
    val rulerSpanPx = (dialWidthPx * 1.25f).coerceAtLeast(1f)
    val focalMm = (currentZoom * 26f).roundToInt()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0E1315).copy(alpha = 0.84f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .onSizeChanged { size ->
                dialWidthPx = size.width.toFloat()
            }
            .pointerInput(minZoom, maxZoom) {
                detectTapGestures { tapOffset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val span = width * 1.25f
                    val dxFromCenter = tapOffset.x - (width / 2f)
                    if (abs(dxFromCenter) < with(density) { 28.dp.toPx() }) {
                        latestOnCollapse()
                    } else {
                        val baseFraction = zoomToFraction(latestZoom, minZoom, maxZoom)
                        val tappedFraction = (baseFraction + (dxFromCenter / span)).coerceIn(0f, 1f)
                        val rawTappedZoom = fractionToZoom(tappedFraction, minZoom, maxZoom)
                        val nearestStop = stopBookmarks.minByOrNull { abs(it - rawTappedZoom) }
                        val targetZoom = if (nearestStop != null && abs(nearestStop - rawTappedZoom) / nearestStop < 0.16f) {
                            nearestStop
                        } else {
                            rawTappedZoom
                        }
                        latestOnZoomChange(targetZoom)
                    }
                }
            }
            .pointerInput(minZoom, maxZoom) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val span = width * 1.25f
                    val baseFraction = zoomToFraction(latestZoom, minZoom, maxZoom)
                    val nextFraction = (baseFraction - (dragAmount / span)).coerceIn(0f, 1f)
                    val rawZoom = fractionToZoom(nextFraction, minZoom, maxZoom)
                    val snappedZoom = stopBookmarks.firstOrNull { stop ->
                        abs(rawZoom - stop) < 0.025f * stop
                    } ?: rawZoom
                    latestOnZoomChange(snappedZoom)
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (dialWidthPx > 0f) {
                val halfWidthPx = dialWidthPx / 2f
                val centerClearancePx = with(density) { 46.dp.toPx() }
                val maxArcPx = with(density) { 6.dp.toPx() }

                stopBookmarks.forEach { stop ->
                    val stopFraction = zoomToFraction(stop, minZoom, maxZoom)
                    val dxPx = (stopFraction - currentFraction) * rulerSpanPx
                    if (abs(dxPx) > centerClearancePx && abs(dxPx) < halfWidthPx - with(density) { 18.dp.toPx() }) {
                        val nx = (dxPx / halfWidthPx).coerceIn(-1f, 1f)
                        val edgeAlpha = (1f - 0.7f * nx * nx).coerceIn(0f, 1f)
                        val centerFade = ((abs(dxPx) - centerClearancePx) / with(density) { 18.dp.toPx() })
                            .coerceIn(0f, 1f)
                        val combinedAlpha = edgeAlpha * centerFade
                        val arcOffsetY = -(nx * nx * maxArcPx).roundToInt()
                        val stopMm = (stop * 26f).roundToInt()

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset {
                                    IntOffset(
                                        x = dxPx.roundToInt(),
                                        y = with(density) { 10.dp.roundToPx() } + arcOffsetY
                                    )
                                }
                                .graphicsLayer {
                                    alpha = combinedAlpha
                                }
                        ) {
                            Text(
                                text = formatBookmarkStop(stop),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "${stopMm}mm",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.1fx", currentZoom).replace(".0x", "x"),
                    color = accentColor,
                    fontSize = 19.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "${focalMm}mm",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .align(Alignment.BottomCenter)
            ) {
                val width = size.width
                val height = size.height
                val centerX = width / 2f
                val halfWidth = (width / 2f).coerceAtLeast(1f)
                val span = (width * 1.25f).coerceAtLeast(1f)
                val maxArc = 7.dp.toPx()
                val baseBottomY = height - 8.dp.toPx()
                val minorTickHeight = 10.dp.toPx()
                val majorTickHeight = 15.dp.toPx()
                val centerTickHeight = 20.dp.toPx()

                val totalTicks = 60
                for (i in 0..totalTicks) {
                    val tickFraction = i.toFloat() / totalTicks.toFloat()
                    val x = centerX + (tickFraction - currentFraction) * span
                    if (x < 8.dp.toPx() || x > width - 8.dp.toPx()) continue

                    val dx = x - centerX
                    val nx = (dx / halfWidth).coerceIn(-1f, 1f)
                    val alpha = (1f - 0.82f * nx * nx).coerceIn(0.08f, 1f)
                    val curveY = -(nx * nx) * maxArc

                    val tickZoom = fractionToZoom(tickFraction, minZoom, maxZoom)
                    val isNearStop = stopBookmarks.any { stop ->
                        abs(tickFraction - zoomToFraction(stop, minZoom, maxZoom)) < (0.5f / totalTicks)
                    } || i % 10 == 0

                    val tickH = if (isNearStop) majorTickHeight else minorTickHeight
                    val strokeW = if (isNearStop) 2.dp.toPx() else 1.2.dp.toPx()
                    val tickColor = if (isNearStop) {
                        Color.White.copy(alpha = (alpha * 0.9f).coerceIn(0f, 1f))
                    } else {
                        Color.White.copy(alpha = (alpha * 0.42f).coerceIn(0f, 1f))
                    }

                    val bottomY = baseBottomY + curveY
                    val topY = bottomY - tickH

                    drawLine(
                        color = tickColor,
                        start = Offset(x, topY),
                        end = Offset(x, bottomY),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }

                drawLine(
                    color = accentColor,
                    start = Offset(centerX, baseBottomY - centerTickHeight),
                    end = Offset(centerX, baseBottomY + 2.dp.toPx()),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, baseBottomY - centerTickHeight + 2.dp.toPx()),
                    end = Offset(centerX, baseBottomY),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun zoomToFraction(zoom: Float, minZoom: Float, maxZoom: Float): Float {
    val safeMin = minZoom.coerceAtLeast(0.1f)
    val safeMax = maxZoom.coerceAtLeast(safeMin + 0.01f)
    val clamped = zoom.coerceIn(safeMin, safeMax)
    return (ln(clamped / safeMin) / ln(safeMax / safeMin)).coerceIn(0f, 1f)
}

private fun fractionToZoom(fraction: Float, minZoom: Float, maxZoom: Float): Float {
    val safeMin = minZoom.coerceAtLeast(0.1f)
    val safeMax = maxZoom.coerceAtLeast(safeMin + 0.01f)
    val clamped = fraction.coerceIn(0f, 1f)
    return (safeMin * (safeMax / safeMin).pow(clamped)).coerceIn(safeMin, safeMax)
}

private fun formatCompactZoom(zoom: Float): String {
    val roundedTenths = (zoom * 10f).roundToInt() / 10f
    return if (abs(roundedTenths - roundedTenths.roundToInt()) < 0.05f) {
        "${roundedTenths.roundToInt()}x"
    } else {
        String.format(Locale.US, "%.1fx", roundedTenths)
    }
}

private fun formatBookmarkStop(stop: Float): String {
    val roundedTenths = (stop * 10f).roundToInt() / 10f
    return if (abs(roundedTenths - roundedTenths.roundToInt()) < 0.05f) {
        roundedTenths.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.1f", roundedTenths)
    }
}

@Composable
private fun CameraPermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 4.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(38.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% Private | Never saved to storage",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Mirror needs camera access to show your live reflection and let you inspect photos and videos. Everything stays in temporary memory and is never saved to your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = onRequestPermission,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Grant Camera Permission",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = onOpenAppSettings,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Open App Settings",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun startVideoRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    mirrorOutput: Boolean,
    aspectRatio: AspectRatioOption,
    viewModel: MirrorViewModel,
    onNavigateToVideoPreview: () -> Unit,
    onRecordingStarted: () -> Unit,
    provideZoomSamples: () -> List<FaceZoomSample>,
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
                onRecordingStarted()
                viewModel.onRecordingStarted()
            }
            is VideoRecordEvent.Finalize -> {
                onRecordingFinished()
                if (!event.hasError() || (outputFile.exists() && outputFile.length() > 0L)) {
                    viewModel.onVideoCaptured(
                        file = outputFile,
                        mirrored = mirrorOutput,
                        zoomSamples = provideZoomSamples(),
                        aspectRatio = aspectRatio,
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
    val aspectRatio: Int,
    val frameRate: Int?,
    val stabilization: Boolean
)

private data class AppliedZoomBounds(
    val current: Float,
    val min: Float,
    val max: Float
)

private const val FACE_ZOOM_STEP_MS = 16L
private const val FACE_METER_INTERVAL_MS = 2500L
private const val FACE_METER_MOVE_THRESHOLD = 0.1f
private const val MIN_FACE_SCORE = 25

private fun applyPreviewTransform(
    view: PreviewView,
    flipScaleX: Float,
    zoom: Float,
    previewNormX: Float,
    previewNormY: Float
) {
    val activeZoom = zoom.coerceIn(1f, FaceZoomTracker.MAX_FACE_ZOOM)
    view.scaleX = flipScaleX * activeZoom
    view.scaleY = activeZoom

    val viewWidth = view.width.toFloat()
    val viewHeight = view.height.toFloat()
    if (viewWidth > 0f && viewHeight > 0f && activeZoom > 1.001f) {
        val maxTx = (activeZoom - 1f) * viewWidth * 0.5f
        val maxTy = (activeZoom - 1f) * viewHeight * 0.5f
        val desiredTx = (0.5f - previewNormX) * viewWidth * view.scaleX
        val desiredTy = (0.5f - previewNormY) * viewHeight * activeZoom
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
    faceZoomTracker: FaceZoomTracker,
    aspectRatio: Int = AspectRatio.RATIO_4_3
): Preview {
    val strategy = if (aspectRatio == AspectRatio.RATIO_16_9) {
        AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
    } else {
        AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
    }
    val resolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(strategy)
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
    aspectRatio: Int,
    frameRate: Int?,
    stabilization: Boolean
): VideoCapture<Recorder> {
    val qualitySelector = QualitySelector.from(
        quality,
        FallbackStrategy.higherQualityOrLowerThan(quality)
    )
    val recorder = Recorder.Builder()
        .setQualitySelector(qualitySelector)
        .setAspectRatio(aspectRatio)
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
): AppliedZoomBounds {
    val zoomState = camera.cameraInfo.zoomState.value
    val minZoom = zoomState?.minZoomRatio ?: 1f
    val maxZoom = zoomState?.maxZoomRatio ?: 1f
    val appliedZoom = if (preserveZoomRatio != null && zoomState != null) {
        preserveZoomRatio.coerceIn(minZoom, maxZoom).also {
            camera.cameraControl.setZoomRatio(it)
        }
    } else if (zoomState != null) {
        minZoom.also {
            camera.cameraControl.setZoomRatio(it)
        }
    } else {
        camera.cameraControl.setLinearZoom(0f)
        1f
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
    return AppliedZoomBounds(
        current = appliedZoom,
        min = minZoom,
        max = maxZoom
    )
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
