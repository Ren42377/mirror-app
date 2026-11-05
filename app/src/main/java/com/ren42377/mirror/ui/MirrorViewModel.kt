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
import com.ren42377.mirror.model.CaptureResult
import com.ren42377.mirror.model.VideoQualityOption
import java.io.File
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

    fun toggleExposureSlider() {
        _uiState.update { state ->
            if (!state.hasExposureControl) {
                state
            } else {
                state.copy(isExposureSliderVisible = !state.isExposureSliderVisible)
            }
        }
    }

    fun openSettingsSheet() {
        if (!_uiState.value.isRecording) {
            _uiState.update {
                it.copy(
                    isSettingsOpen = true,
                    isExposureSliderVisible = false
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
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val transformed = withContext(Dispatchers.Default) {
                transformBitmap(sourceBitmap, rotationDegrees, mirrorHorizontally)
            }
            clearCaptureResult()
            _captureResult.value = CaptureResult.Photo(transformed)
            _uiState.update { it.copy(isCapturingPhoto = false) }
            onComplete()
        }
    }

    fun onRecordingStarted() {
        _uiState.update {
            it.copy(
                isRecording = true,
                recordingSeconds = 0L,
                isSettingsOpen = false
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

    fun onVideoCaptured(file: File, mirrored: Boolean, onComplete: () -> Unit) {
        clearCaptureResult(keepFile = file)
        _captureResult.value = CaptureResult.Video(file = file, mirrored = mirrored)
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
            mirrorHorizontally: Boolean
        ): Bitmap {
            val matrix = Matrix()
            if (rotationDegrees != 0) {
                matrix.postRotate(rotationDegrees.toFloat())
            }
            if (mirrorHorizontally) {
                matrix.postScale(-1f, 1f)
            }
            if (rotationDegrees == 0 && !mirrorHorizontally) {
                return source
            }
            val result = Bitmap.createBitmap(
                source,
                0,
                0,
                source.width,
                source.height,
                matrix,
                true
            )
            if (result !== source) {
                source.recycle()
            }
            return result
        }
    }
}
