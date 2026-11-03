package com.elli0t0.gallery.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaRepository {

    suspend fun loadAll(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        items += query(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false)
        items += query(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)
        items.filterNot { isJunkPath(it.bucket, it.relativePath) }
            .sortedByDescending { it.dateTakenMs }
    }

    private fun query(context: Context, uri: android.net.Uri, isVideo: Boolean): List<MediaItem> {
        val out = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.Video.Media.DURATION,
            MediaStore.Images.Media.LATITUDE,
            MediaStore.Images.Media.LONGITUDE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )
        val cursor = context.contentResolver.query(
            uri, projection, null, null,
            "${MediaStore.MediaColumns.DATE_TAKEN} DESC"
        ) ?: return out
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val takenCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val addedCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val bucketCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val pathCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val durCol = it.getColumnIndex(MediaStore.Video.Media.DURATION)
            val latCol = it.getColumnIndex(MediaStore.Images.Media.LATITUDE)
            val lngCol = it.getColumnIndex(MediaStore.Images.Media.LONGITUDE)
            val sizeCol = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val widthCol = it.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightCol = it.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val taken = it.getLong(takenCol)
                val addedSec = it.getLong(addedCol)
                val dateMs = if (taken > 0) taken else addedSec * 1000L
                val mime = it.getString(mimeCol) ?: if (isVideo) "video/mp4" else "image/jpeg"
                out += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(uri, id),
                    displayName = it.getString(nameCol) ?: "",
                    mimeType = mime,
                    dateTakenMs = dateMs,
                    durationMs = if (durCol >= 0 && isVideo) it.getLong(durCol) else 0L,
                    bucket = it.getString(bucketCol) ?: "Unknown",
                    relativePath = it.getString(pathCol) ?: "",
                    latitude = if (latCol >= 0 && !it.isNull(latCol)) it.getDouble(latCol) else null,
                    longitude = if (lngCol >= 0 && !it.isNull(lngCol)) it.getDouble(lngCol) else null,
                    sizeBytes = if (sizeCol >= 0 && !it.isNull(sizeCol)) it.getLong(sizeCol) else 0L,
                    width = if (widthCol >= 0 && !it.isNull(widthCol)) it.getInt(widthCol) else 0,
                    height = if (heightCol >= 0 && !it.isNull(heightCol)) it.getInt(heightCol) else 0
                )
            }
        }
        return out
    }

    fun albums(items: List<MediaItem>, merge: Boolean = false): List<Album> {
        return if (!merge) {
            items.groupBy { it.bucket }
                .map { (name, list) -> Album(name, list) }
                .sortedByDescending { it.count }
        } else {
            items.groupBy { it.bucket.lowercase() }
                .map { (_, list) -> Album(list.first().bucket, list) }
                .sortedByDescending { it.count }
        }
    }

    suspend fun copyToAlbum(context: Context, items: List<MediaItem>, albumName: String, move: Boolean): Int =
        withContext(Dispatchers.IO) {
            var copied = 0
            val resolver = context.contentResolver
            items.forEach { item ->
                try {
                    val collection = if (item.isVideo) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
                        put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                        put(MediaStore.MediaColumns.DATE_TAKEN, item.dateTakenMs)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$albumName")
                    }
                    val newUri = resolver.insert(collection, values) ?: return@forEach
                    resolver.openInputStream(item.uri)?.use { input ->
                        resolver.openOutputStream(newUri)?.use { output -> input.copyTo(output) }
                    }
                    copied++
                    if (move) {
                        try {
                            resolver.delete(item.uri, null, null)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
            }
            copied
        }

    suspend fun updateDateTime(context: Context, items: List<MediaItem>, dateMs: Long): Int =
        withContext(Dispatchers.IO) {
            var updated = 0
            items.forEach { item ->
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DATE_TAKEN, dateMs)
                    }
                    if (context.contentResolver.update(item.uri, values, null, null) > 0) updated++
                } catch (_: Exception) {
                }
            }
            updated
        }

    suspend fun updateLocation(context: Context, items: List<MediaItem>, lat: Double, lng: Double): Int =
        withContext(Dispatchers.IO) {
            var updated = 0
            items.forEach { item ->
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.LATITUDE, lat)
                        put(MediaStore.Images.Media.LONGITUDE, lng)
                    }
                    if (context.contentResolver.update(item.uri, values, null, null) > 0) updated++
                } catch (_: Exception) {
                }
            }
            updated
        }

    suspend fun deletePermanently(context: Context, items: List<MediaItem>) {
        withContext(Dispatchers.IO) {
            items.forEach { item ->
                try {
                    context.contentResolver.delete(item.uri, null, null)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun essentialAlbums(items: List<MediaItem>): List<Album> {
        val byBucket = items.groupBy { it.bucket }
        val out = mutableListOf<Album>()
        if (items.isNotEmpty()) out += Album("Recent", items)
        listOf("Camera", "Screenshots", "Download", "Pictures", "DCIM").forEach { name ->
            byBucket[name]?.let { out += Album(name, it) }
        }
        return out.distinctBy { it.name }
    }
}
