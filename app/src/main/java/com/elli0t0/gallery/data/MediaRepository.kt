package com.elli0t0.gallery.data

import android.content.ContentUris
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
            MediaStore.Video.Media.DURATION
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
                    relativePath = it.getString(pathCol) ?: ""
                )
            }
        }
        return out
    }

    fun albums(items: List<MediaItem>): List<Album> {
        return items.groupBy { it.bucket }
            .map { (name, list) -> Album(name, list) }
            .sortedByDescending { it.count }
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
