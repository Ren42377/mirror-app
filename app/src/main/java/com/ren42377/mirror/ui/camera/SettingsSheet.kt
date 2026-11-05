package com.ren42377.mirror.ui.camera

import android.util.Size
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EightMp
import androidx.compose.material.icons.filled.EighteenMp
import androidx.compose.material.icons.filled.ElevenMp
import androidx.compose.material.icons.filled.FifteenMp
import androidx.compose.material.icons.filled.FiveMp
import androidx.compose.material.icons.filled.FourMp
import androidx.compose.material.icons.filled.FourteenMp
import androidx.compose.material.icons.filled.NineMp
import androidx.compose.material.icons.filled.NineteenMp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SevenMp
import androidx.compose.material.icons.filled.SeventeenMp
import androidx.compose.material.icons.filled.SixMp
import androidx.compose.material.icons.filled.SixteenMp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TenMp
import androidx.compose.material.icons.filled.ThirteenMp
import androidx.compose.material.icons.filled.ThreeMp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TwelveMp
import androidx.compose.material.icons.filled.TwentyFourMp
import androidx.compose.material.icons.filled.TwentyMp
import androidx.compose.material.icons.filled.TwentyOneMp
import androidx.compose.material.icons.filled.TwentyThreeMp
import androidx.compose.material.icons.filled.TwentyTwoMp
import androidx.compose.material.icons.filled.TwoMp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ren42377.mirror.camera.curatePhotoSizes
import com.ren42377.mirror.model.VideoModeCapability
import com.ren42377.mirror.model.VideoQualityOption
import kotlin.math.roundToInt

private const val GRID_COLUMNS = 3

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

    val gridPhotoSizes = remember(supportedPhotoSizes, selectedPhotoSize) {
        val curated = curatePhotoSizes(supportedPhotoSizes)
        if (selectedPhotoSize != null && supportedPhotoSizes.contains(selectedPhotoSize) && !curated.contains(selectedPhotoSize)) {
            (curated.dropLast(1) + selectedPhotoSize)
                .distinct()
                .sortedByDescending { it.width.toLong() * it.height.toLong() }
        } else {
            curated
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Camera Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Photo resolution, video quality, and stabilization",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Default.PhotoCamera,
                        title = "Photo Resolution",
                        trailingBadge = selectedPhotoSize?.let { "${it.width} x ${it.height}" }
                    )

                    if (gridPhotoSizes.isEmpty()) {
                        Text(
                            text = "Default device resolution",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        val rows = remember(gridPhotoSizes) {
                            gridPhotoSizes.chunked(GRID_COLUMNS)
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rows.forEach { rowSizes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowSizes.forEach { size ->
                                        PhotoResolutionTile(
                                            size = size,
                                            isSelected = selectedPhotoSize == size,
                                            isHighRes = highResolutionPhotoSizes.contains(size),
                                            onClick = { onSelectPhotoSize(size) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(GRID_COLUMNS - rowSizes.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Default.Videocam,
                        title = "Video Capture",
                        trailingBadge = selectedMode?.resolution?.let { "${it.width} x ${it.height}" }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Quality",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (videoModes.isEmpty()) {
                        Text(
                            text = "Default device quality",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            videoModes.forEach { mode ->
                                val isSelected = mode.quality == selectedVideoQuality
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectVideoQuality(mode.quality) },
                                    label = { Text(text = mode.quality.label) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Frame Rate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        frameRateOptions.forEach { rate ->
                            val isSelected = rate == selectedFrameRate
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectFrameRate(rate) },
                                label = { Text(text = "$rate fps") },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else {
                                    null
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    if (isVideoStabilizationSupported || isOpticalStabilizationSupported) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (isVideoStabilizationSupported) {
                        SwitchRow(
                            icon = Icons.Default.Videocam,
                            title = "Video Stabilization (EIS)",
                            description = "Electronic smoothing for handheld video",
                            checked = isVideoStabilizationEnabled,
                            onCheckedChange = onToggleVideoStabilization
                        )
                    }

                    if (isOpticalStabilizationSupported) {
                        SwitchRow(
                            icon = Icons.Default.CenterFocusStrong,
                            title = "Optical Stabilization (OIS)",
                            description = "Hardware lens shake reduction",
                            checked = isOpticalStabilizationEnabled,
                            onCheckedChange = onToggleOpticalStabilization
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoResolutionTile(
    size: Size,
    isSelected: Boolean,
    isHighRes: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val megapixels = remember(size) {
        ((size.width.toLong() * size.height.toLong()) / 1_000_000.0)
            .roundToInt()
            .coerceAtLeast(1)
    }

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "photoTileContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        },
        label = "photoTileBorder"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "photoTileIcon"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "photoTileText"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MegapixelBadgeIcon(
                megapixels = megapixels,
                tint = iconColor,
                cutoutTextColor = containerColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${size.width} x ${size.height}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (isHighRes) {
                Text(
                    text = "High-Res",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MegapixelBadgeIcon(
    megapixels: Int,
    tint: Color,
    cutoutTextColor: Color
) {
    val vectorIcon = megapixelVectorIcon(megapixels)
    if (vectorIcon != null) {
        Icon(
            imageVector = vectorIcon,
            contentDescription = "$megapixels MP",
            tint = tint,
            modifier = Modifier.size(30.dp)
        )
    } else {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(color = tint, shape = RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = megapixels.toString(),
                    color = cutoutTextColor,
                    fontSize = 9.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "MP",
                    color = cutoutTextColor,
                    fontSize = 7.sp,
                    lineHeight = 7.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun megapixelVectorIcon(megapixels: Int): ImageVector? {
    return when (megapixels) {
        2 -> Icons.Default.TwoMp
        3 -> Icons.Default.ThreeMp
        4 -> Icons.Default.FourMp
        5 -> Icons.Default.FiveMp
        6 -> Icons.Default.SixMp
        7 -> Icons.Default.SevenMp
        8 -> Icons.Default.EightMp
        9 -> Icons.Default.NineMp
        10 -> Icons.Default.TenMp
        11 -> Icons.Default.ElevenMp
        12 -> Icons.Default.TwelveMp
        13 -> Icons.Default.ThirteenMp
        14 -> Icons.Default.FourteenMp
        15 -> Icons.Default.FifteenMp
        16 -> Icons.Default.SixteenMp
        17 -> Icons.Default.SeventeenMp
        18 -> Icons.Default.EighteenMp
        19 -> Icons.Default.NineteenMp
        20 -> Icons.Default.TwentyMp
        21 -> Icons.Default.TwentyOneMp
        22 -> Icons.Default.TwentyTwoMp
        23 -> Icons.Default.TwentyThreeMp
        24 -> Icons.Default.TwentyFourMp
        else -> null
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    trailingBadge: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (trailingBadge != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}
