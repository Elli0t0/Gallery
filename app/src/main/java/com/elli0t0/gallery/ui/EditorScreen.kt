package com.elli0t0.gallery.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elli0t0.gallery.R
import com.elli0t0.gallery.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditorScreen(item: MediaItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var base by remember { mutableStateOf<Bitmap?>(null) }
    var transform by remember { mutableStateOf(TransformState()) }
    var adjustments by remember { mutableStateOf(Adjustments()) }
    var tab by remember { mutableStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showResize by remember { mutableStateOf(false) }
    var resizeMax by remember { mutableStateOf(0) }
    var straighten by remember { mutableStateOf(0f) }
    val history = remember { mutableStateListOf(EditSnapshot(TransformState(), Adjustments())) }
    var historyIndex by remember { mutableStateOf(0) }

    fun snapshot(): EditSnapshot = EditSnapshot(transform, adjustments)
    fun pushHistory() {
        while (history.size > historyIndex + 1) history.removeAt(history.size - 1)
        history.add(snapshot())
        if (history.size > 20) history.removeAt(0)
        historyIndex = history.size - 1
    }
    fun applySnapshot(s: EditSnapshot) {
        transform = s.transform
        adjustments = s.adjustments
        straighten = s.transform.straighten
    }
    fun recompute() {
        val b = base ?: return
        scope.launch {
            val transformed = withContext(Dispatchers.Default) { applyTransform(b, transform) }
            val adjusted = withContext(Dispatchers.Default) { applyAdjustments(transformed, adjustments) }
            preview = adjusted
        }
    }
    LaunchedEffect(item.uri) {
        base = withContext(Dispatchers.IO) { decodeBitmap(context, item.uri, 1600) }
    }
    LaunchedEffect(base) { recompute() }

    fun save(asCopy: Boolean) {
        saving = true
        scope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                renderEdited(context, item.uri, snapshot(), 4096, resizeMax)
            }
            if (bitmap == null) {
                saving = false
                Toast.makeText(context, R.string.save_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val done = withContext(Dispatchers.IO) {
                if (asCopy) {
                    saveEditedCopy(context, bitmap, item.displayName)
                } else {
                    try {
                        context.contentResolver.openOutputStream(item.uri, "w")?.use {
                            it.write(bitmapToJpeg(bitmap))
                        }
                        true
                    } catch (_: Exception) {
                        saveEditedCopy(context, bitmap, item.displayName)
                    }
                }
            }
            saving = false
            Toast.makeText(
                context,
                if (done) R.string.saved else R.string.save_failed,
                Toast.LENGTH_SHORT
            ).show()
            if (done && !asCopy) onClose()
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
            IconButton(
                enabled = historyIndex > 0,
                onClick = { historyIndex--; applySnapshot(history[historyIndex]); recompute() }
            ) { Icon(Icons.Filled.Undo, null, tint = if (historyIndex > 0) Color.White else Color.Gray) }
            IconButton(
                enabled = historyIndex < history.size - 1,
                onClick = { historyIndex++; applySnapshot(history[historyIndex]); recompute() }
            ) { Icon(Icons.Filled.Redo, null, tint = if (historyIndex < history.size - 1) Color.White else Color.Gray) }
            TextButton(onClick = {
                transform = TransformState(); adjustments = Adjustments(); straighten = 0f
                pushHistory(); recompute()
            }) { Text(stringResource(R.string.revert), color = Color.White, fontSize = 13.sp) }
            Spacer(Modifier.weight(1f))
            TextButton(enabled = !saving, onClick = { save(false) }) {
                Text(if (saving) stringResource(R.string.saving) else stringResource(R.string.save), color = Color.White, fontSize = 13.sp)
            }
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, null, tint = Color.White) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.save_copy)) }, onClick = { showMenu = false; save(true) })
                DropdownMenuItem(text = { Text(stringResource(R.string.resize)) }, onClick = { showMenu = false; showResize = true })
                DropdownMenuItem(text = { Text(stringResource(R.string.copy_edits)) }, onClick = {
                    showMenu = false
                    EditClipboard.snapshot = snapshot()
                    Toast.makeText(context, R.string.edits_copied, Toast.LENGTH_SHORT).show()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.paste_edits)) }, onClick = {
                    showMenu = false
                    val copied = EditClipboard.snapshot
                    if (copied == null) {
                        Toast.makeText(context, R.string.no_copied_edits, Toast.LENGTH_SHORT).show()
                    } else {
                        applySnapshot(copied); pushHistory(); recompute()
                    }
                })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val bitmap = preview
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(), contentDescription = null,
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(stringResource(R.string.cannot_load_image), color = Color.White, fontSize = 14.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf(R.string.crop_tab to 0, R.string.filters_tab to 1, R.string.adjust_tab to 2).forEach { (label, index) ->
                TextButton(onClick = { tab = index }) {
                    Text(
                        stringResource(label),
                        color = if (tab == index) Color.White else Color.Gray, fontSize = 14.sp
                    )
                }
            }
        }
        when (tab) {
            0 -> CropControls(
                straighten = straighten,
                onStraighten = { straighten = it },
                onStraightenDone = {
                    transform = transform.copy(straighten = straighten)
                    pushHistory(); recompute()
                },
                onCropRatio = { ratio ->
                    transform = transform.copy(cropRatio = ratio)
                    pushHistory(); recompute()
                },
                onRotate = { steps ->
                    transform = transform.copy(rotateSteps = transform.rotateSteps + steps)
                    pushHistory(); recompute()
                },
                onFlipH = {
                    transform = transform.copy(flipH = !transform.flipH)
                    pushHistory(); recompute()
                },
                onFlipV = {
                    transform = transform.copy(flipV = !transform.flipV)
                    pushHistory(); recompute()
                }
            )
            1 -> FilterStrip(base = base, onFilter = { filter ->
                adjustments = filter; pushHistory(); recompute()
            })
            2 -> AdjustControls(
                adjustments = adjustments,
                onChange = { adjustments = it },
                onDone = { pushHistory(); recompute() },
                onAuto = {
                    adjustments = autoAdjustments(preview)
                    pushHistory(); recompute()
                }
            )
        }
        Spacer(Modifier.height(8.dp))
    }
    if (showResize) {
        AlertDialog(
            onDismissRequest = { showResize = false },
            title = { Text(stringResource(R.string.resize)) },
            text = {
                Column {
                    listOf(0 to R.string.resize_original, 2048 to R.string.resize_2048, 1080 to R.string.resize_1080, 720 to R.string.resize_720)
                        .forEach { (max, label) ->
                            TextButton(onClick = { resizeMax = max; showResize = false }) {
                                Text(stringResource(label))
                            }
                        }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showResize = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropControls(
    straighten: Float,
    onStraighten: (Float) -> Unit,
    onStraightenDone: () -> Unit,
    onCropRatio: (Float) -> Unit,
    onRotate: (Int) -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                0f to R.string.aspect_original, 1f to R.string.aspect_square,
                4f / 3f to R.string.aspect_4_3, 3f / 2f to R.string.aspect_3_2, 16f / 9f to R.string.aspect_16_9
            ).forEach { (ratio, label) ->
                TextButton(onClick = { onCropRatio(ratio) }) { Text(stringResource(label), fontSize = 13.sp) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onRotate(-1) }) { Text(stringResource(R.string.rotate_left), fontSize = 13.sp) }
            TextButton(onClick = { onRotate(1) }) { Text(stringResource(R.string.rotate_right), fontSize = 13.sp) }
            TextButton(onClick = onFlipH) { Text(stringResource(R.string.flip_horizontal), fontSize = 13.sp) }
            TextButton(onClick = onFlipV) { Text(stringResource(R.string.flip_vertical), fontSize = 13.sp) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.straighten), color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Slider(
                value = straighten, onValueChange = onStraighten,
                onValueChangeFinished = onStraightenDone,
                valueRange = -45f..45f, modifier = Modifier.weight(1f)
            )
            Text("${straighten.toInt()}°", color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
fun FilterStrip(base: Bitmap?, onFilter: (Adjustments) -> Unit) {
    val scope = rememberCoroutineScope()
    val thumbs = remember { mutableStateListOf<Bitmap?>() }
    LaunchedEffect(base) {
        if (base == null) return@LaunchedEffect
        val small = Bitmap.createScaledBitmap(base, 160, (160f * base.height / base.width).toInt().coerceAtLeast(1), true)
        thumbs.clear()
        FILTERS.forEach { (_, adj) ->
            thumbs.add(withContext(Dispatchers.Default) { applyAdjustments(small, adj) })
        }
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FILTERS.forEachIndexed { index, (label, adj) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onFilter(adj) }) {
                val thumb = thumbs.getOrNull(index)
                if (thumb != null) {
                    Image(
                        bitmap = thumb.asImageBitmap(), contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(label), color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

fun autoAdjustments(bitmap: Bitmap?): Adjustments {
    if (bitmap == null) return Adjustments()
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    var sum = 0L
    var step = (pixels.size / 10000).coerceAtLeast(1)
    var count = 0
    var i = 0
    while (i < pixels.size) {
        val p = pixels[i]
        sum += (0.299 * android.graphics.Color.red(p) + 0.587 * android.graphics.Color.green(p) + 0.114 * android.graphics.Color.blue(p)).toLong()
        count++
        i += step
    }
    val mean = if (count > 0) sum.toFloat() / count / 255f else 0.5f
    val brightness = ((0.5f - mean) * 60f).coerceIn(-30f, 30f)
    return Adjustments(brightness = brightness, contrast = 10f, saturation = 10f)
}
