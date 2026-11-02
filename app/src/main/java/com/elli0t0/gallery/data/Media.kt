package com.elli0t0.gallery.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val dateTakenMs: Long,
    val durationMs: Long,
    val bucket: String,
    val relativePath: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}

data class Album(
    val name: String,
    val items: List<MediaItem>
) {
    val count: Int get() = items.size
    val cover: MediaItem? get() = items.firstOrNull()
}

fun isJunkPath(bucket: String, relativePath: String): Boolean {
    val path = (relativePath + "/" + bucket).lowercase()
    val junk = listOf(
        "/android/", "/.cache/", "/cache/", "/.thumbnails/", "/thumbnails/",
        "/node_modules/", "/.tmp/", "/tmp/", "/.trash/"
    )
    if (junk.any { it in path }) return true
    if (bucket.startsWith(".")) return true
    if (bucket.startsWith("com.") || bucket.startsWith("android")) return true
    return false
}
