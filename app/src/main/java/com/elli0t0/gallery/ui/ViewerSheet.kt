package com.elli0t0.gallery.ui

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.print.PrintHelper
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.elli0t0.gallery.R
import com.elli0t0.gallery.data.MediaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.0f KB".format(kb)
    return "%.1f MB".format(kb / 1024.0)
}

fun formatDateTime(ms: Long): String {
    if (ms <= 0) return "—"
    return SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(ms))
}

fun launchExternalEditor(context: Context, item: MediaItem) {
    try {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(item.uri, item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            Toast.makeText(context, R.string.no_editor_found, Toast.LENGTH_SHORT).show()
            return
        }
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: Exception) {
        Toast.makeText(context, R.string.no_editor_found, Toast.LENGTH_SHORT).show()
    }
}

fun shareItem(context: Context, item: MediaItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType
        putExtra(Intent.EXTRA_STREAM, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

fun copyItemToClipboard(context: Context, item: MediaItem) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newUri(context.contentResolver, item.displayName, item.uri))
    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
}

fun setItemAsWallpaper(context: Context, item: MediaItem) {
    try {
        context.contentResolver.openInputStream(item.uri)?.use { input ->
            WallpaperManager.getInstance(context).setStream(input)
        }
        Toast.makeText(context, R.string.wallpaper_set, Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, R.string.cannot_open_video, Toast.LENGTH_SHORT).show()
    }
}

suspend fun printItem(context: Context, item: MediaItem) {
    try {
        val bitmap = withContext(Dispatchers.IO) {
            val result = ImageLoader(context).execute(
                ImageRequest.Builder(context).data(item.uri).build()
            )
            ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        } ?: return
        val helper = PrintHelper(context)
        helper.scaleMode = PrintHelper.SCALE_MODE_FIT
        helper.printBitmap(item.displayName, bitmap)
    } catch (_: Exception) {
    }
}

@Composable
fun ViewerBottomPill(
    item: MediaItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowDetails: () -> Unit,
    onPrint: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(36.dp))
                .background(PillColor)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    null, tint = if (isFavorite) Color.Red else Color.White
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, null, tint = Color.White)
            }
            IconButton(onClick = { shareItem(context, item) }) {
                Icon(Icons.Filled.Share, null, tint = Color.White)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, null, tint = Color.White)
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, null, tint = Color.White)
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.details)) },
                onClick = { showMenu = false; onShowDetails() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy_to_clipboard)) },
                onClick = { showMenu = false; copyItemToClipboard(context, item) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.set_as_wallpaper)) },
                onClick = { showMenu = false; setItemAsWallpaper(context, item) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.print)) },
                onClick = { showMenu = false; onPrint() }
            )
        }
    }
}

@Composable
fun Filmstrip(
    list: List<MediaItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(list) { index, item ->
            val borderColor = if (index == currentIndex) Color(0xFF0A84FF) else Color.Transparent
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable { onSelect(index) }
            ) {
                Thumb(item, Modifier.fillMaxWidth().aspectRatio(1f), sizePx = 128)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsSheet(
    item: MediaItem,
    tags: Set<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTag by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardColor) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            DetailRow(stringResource(R.string.detail_date), formatDateTime(item.dateTakenMs))
            DetailRow(stringResource(R.string.detail_name), item.displayName)
            DetailRow(stringResource(R.string.detail_path), (item.relativePath + item.displayName))
            DetailRow(stringResource(R.string.detail_size), formatSize(item.sizeBytes))
            DetailRow(
                stringResource(R.string.detail_resolution),
                if (item.width > 0 && item.height > 0) "${item.width} x ${item.height}" else "—"
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.tags), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF404040))
                            .clickable { onRemoveTag(tag) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(tag, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text(stringResource(R.string.add_tag)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    enabled = newTag.isNotBlank(),
                    onClick = { onAddTag(newTag.trim()); newTag = "" }
                ) { Text(stringResource(R.string.add)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.width(110.dp))
        Text(value, color = Color.White, fontSize = 14.sp)
    }
}
