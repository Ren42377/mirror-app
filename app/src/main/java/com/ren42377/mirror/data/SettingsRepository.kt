package com.ren42377.mirror.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ren42377.mirror.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mirror_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PHOTO_RESOLUTION = stringPreferencesKey("photo_resolution")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            photoResolution = preferences[Keys.PHOTO_RESOLUTION] ?: "",
            videoQuality = preferences[Keys.VIDEO_QUALITY] ?: "FHD"
        )
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
}
