package com.elli0t0.gallery.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.galleryDataStore: DataStore<Preferences> by preferencesDataStore(name = "gallery")

object Store {
    private val FAVORITES = stringSetPreferencesKey("favorites")
    private val TRASH = stringSetPreferencesKey("trash")
    private val TAGS = stringSetPreferencesKey("tags")
    private val MOTION_AUTOPLAY = booleanPreferencesKey("motion_autoplay")
    private val ESSENTIAL_ALBUMS = booleanPreferencesKey("essential_albums")
    private val MERGE_ALBUMS = booleanPreferencesKey("merge_albums")

    data class Settings(
        val motionAutoplay: Boolean = true,
        val essentialAlbums: Boolean = true,
        val mergeAlbums: Boolean = false
    )

    fun favorites(context: Context): Flow<Set<String>> =
        context.galleryDataStore.data.map { it[FAVORITES] ?: emptySet() }

    fun trash(context: Context): Flow<Map<String, Long>> =
        context.galleryDataStore.data.map { prefs ->
            (prefs[TRASH] ?: emptySet()).mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size == 2) {
                    val ts = parts[1].toLongOrNull()
                    if (ts != null) parts[0] to ts else null
                } else null
            }.toMap()
        }

    fun tags(context: Context): Flow<Map<String, Set<String>>> =
        context.galleryDataStore.data.map { prefs ->
            val out = mutableMapOf<String, MutableSet<String>>()
            (prefs[TAGS] ?: emptySet()).forEach { entry ->
                val parts = entry.split("|", limit = 2)
                if (parts.size == 2) out.getOrPut(parts[0]) { mutableSetOf() } += parts[1]
            }
            out
        }

    fun settings(context: Context): Flow<Settings> =
        context.galleryDataStore.data.map {
            Settings(
                motionAutoplay = it[MOTION_AUTOPLAY] ?: true,
                essentialAlbums = it[ESSENTIAL_ALBUMS] ?: true,
                mergeAlbums = it[MERGE_ALBUMS] ?: false
            )
        }

    suspend fun setFavorite(context: Context, uri: String, add: Boolean) {
        context.galleryDataStore.edit {
            val current = (it[FAVORITES] ?: emptySet()).toMutableSet()
            if (add) current += uri else current -= uri
            it[FAVORITES] = current
        }
    }

    suspend fun setTrash(context: Context, uri: String, timestampMs: Long?) {
        context.galleryDataStore.edit {
            val current = (it[TRASH] ?: emptySet()).toMutableSet()
            current.removeAll { entry -> entry.substringBefore("|") == uri }
            if (timestampMs != null) current += "$uri|$timestampMs"
            it[TRASH] = current
        }
    }

    suspend fun setTag(context: Context, uri: String, tag: String, add: Boolean) {
        context.galleryDataStore.edit {
            val current = (it[TAGS] ?: emptySet()).toMutableSet()
            if (add) current += "$uri|$tag" else current -= "$uri|$tag"
            it[TAGS] = current
        }
    }

    suspend fun setSetting(context: Context, key: String, value: Boolean) {
        context.galleryDataStore.edit {
            when (key) {
                "motion" -> it[MOTION_AUTOPLAY] = value
                "essential" -> it[ESSENTIAL_ALBUMS] = value
                "merge" -> it[MERGE_ALBUMS] = value
            }
        }
    }
}
