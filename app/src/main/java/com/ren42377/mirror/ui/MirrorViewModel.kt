package com.ren42377.mirror.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ren42377.mirror.camera.CameraCapabilities
import com.ren42377.mirror.data.RecordingCacheManager
import com.ren42377.mirror.data.SettingsRepository
import com.ren42377.mirror.model.AppSettings
import com.ren42377.mirror.model.AspectRatioOption
import com.ren42377.mirror.model.CaptureResult
import com.ren42377.mirror.model.FaceZoomSample
import com.ren42377.mirror.model.VideoQualityOption
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MirrorViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(MirrorUiState())
    val uiState: StateFlow<MirrorUiState> = _uiState.asStateFlow()

    private val unsupportedPhotoSizes = mutableSetOf<String>()

    private val _captureResult = MutableStateFlow<CaptureResult?>(null)
    val captureResult: StateFlow<CaptureResult?> = _captureResult.asStateFlow()

    init {
        RecordingCacheManager.clearAllRecordings(application.applicationContext)
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { appSettings ->
                _uiState.update { current ->
                    current.copy(settings = appSettings)
                }
            }
        }
    }

    fun toggleFlip() {
        _uiState.update { current ->
            if (current.isRecording) {
                current
            } else {
                current.copy(isFlipped = !current.isFlipped)
            }
        }
    }

    fun switchCamera() {
        _uiState.update { current ->
            if (current.isRecording || !current.hasMultipleCameras) {
                current
            } else {
                val nextLens = if (current.lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                current.copy(
                    lensFacing = nextLens,
                    isFlipped = false,
                    exposureIndex = 0,
                    isExposureSliderVisible = false,
                    isAspectRatioBarVisible = false,
                    isFaceDetected = false
                )
            }
        }
    }

    fun toggleRingLight() {
        _uiState.update { current ->
            current.copy(isRingLightEnabled = !current.isRingLightEnabled)
        }
    }

    fun toggleFaceAutoZoom() {
        _uiState.update { current ->
            val nextEnabled = !current.isFaceAutoZoomEnabled
            if (nextEnabled && !current.isFaceTrackingSupported) {
                current
            } else {
                current.copy(
                    isFaceAutoZoomEnabled = nextEnabled,
                    isFaceDetected = false
                )
            }
        }
    }

    fun setFaceDetected(isDetected: Boolean) {
        _uiState.update { current ->
            if (current.isFaceDetected == isDetected) current else current.copy(isFaceDetected = isDetected)
        }
    }

    fun setLensFacingIfUnavailable(hasFront: Boolean, hasBack: Boolean) {
        _uiState.update { current ->
            val hasMultiple = hasFront && hasBack
            val resolvedLens = when {
                current.lensFacing == CameraSelector.LENS_FACING_FRONT && !hasFront && hasBack ->
                    CameraSelector.LENS_FACING_BACK
                current.lensFacing == CameraSelector.LENS_FACING_BACK && !hasBack && hasFront ->
                    CameraSelector.LENS_FACING_FRONT
                else -> current.lensFacing
            }
            current.copy(
                lensFacing = resolvedLens,
                hasMultipleCameras = hasMultiple
            )
        }
    }

    fun updateActiveCameraCapabilities(capabilities: CameraCapabilities, lensFacing: Int) {
        _uiState.update { current ->
            val usablePhotoSizes = capabilities.photoSizes.filterNot {
                unsupportedPhotoSizes.contains(photoSizeKey(lensFacing, it))
            }
            val faceTrackingSupported = capabilities.isFaceTrackingSupported
            current.copy(
                supportedPhotoSizes = usablePhotoSizes,
                highResolutionPhotoSizes = capabilities.highResolutionPhotoSizes,
                videoCapabilities = capabilities.videoModes,
                isVideoStabilizationSupported = capabilities.isVideoStabilizationSupported,
                isOpticalStabilizationSupported = capabilities.isOpticalStabilizationSupported,
                isFaceTrackingSupported = faceTrackingSupported,
                isFaceAutoZoomEnabled = current.isFaceAutoZoomEnabled && faceTrackingSupported,
                isFaceDetected = current.isFaceDetected && faceTrackingSupported
            )
        }
    }

    /**
     * Remembers that the camera could not deliver [size] when it was requested, so the size is
     * no longer offered for this lens.
     */
    fun markPhotoSizeUnsupported(lensFacing: Int, size: Size) {
        unsupportedPhotoSizes.add(photoSizeKey(lensFacing, size))
        _uiState.update { current ->
            current.copy(supportedPhotoSizes = current.supportedPhotoSizes.filterNot { it == size })
        }
    }

    fun updateExposureRange(min: Int, max: Int, current: Int) {
        _uiState.update { state ->
            val safeMin = min.coerceAtMost(max)
            val safeMax = max.coerceAtLeast(min)
            state.copy(
                minExposureIndex = safeMin,
                maxExposureIndex = safeMax,
                exposureIndex = current.coerceIn(safeMin, safeMax)
            )
        }
    }

    fun setExposureIndex(index: Int) {
        _uiState.update { state ->
            if (!state.hasExposureControl) {
                state
            } else {
                state.copy(
                    exposureIndex = index.coerceIn(state.minExposureIndex, state.maxExposureIndex)
                )
            }
        }
    }

    fun toggleAspectRatioBar() {
        _uiState.update { state ->
            if (state.isRecording) {
                state
            } else {
                val nextVisible = !state.isAspectRatioBarVisible
                state.copy(
                    isAspectRatioBarVisible = nextVisible,
                    isExposureSliderVisible = if (nextVisible) false else state.isExposureSliderVisible
                )
            }
        }
    }

    fun selectAspectRatio(option: AspectRatioOption) {
        _uiState.update { current ->
            current.copy(
                settings = current.settings.copy(aspectRatio = option.key),
                isAspectRatioBarVisible = false
            )
        }
        viewModelScope.launch {
            settingsRepository.setAspectRatio(option.key)
        }
    }

    fun toggleExposureSlider() {
        _uiState.update { state ->
            if (!state.hasExposureControl) {
                state
            } else {
                val nextVisible = !state.isExposureSliderVisible
                state.copy(
                    isExposureSliderVisible = nextVisible,
                    isAspectRatioBarVisible = if (nextVisible) false else state.isAspectRatioBarVisible
                )
            }
        }
    }

    fun openSettingsSheet() {
        if (!_uiState.value.isRecording) {
            _uiState.update {
                it.copy(
                    isSettingsOpen = true,
                    isExposureSliderVisible = false,
                    isAspectRatioBarVisible = false
                )
            }
        }
    }

    fun closeSettingsSheet() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun selectPhotoResolution(size: Size) {
        viewModelScope.launch {
            settingsRepository.setPhotoResolution(AppSettings.formatSize(size))
        }
    }

    fun selectVideoQuality(option: VideoQualityOption) {
        viewModelScope.launch {
            settingsRepository.setVideoQuality(option.key)
        }
    }

    fun selectVideoFrameRate(frameRate: Int) {
        viewModelScope.launch {
            settingsRepository.setVideoFrameRate(frameRate)
        }
    }

    fun setVideoStabilization(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVideoStabilization(enabled)
        }
    }

    fun setOpticalStabilization(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOpticalStabilization(enabled)
        }
    }

    fun setCapturingPhoto(isCapturing: Boolean) {
        _uiState.update { it.copy(isCapturingPhoto = isCapturing) }
    }

    fun processCapturedPhoto(
        sourceBitmap: Bitmap,
        rotationDegrees: Int,
        mirrorHorizontally: Boolean,
        cropZoom: Float = 1f,
        cropCenterX: Float = 0.5f,
        cropCenterY: Float = 0.5f,
        targetAspectRatio: Float? = null,
        aspectRatioOption: AspectRatioOption = _uiState.value.aspectRatio,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val transformed = withContext(Dispatchers.Default) {
                transformBitmap(
                    source = sourceBitmap,
                    rotationDegrees = rotationDegrees,
                    mirrorHorizontally = mirrorHorizontally,
                    cropZoom = cropZoom,
                    cropCenterX = cropCenterX,
                    cropCenterY = cropCenterY,
                    targetAspectRatio = targetAspectRatio
                )
            }
            clearCaptureResult()
            _captureResult.value = CaptureResult.Photo(
                bitmap = transformed,
                aspectRatio = aspectRatioOption
            )
            _uiState.update { it.copy(isCapturingPhoto = false) }
            onComplete()
        }
    }

    fun onRecordingStarted() {
        _uiState.update {
            it.copy(
                isRecording = true,
                recordingSeconds = 0L,
                isSettingsOpen = false,
                isAspectRatioBarVisible = false
            )
        }
    }

    fun onRecordingTick(seconds: Long) {
        _uiState.update { current ->
            if (current.isRecording) {
                current.copy(recordingSeconds = seconds)
            } else {
                current
            }
        }
    }

    fun onRecordingStopped() {
        _uiState.update {
            it.copy(
                isRecording = false,
                recordingSeconds = 0L
            )
        }
    }

    fun onVideoCaptured(
        file: File,
        mirrored: Boolean,
        zoomSamples: List<FaceZoomSample> = emptyList(),
        aspectRatio: AspectRatioOption = _uiState.value.aspectRatio,
        onComplete: () -> Unit
    ) {
        clearCaptureResult(keepFile = file)
        _captureResult.value = CaptureResult.Video(
            file = file,
            mirrored = mirrored,
            zoomSamples = zoomSamples,
            aspectRatio = aspectRatio
        )
        _uiState.update {
            it.copy(
                isRecording = false,
                recordingSeconds = 0L
            )
        }
        onComplete()
    }

    fun clearCaptureResult(keepFile: File? = null) {
        val previous = _captureResult.value
        _captureResult.value = null
        if (previous is CaptureResult.Video && previous.file != keepFile) {
            RecordingCacheManager.deleteRecording(previous.file)
        }
        if (keepFile == null) {
            RecordingCacheManager.clearAllRecordings(getApplication())
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearCaptureResult()
    }

    private fun photoSizeKey(lensFacing: Int, size: Size): String {
        return "$lensFacing:${size.width}x${size.height}"
    }

    companion object {
        fun transformBitmap(
            source: Bitmap,
            rotationDegrees: Int,
            mirrorHorizontally: Boolean,
            cropZoom: Float = 1f,
            cropCenterX: Float = 0.5f,
            cropCenterY: Float = 0.5f,
            targetAspectRatio: Float? = null
        ): Bitmap {
            val matrix = Matrix()
            if (rotationDegrees != 0) {
                matrix.postRotate(rotationDegrees.toFloat())
            }
            if (mirrorHorizontally) {
                matrix.postScale(-1f, 1f)
            }
            val oriented = if (rotationDegrees == 0 && !mirrorHorizontally) {
                source
            } else {
                Bitmap.createBitmap(
                    source,
                    0,
                    0,
                    source.width,
                    source.height,
                    matrix,
                    true
                ).also { created ->
                    if (created !== source) {
                        source.recycle()
                    }
                }
            }

            val currentRatio = oriented.width.toFloat() / oriented.height.coerceAtLeast(1).toFloat()
            val desiredRatio = targetAspectRatio?.takeIf { it > 0f } ?: currentRatio
            val (baseWidth, baseHeight) = if (abs(currentRatio - desiredRatio) <= 0.005f) {
                oriented.width to oriented.height
            } else if (currentRatio > desiredRatio) {
                val croppedW = (oriented.height * desiredRatio).roundToInt().coerceIn(1, oriented.width)
                croppedW to oriented.height
            } else {
                val croppedH = (oriented.width / desiredRatio).roundToInt().coerceIn(1, oriented.height)
                oriented.width to croppedH
            }
            val baseLeft = (oriented.width - baseWidth) / 2
            val baseTop = (oriented.height - baseHeight) / 2

            val effectiveZoom = cropZoom.coerceAtLeast(1f)
            val cropWidth = if (effectiveZoom <= 1.01f) {
                baseWidth
            } else {
                (baseWidth / effectiveZoom).roundToInt().coerceIn(1, baseWidth)
            }
            val cropHeight = if (effectiveZoom <= 1.01f) {
                baseHeight
            } else {
                (baseHeight / effectiveZoom).roundToInt().coerceIn(1, baseHeight)
            }
            val cropLeft = if (effectiveZoom <= 1.01f) {
                baseLeft
            } else {
                val cx = cropCenterX.coerceIn(0f, 1f) * baseWidth
                baseLeft + (cx - cropWidth * 0.5f).roundToInt().coerceIn(0, baseWidth - cropWidth)
            }
            val cropTop = if (effectiveZoom <= 1.01f) {
                baseTop
            } else {
                val cy = cropCenterY.coerceIn(0f, 1f) * baseHeight
                baseTop + (cy - cropHeight * 0.5f).roundToInt().coerceIn(0, baseHeight - cropHeight)
            }

            if (cropLeft == 0 && cropTop == 0 && cropWidth == oriented.width && cropHeight == oriented.height) {
                return oriented
            }
            val cropped = Bitmap.createBitmap(oriented, cropLeft, cropTop, cropWidth, cropHeight)
            if (cropped !== oriented) {
                oriented.recycle()
            }
            return cropped
        }
    }
}
