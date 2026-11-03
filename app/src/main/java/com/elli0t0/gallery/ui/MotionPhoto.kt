package com.elli0t0.gallery.ui

import android.content.Context
import com.elli0t0.gallery.data.MediaItem
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

// Motion photos (JPEG + MP4 appended after the JPEG end marker) store the
// video right after 0xFFD9, starting at a box header whose type is "ftyp".
// This scans the file in chunks, copies only the video part to app cache,
// and returns it, or null when the file is not a motion photo.
fun findMotionVideo(context: Context, item: MediaItem): File? {
    if (!item.mimeType.startsWith("image/jpeg")) return null
    val cached = File(context.cacheDir, "motion_${item.id}.mp4")
    if (cached.exists() && cached.length() > 0) return cached
    val input: InputStream = context.contentResolver.openInputStream(item.uri) ?: return null
    return try {
        val buffer = ByteArray(64 * 1024)
        var videoStart = -1L
        var totalRead = 0L
        var previousTail = ByteArray(0)
        var found = false
        while (!found) {
            val read = input.read(buffer)
            if (read <= 0) break
            val chunk = previousTail + buffer.copyOfRange(0, read)
            // Find JPEG end marker, then "ftyp" after it.
            var searchFrom = 0
            while (true) {
                val eoi = indexOf(chunk, byteArrayOf(0xFF.toByte(), 0xD9.toByte()), searchFrom)
                if (eoi < 0) break
                val ftyp = indexOf(chunk, "ftyp".toByteArray(Charsets.US_ASCII), eoi + 2)
                if (ftyp >= 4) {
                    videoStart = totalRead - previousTail.size + ftyp - 4
                    found = true
                    break
                }
                searchFrom = eoi + 2
            }
            if (!found) {
                previousTail = chunk.takeLast(8).toByteArray()
                totalRead += read
            } else {
                // Copy from videoStart to end of file.
                val out = FileOutputStream(cached)
                out.use { output ->
                    // The video part already read is in this chunk, after videoStart.
                    val alreadyReadStart = totalRead - previousTail.size
                    val offsetInChunk = (videoStart - alreadyReadStart).toInt()
                    if (offsetInChunk in chunk.indices) {
                        output.write(chunk, offsetInChunk, chunk.size - offsetInChunk)
                    }
                    var n: Int
                    while (input.read(buffer).also { n = it } > 0) {
                        output.write(buffer, 0, n)
                    }
                }
                if (cached.length() > 0) return cached
                cached.delete()
                return null
            }
        }
        null
    } catch (_: Exception) {
        cached.delete()
        null
    } finally {
        try {
            input.close()
        } catch (_: Exception) {
        }
    }
}

private fun indexOf(data: ByteArray, pattern: ByteArray, from: Int): Int {
    if (pattern.isEmpty() || data.size < pattern.size) return -1
    for (i in from..data.size - pattern.size) {
        var match = true
        for (j in pattern.indices) {
            if (data[i + j] != pattern[j]) {
                match = false
                break
            }
        }
        if (match) return i
    }
    return -1
}
