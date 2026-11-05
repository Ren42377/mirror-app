package com.ren42377.mirror.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ren42377.mirror.model.AppSettings
import com.ren42377.mirror.model.AspectRatioOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mirror_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        val PHOTO_RESOLUTION = stringPreferencesKey("photo_resolution")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val VIDEO_FRAME_RATE = intPreferencesKey("video_frame_rate")
        val VIDEO_STABILIZATION = booleanPreferencesKey("video_stabilization")
        val OPTICAL_STABILIZATION = booleanPreferencesKey("optical_stabilization")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            aspectRatio = preferences[Keys.ASPECT_RATIO] ?: AspectRatioOption.RATIO_4_3.key,
            photoResolution = preferences[Keys.PHOTO_RESOLUTION] ?: "",
            videoQuality = preferences[Keys.VIDEO_QUALITY] ?: "FHD",
            videoFrameRate = preferences[Keys.VIDEO_FRAME_RATE] ?: 30,
            videoStabilization = preferences[Keys.VIDEO_STABILIZATION] ?: false,
            opticalStabilization = preferences[Keys.OPTICAL_STABILIZATION] ?: false
        )
    }

    suspend fun setAspectRatio(aspectRatioKey: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ASPECT_RATIO] = aspectRatioKey
        }
    }

    suspend fun setPhotoResolution(resolution: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PHOTO_RESOLUTION] = resolution
        }
    }

    suspend fun setVideoQuality(qualityKey: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.VIDEO_QUALITY] = qualityKey
        }
    }

    suspend fun setVideoFrameRate(frameRate: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.VIDEO_FRAME_RATE] = frameRate
        }
    }

    suspend fun setVideoStabilization(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.VIDEO_STABILIZATION] = enabled
        }
    }

    suspend fun setOpticalStabilization(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.OPTICAL_STABILIZATION] = enabled
        }
    }
}
