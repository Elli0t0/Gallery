package com.elli0t0.gallery.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.elli0t0.gallery.R
import com.elli0t0.gallery.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun CreateScreen(items: List<MediaItem>, onBack: () -> Unit, onCreated: () -> Unit) {
    var kind by remember { mutableStateOf<String?>(null) }
    var slideshow by remember { mutableStateOf<List<MediaItem>?>(null) }
    val slideshowItems = slideshow
    if (slideshowItems != null) {
        SlideshowScreen(items = slideshowItems, onBack = { slideshow = null })
        return
    }
    if (kind == null) {
        Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                }
                Text(stringResource(R.string.create), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
            listOf(
                Triple(R.string.collage, R.string.collage_desc, "collage"),
                Triple(R.string.slideshow, R.string.slideshow_desc, "slideshow"),
                Triple(R.string.gif, R.string.gif_desc, "gif")
            ).forEach { (title, desc, value) ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1A1A1A))
                        .clickable { kind = value }
                        .padding(20.dp)
                ) {
                    Column {
                        Text(stringResource(title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(desc), color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
    } else {
        CreatePickerScreen(
            kind = kind!!,
            items = if (kind == "slideshow") items else items.filterNot { it.isVideo },
            onBack = { kind = null },
            onSlideshow = { slideshow = it },
            onCreated = onCreated
        )
    }
}

@Composable
fun CreatePickerScreen(
    kind: String,
    items: List<MediaItem>,
    onBack: () -> Unit,
    onSlideshow: (List<MediaItem>) -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Set<MediaItem>>(emptySet()) }
    var creating by remember { mutableStateOf(false) }
    val max = if (kind == "collage") 4 else if (kind == "gif") 10 else 50
    val title = if (kind == "collage") R.string.collage else if (kind == "gif") R.string.gif else R.string.slideshow
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
            }
            Text(stringResource(title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${selected.size} / $max", color = Color.Gray, fontSize = 14.sp)
        }
        Text(
            stringResource(R.string.select_photos),
            color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(4.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items, key = { it.id }) { item ->
                val isSelected = selected.contains(item)
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) Color(0xFF0A84FF) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selected = if (isSelected) selected - item
                            else if (selected.size < max) selected + item else selected
                        }
                ) {
                    Thumb(item, Modifier.fillMaxSize())
                    if (isSelected) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0A84FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            if (creating) {
                CircularProgressIndicator()
            } else {
                Button(
                    enabled = selected.size >= 2,
                    onClick = {
                        val list = selected.sortedBy { it.dateTakenMs }
                        if (kind == "slideshow") {
                            onSlideshow(list)
                        } else {
                            creating = true
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    try {
                                        if (kind == "collage") createCollage(context, list)
                                        else createGif(context, list)
                                        true
                                    } catch (_: Exception) {
                                        false
                                    }
                                }
                                creating = false
                                if (ok) {
                                    Toast.makeText(context, R.string.created, Toast.LENGTH_SHORT).show()
                                    onCreated()
                                } else {
                                    Toast.makeText(context, R.string.create_failed, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        stringResource(
                            if (kind == "collage") R.string.create_collage
                            else if (kind == "gif") R.string.create_gif
                            else R.string.start_slideshow
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SlideshowScreen(items: List<MediaItem>, onBack: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    LaunchedEffect(playing, index) {
        if (playing && items.size > 1) {
            delay(3000)
            index = (index + 1) % items.size
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = items[index].uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().clickable { playing = !playing }
        )
        Row(
            Modifier.align(Alignment.TopStart).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { playing = !playing }) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    null, tint = Color.White, modifier = Modifier.size(32.dp)
                )
            }
            Text("${index + 1} / ${items.size}", color = Color.White, fontSize = 14.sp)
        }
    }
}

private fun decodeBitmap(context: Context, item: MediaItem, maxSize: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(item.uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }
    var sample = 1
    while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(item.uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
}

private fun drawCover(canvas: Canvas, bitmap: Bitmap, dest: Rect) {
    val src = Rect(0, 0, bitmap.width, bitmap.height)
    val scale = maxOf(
        dest.width().toFloat() / bitmap.width,
        dest.height().toFloat() / bitmap.height
    )
    val w = (bitmap.width * scale).toInt()
    val h = (bitmap.height * scale).toInt()
    val left = dest.left + (dest.width() - w) / 2
    val top = dest.top + (dest.height() - h) / 2
    canvas.drawBitmap(bitmap, src, Rect(left, top, left + w, top + h), null)
}

private fun createCollage(context: Context, items: List<MediaItem>) {
    val size = 1080
    val bitmaps = items.mapNotNull { decodeBitmap(context, it, 1080) }
    require(bitmaps.isNotEmpty())
    val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(android.graphics.Color.BLACK)
    when (bitmaps.size) {
        1 -> drawCover(canvas, bitmaps[0], Rect(0, 0, size, size))
        2 -> {
            drawCover(canvas, bitmaps[0], Rect(0, 0, size / 2, size))
            drawCover(canvas, bitmaps[1], Rect(size / 2, 0, size, size))
        }
        3 -> {
            drawCover(canvas, bitmaps[0], Rect(0, 0, size, size / 2))
            drawCover(canvas, bitmaps[1], Rect(0, size / 2, size / 2, size))
            drawCover(canvas, bitmaps[2], Rect(size / 2, size / 2, size, size))
        }
        else -> {
            for (i in 0 until 4) {
                val x = (i % 2) * size / 2
                val y = (i / 2) * size / 2
                drawCover(canvas, bitmaps[i], Rect(x, y, x + size / 2, y + size / 2))
            }
        }
    }
    val stream = ByteArrayOutputStream()
    out.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    bitmaps.forEach { it.recycle() }
    out.recycle()
    saveToGallery(context, stream.toByteArray(), "collage_${System.currentTimeMillis()}.jpg", "image/jpeg")
}

private fun createGif(context: Context, items: List<MediaItem>) {
    val frames = items.mapNotNull { item ->
        decodeBitmap(context, item, 480)?.let { bitmap ->
            val scaled = Bitmap.createScaledBitmap(bitmap, 480, 480 * bitmap.height / bitmap.width, false)
            if (scaled !== bitmap) bitmap.recycle()
            scaled
        }
    }
    require(frames.isNotEmpty())
    val width = frames[0].width
    val height = frames[0].height
    val normalized = frames.map { frame ->
        if (frame.width == width && frame.height == height) frame
        else Bitmap.createScaledBitmap(frame, width, height, false)
    }
    val stream = ByteArrayOutputStream()
    GifEncoder.encode(normalized, 50, stream)
    normalized.forEach { it.recycle() }
    saveToGallery(context, stream.toByteArray(), "gif_${System.currentTimeMillis()}.gif", "image/gif")
}

private fun saveToGallery(context: Context, bytes: ByteArray, name: String, mime: String) {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, mime)
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Gallery")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IllegalStateException("MediaStore insert failed")
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
}
