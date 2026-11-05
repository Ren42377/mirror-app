package com.ren42377.mirror.ui.camera

import android.util.Size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ren42377.mirror.model.VideoModeCapability
import com.ren42377.mirror.model.VideoQualityOption
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    supportedPhotoSizes: List<Size>,
    highResolutionPhotoSizes: Set<Size>,
    selectedPhotoSize: Size?,
    videoModes: List<VideoModeCapability>,
    selectedVideoQuality: VideoQualityOption,
    selectedFrameRate: Int,
    frameRateOptions: List<Int>,
    isVideoStabilizationSupported: Boolean,
    isVideoStabilizationEnabled: Boolean,
    isOpticalStabilizationSupported: Boolean,
    isOpticalStabilizationEnabled: Boolean,
    onSelectPhotoSize: (Size) -> Unit,
    onSelectVideoQuality: (VideoQualityOption) -> Unit,
    onSelectFrameRate: (Int) -> Unit,
    onToggleVideoStabilization: (Boolean) -> Unit,
    onToggleOpticalStabilization: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedMode = videoModes.firstOrNull { it.quality == selectedVideoQuality }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "Camera Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Video Quality",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                if (videoModes.isEmpty()) {
                    Text(
                        text = "Default device quality",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        videoModes.forEach { mode ->
                            FilterChip(
                                selected = mode.quality == selectedVideoQuality,
                                onClick = { onSelectVideoQuality(mode.quality) },
                                label = { Text(text = mode.quality.label) }
                            )
                        }
                    }
                    selectedMode?.resolution?.let { resolution ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${resolution.width} x ${resolution.height}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Frame Rate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    frameRateOptions.forEach { rate ->
                        FilterChip(
                            selected = rate == selectedFrameRate,
                            onClick = { onSelectFrameRate(rate) },
                            label = { Text(text = "$rate fps") }
                        )
                    }
                }
            }

            if (isVideoStabilizationSupported) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SwitchRow(
                        title = "Video Stabilization (EIS)",
                        description = "Smoother video, may slightly narrow the view",
                        checked = isVideoStabilizationEnabled,
                        onCheckedChange = onToggleVideoStabilization
                    )
                }
            }

            if (isOpticalStabilizationSupported) {
                item {
                    SwitchRow(
                        title = "Optical Stabilization (OIS)",
                        description = "Lens based shake reduction",
                        checked = isOpticalStabilizationEnabled,
                        onCheckedChange = onToggleOpticalStabilization
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Photo Resolution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (supportedPhotoSizes.isEmpty()) {
                    Text(
                        text = "Default device resolution",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            items(
                items = supportedPhotoSizes,
                key = { "${it.width}x${it.height}" }
            ) { size ->
                val isSelected = selectedPhotoSize == size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPhotoSize(size) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${size.width} x ${size.height}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            text = formatResolutionDetails(
                                size = size,
                                isHighResolution = highResolutionPhotoSizes.contains(size)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectPhotoSize(size) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun formatResolutionDetails(size: Size, isHighResolution: Boolean): String {
    val megapixels = (size.width.toLong() * size.height.toLong()) / 1_000_000.0
    val mpText = String.format(Locale.US, "%.1f MP", megapixels)
    val gcd = gcd(size.width, size.height)
    val ratioW = size.width / gcd
    val ratioH = size.height / gcd
    val base = if (ratioW <= 32 && ratioH <= 32) {
        "$mpText (${ratioW}:${ratioH})"
    } else {
        val approxRatio = (size.width.toDouble() / size.height.toDouble() * 100).roundToInt() / 100.0
        "$mpText ($approxRatio:1)"
    }
    return if (isHighResolution) "$base, high resolution mode" else base
}

private tailrec fun gcd(a: Int, b: Int): Int {
    return if (b == 0) a else gcd(b, a % b)
}
