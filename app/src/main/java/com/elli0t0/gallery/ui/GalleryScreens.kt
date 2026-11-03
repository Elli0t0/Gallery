package com.elli0t0.gallery.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import com.elli0t0.gallery.R
import com.elli0t0.gallery.data.Album
import com.elli0t0.gallery.data.MediaItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.hypot
import kotlinx.coroutines.launch

fun formatDuration(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

private object ThumbLoader {
    private var loader: ImageLoader? = null
    fun get(context: Context): ImageLoader {
        return loader ?: ImageLoader.Builder(context.applicationContext)
            .components { add(VideoFrameDecoder.Factory()) }
            .build().also { loader = it }
    }
}

@Composable
fun Thumb(item: MediaItem, modifier: Modifier = Modifier, sizePx: Int = 360) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(item.uri).size(sizePx).crossfade(false).build(),
        imageLoader = ThumbLoader.get(LocalContext.current),
        contentDescription = item.displayName,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(Color(0xFF1A1A1A))
    )
}

@Composable
fun dateLabel(ms: Long): String {
    val today = stringResource(R.string.today)
    val yesterday = stringResource(R.string.yesterday)
    val cal = Calendar.getInstance().apply { timeInMillis = ms }
    val now = Calendar.getInstance()
    val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    if (isToday) return today
    val yCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = cal.get(Calendar.YEAR) == yCal.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == yCal.get(Calendar.DAY_OF_YEAR)
    if (isYesterday) return yesterday
    val pattern = if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) "MMM d" else "MMM d, yyyy"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(ms))
}

private sealed interface PicRow {
    data class Header(val label: String) : PicRow
    data class GridRow(val items: List<MediaItem>) : PicRow
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PicturesScreen(
    items: List<MediaItem>,
    onOpen: (List<MediaItem>, Int) -> Unit,
    onSearch: () -> Unit = {},
    onRefresh: () -> Unit = {},
    titleText: String? = null,
    titleRes: Int = R.string.pictures,
    albumNames: List<String> = emptyList(),
    isLoading: Boolean = false,
    externalListState: LazyListState? = null,
    onMoveToTrash: (List<MediaItem>) -> Unit = {},
    onCopyToAlbum: (List<MediaItem>, String, Boolean) -> Unit = { _, _, _ -> },
    onToggleFavorites: (List<MediaItem>, Boolean) -> Unit = { _, _ -> },
    onAddTagToSelected: (List<MediaItem>, String) -> Unit = { _, _ -> },
    onEditDateTime: (List<MediaItem>, Long) -> Unit = { _, _ -> },
    onEditLocation: (List<MediaItem>, Double, Double) -> Unit = { _, _, _ -> },
    onSelectionChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var columns by remember { mutableStateOf(4) }
    val listState = externalListState ?: rememberLazyListState()
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var dragLabel by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Set<MediaItem>>(emptySet()) }
    val inSelection = selected.isNotEmpty()
    var showTopMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var albumDialogMode by remember { mutableStateOf<String?>(null) }
    var albumName by remember { mutableStateOf("") }
    var tagDialog by remember { mutableStateOf(false) }
    var tagText by remember { mutableStateOf("") }
    var dateDialog by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }
    var locationDialog by remember { mutableStateOf(false) }
    var latText by remember { mutableStateOf("") }
    var lngText by remember { mutableStateOf("") }
    LaunchedEffect(inSelection) { onSelectionChanged(inSelection) }
    BackHandler(enabled = inSelection) { selected = emptySet() }
    fun shareSelected() {
        val list = selected.toList()
        if (list.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(list.map { it.uri }))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
        selected = emptySet()
    }

    val rows = remember(items, columns) {
        val out = mutableListOf<PicRow>()
        var currentLabel = ""
        var current = mutableListOf<MediaItem>()
        fun flush() {
            if (current.isNotEmpty()) {
                current.chunked(columns).forEach { out += PicRow.GridRow(it) }
                current = mutableListOf()
            }
        }
        items.forEach { item ->
            val label = run {
                val cal = Calendar.getInstance().apply { timeInMillis = item.dateTakenMs }
                val now = Calendar.getInstance()
                val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                if (isToday) "TODAY" else {
                    val y = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    val isY = cal.get(Calendar.YEAR) == y.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
                    if (isY) "YESTERDAY" else SimpleDateFormat(
                        if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) "MMM d" else "MMM d, yyyy",
                        Locale.getDefault()
                    ).format(Date(item.dateTakenMs))
                }
            }
            if (label != currentLabel) {
                flush()
                currentLabel = label
                out += PicRow.Header(label)
            }
            current += item
        }
        flush()
        out
    }
    val headerLabel = { raw: String ->
        when (raw) {
            "TODAY" -> context.getString(R.string.today)
            "YESTERDAY" -> context.getString(R.string.yesterday)
            else -> raw
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Black,
        topBar = {
            if (inSelection) {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.selected_count, selected.size), fontWeight = FontWeight.Bold) },
                    actions = {
                        Checkbox(
                            checked = selected.size == items.size && items.isNotEmpty(),
                            onCheckedChange = { all ->
                                selected = if (all) items.toSet() else emptySet()
                            }
                        )
                        IconButton(onClick = { selected = emptySet() }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Black),
                    scrollBehavior = scrollBehavior
                )
            } else {
                LargeTopAppBar(
                    title = { Text(titleText ?: stringResource(titleRes), fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                        }
                        Box {
                            IconButton(onClick = { showTopMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                            }
                            DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.select)) },
                                    onClick = {
                                        showTopMenu = false
                                        if (items.isNotEmpty()) selected = setOf(items.first())
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.refresh)) },
                                    onClick = { showTopMenu = false; onRefresh() }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Black),
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                    var startDist: Float? = null
                    var handled = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size < 2) {
                            startDist = null
                            handled = false
                            continue
                        }
                        val d = hypot(
                            (pressed[0].position.x - pressed[1].position.x).toDouble(),
                            (pressed[0].position.y - pressed[1].position.y).toDouble()
                        ).toFloat()
                        if (startDist == null) {
                            startDist = d
                        } else if (!handled) {
                            val ratio = d / startDist!!
                            if (ratio > 1.35f) {
                                columns = (columns - 1).coerceAtLeast(2)
                                handled = true
                            } else if (ratio < 0.74f) {
                                columns = (columns + 1).coerceAtMost(6)
                                handled = true
                            }
                        }
                    }
                    }
                }
        ) {
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Text(stringResource(R.string.empty_gallery), color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(rows) { _, row ->
                        when (row) {
                            is PicRow.Header -> Text(
                                headerLabel(row.label),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                            )
                            is PicRow.GridRow -> Row(Modifier.fillMaxWidth()) {
                                row.items.forEach { item ->
                                    val isSel = selected.contains(item)
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(0.5.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    if (inSelection) {
                                                        selected = if (isSel) selected - item else selected + item
                                                    } else {
                                                        val idx = items.indexOf(item)
                                                        if (idx >= 0) onOpen(items, idx)
                                                    }
                                                },
                                                onLongClick = { selected = selected + item }
                                            )
                                    ) {
                                        Thumb(item, Modifier.fillMaxSize())
                                        if (inSelection) {
                                            Box(
                                                Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(8.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSel) Color(0xFF0A84FF) else Color.Transparent)
                                                    .border(BorderStroke(2.dp, Color.White), CircleShape)
                                            )
                                        }
                                        if (item.isVideo) {
                                            Row(
                                                Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Text(formatDuration(item.durationMs), color = Color.White, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                                repeat(columns - row.items.size) {
                                    Spacer(Modifier.weight(1f).aspectRatio(1f).padding(0.5.dp))
                                }
                            }
                        }
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(28.dp)
                    .fillMaxSize()
                    .pointerInput(rows) {
                        detectVerticalDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false }
                        ) { change, _ ->
                            val sizeH = size.height.toFloat()
                            if (sizeH <= 0f) return@detectVerticalDragGestures
                            val fraction = (change.position.y / sizeH).coerceIn(0f, 1f)
                            val target = (fraction * (rows.size - 1)).toInt().coerceIn(0, rows.size - 1)
                            var label = ""
                            for (i in target downTo 0) {
                                val r = rows[i]
                                if (r is PicRow.Header) {
                                    label = headerLabel(r.label)
                                    break
                                }
                            }
                            dragLabel = label
                            scope.launch { listState.scrollToItem(target) }
                        }
                    }
            ) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .width(4.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF555555))
                )
            }
            if (dragging && dragLabel.isNotEmpty()) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 40.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0A84FF))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(dragLabel, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (inSelection) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(36.dp))
                            .background(PillColor)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.create), color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { albumDialogMode = "create"; albumName = "" })
                        Text(stringResource(R.string.share), color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { shareSelected() })
                        Text(stringResource(R.string.delete), color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { showDeleteDialog = true })
                        Box {
                            Text(stringResource(R.string.more), color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { showMoreMenu = true })
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.copy_to_album)) },
                                    onClick = { showMoreMenu = false; albumDialogMode = "copy"; albumName = "" }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_to_album)) },
                                    onClick = { showMoreMenu = false; albumDialogMode = "move"; albumName = "" }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.add_to_favorites)) },
                                    onClick = { showMoreMenu = false; onToggleFavorites(selected.toList(), true); selected = emptySet() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.remove_from_favorites)) },
                                    onClick = { showMoreMenu = false; onToggleFavorites(selected.toList(), false); selected = emptySet() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.add_tag)) },
                                    onClick = { showMoreMenu = false; tagText = ""; tagDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_date_time)) },
                                    onClick = {
                                        showMoreMenu = false
                                        val first = selected.firstOrNull()
                                        val cal = java.util.Calendar.getInstance()
                                        if (first != null) cal.timeInMillis = first.dateTakenMs
                                        dateText = "%04d-%02d-%02d".format(
                                            cal.get(java.util.Calendar.YEAR),
                                            cal.get(java.util.Calendar.MONTH) + 1,
                                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                                        )
                                        timeText = "%02d:%02d".format(
                                            cal.get(java.util.Calendar.HOUR_OF_DAY),
                                            cal.get(java.util.Calendar.MINUTE)
                                        )
                                        dateDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_location)) },
                                    onClick = {
                                        showMoreMenu = false
                                        val first = selected.firstOrNull()
                                        latText = first?.latitude?.toString() ?: ""
                                        lngText = first?.longitude?.toString() ?: ""
                                        locationDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_to_trash)) },
                text = { Text(stringResource(R.string.delete_to_trash_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        onMoveToTrash(selected.toList())
                        selected = emptySet()
                    }) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
        if (tagDialog) {
            AlertDialog(
                onDismissRequest = { tagDialog = false },
                title = { Text(stringResource(R.string.add_tag)) },
                text = {
                    TextField(
                        value = tagText,
                        onValueChange = { tagText = it },
                        label = { Text(stringResource(R.string.add_tag)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = tagText.isNotBlank(),
                        onClick = {
                            tagDialog = false
                            onAddTagToSelected(selected.toList(), tagText.trim())
                            selected = emptySet()
                        }
                    ) { Text(stringResource(R.string.confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { tagDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
        if (dateDialog) {
            AlertDialog(
                onDismissRequest = { dateDialog = false },
                title = { Text(stringResource(R.string.edit_date_time)) },
                text = {
                    Column {
                        TextField(
                            value = dateText,
                            onValueChange = { dateText = it },
                            label = { Text(stringResource(R.string.date_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        TextField(
                            value = timeText,
                            onValueChange = { timeText = it },
                            label = { Text(stringResource(R.string.time_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            val parsed = SimpleDateFormat(
                                "yyyy-MM-dd HH:mm", Locale.getDefault()
                            ).parse("$dateText $timeText") ?: throw IllegalArgumentException()
                            dateDialog = false
                            onEditDateTime(selected.toList(), parsed.time)
                            selected = emptySet()
                        } catch (_: Exception) {
                            Toast.makeText(context, R.string.invalid_date, Toast.LENGTH_SHORT).show()
                        }
                    }) { Text(stringResource(R.string.confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { dateDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
        if (locationDialog) {
            AlertDialog(
                onDismissRequest = { locationDialog = false },
                title = { Text(stringResource(R.string.edit_location)) },
                text = {
                    Column {
                        TextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text(stringResource(R.string.latitude)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        TextField(
                            value = lngText,
                            onValueChange = { lngText = it },
                            label = { Text(stringResource(R.string.longitude)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val lat = latText.toDoubleOrNull()
                        val lng = lngText.toDoubleOrNull()
                        if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                            Toast.makeText(context, R.string.invalid_location, Toast.LENGTH_SHORT).show()
                        } else {
                            locationDialog = false
                            onEditLocation(selected.toList(), lat, lng)
                            selected = emptySet()
                        }
                    }) { Text(stringResource(R.string.confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { locationDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
        if (albumDialogMode != null) {
            val mode = albumDialogMode!!
            Dialog(onDismissRequest = { albumDialogMode = null }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(CardColor)
                        .padding(24.dp)
                ) {
                    Text(
                        if (mode == "create") stringResource(R.string.create) else if (mode == "copy") stringResource(R.string.copy_to_album) else stringResource(R.string.move_to_album),
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    TextField(
                        value = albumName,
                        onValueChange = { albumName = it },
                        label = { Text(stringResource(R.string.album_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    albumNames.forEach { name ->
                        Text(
                            name, color = Color(0xFF0A84FF), fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCopyToAlbum(selected.toList(), name, mode == "move")
                                    Toast.makeText(context, if (mode == "move") R.string.moved_to_album else R.string.copied_to_album, Toast.LENGTH_SHORT).show()
                                    albumDialogMode = null
                                    selected = emptySet()
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { albumDialogMode = null }) { Text(stringResource(R.string.cancel)) }
                        TextButton(
                            enabled = albumName.isNotBlank(),
                            onClick = {
                                onCopyToAlbum(selected.toList(), albumName.trim(), mode == "move")
                                Toast.makeText(context, if (mode == "move") R.string.moved_to_album else R.string.copied_to_album, Toast.LENGTH_SHORT).show()
                                albumDialogMode = null
                                selected = emptySet()
                            }
                        ) { Text(stringResource(R.string.confirm)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    essential: List<Album>,
    all: List<Album>,
    onOpenAlbum: (Album) -> Unit,
    onSearch: () -> Unit = {},
    showEssential: Boolean = true
) {
    val context = LocalContext.current
    var showAll by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Black,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.albums), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Black),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (showEssential) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.essential_albums), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            if (showAll) stringResource(R.string.show_less) else stringResource(R.string.view_all),
                            color = Color(0xFF0A84FF),
                            fontSize = 15.sp,
                            modifier = Modifier.clickable { showAll = !showAll }
                        )
                    }
                }
                items(essential) { album -> AlbumTile(album, onOpenAlbum) }
            }
            if (showAll || !showEssential) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Text(stringResource(R.string.all_albums), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 16.dp))
                }
                items(all) { album -> AlbumTile(album, onOpenAlbum) }
            }
        }
    }
}

@Composable
fun AlbumTile(album: Album, onClick: (Album) -> Unit) {
    Column(Modifier.clickable { onClick(album) }) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            album.cover?.let { Thumb(it, Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.height(6.dp))
        Text(album.name, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${album.count}", color = Color.Gray, fontSize = 13.sp)
    }
}

enum class MenuDestination { VIDEOS, FAVORITES, RECENT, LOCATIONS, TRASH, SETTINGS }

@Composable
fun MenuPopup(onDismiss: () -> Unit, onSelect: (MenuDestination) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(CardColor)
                .padding(24.dp)
        ) {
            val items = listOf(
                Triple(R.string.videos, Icons.Filled.PlayCircle, MenuDestination.VIDEOS),
                Triple(R.string.favorites, Icons.Filled.FavoriteBorder, MenuDestination.FAVORITES),
                Triple(R.string.recent, Icons.Filled.AccessTime, MenuDestination.RECENT),
                Triple(R.string.locations, Icons.Filled.Place, MenuDestination.LOCATIONS),
                Triple(R.string.trash, Icons.Filled.Delete, MenuDestination.TRASH),
                Triple(R.string.settings, Icons.Filled.Settings, MenuDestination.SETTINGS)
            )
            items.chunked(4).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    rowItems.forEach { (label, icon, dest) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelect(dest); onDismiss() }
                                .padding(vertical = 12.dp)
                        ) {
                            Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(label), color = Color.White, fontSize = 13.sp)
                        }
                    }
                    repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun ViewerScreen(
    list: List<MediaItem>,
    startIndex: Int,
    onBack: () -> Unit,
    favoriteUris: Set<String> = emptySet(),
    tags: Map<String, Set<String>> = emptyMap(),
    onToggleFavorite: (MediaItem, Boolean) -> Unit = { _, _ -> },
    onDeleteToTrash: (MediaItem) -> Unit = {},
    onAddTag: (MediaItem, String) -> Unit = { _, _ -> },
    onRemoveTag: (MediaItem, String) -> Unit = { _, _ -> },
    onEdit: (MediaItem) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDetails by remember { mutableStateOf(false) }
    var showVideoControls by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))) { list.size }
    BackHandler { onBack() }
    val player = remember {
        ExoPlayer.Builder(context).build()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }
    val currentIndex = pagerState.currentPage.coerceIn(0, (list.size - 1).coerceAtLeast(0))
    LaunchedEffect(showVideoControls, currentIndex) {
        if (showVideoControls) {
            kotlinx.coroutines.delay(3000)
            showVideoControls = false
        }
    }
    LaunchedEffect(currentIndex, list) {
        val current = list.getOrNull(currentIndex)
        if (current != null && current.isVideo) {
            player.setMediaItem(ExoMediaItem.fromUri(current.uri))
            player.prepare()
            player.playWhenReady = true
            showVideoControls = true
        } else {
            player.pause()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { list[it].id }
        ) { page ->
            val item = list[page]
            if (item.isVideo && page == pagerState.currentPage) {
                VideoSurface(
                    player = player,
                    onTap = { showVideoControls = !showVideoControls }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ZoomableImage(item)
                    if (item.isVideo) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
        }
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
        }
        if (list.isNotEmpty()) {
            val current = list[currentIndex]
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (current.isVideo && showVideoControls) {
                    VideoControls(player = player, item = current)
                    Spacer(Modifier.height(8.dp))
                }
                ViewerBottomPill(
                    item = current,
                    isFavorite = favoriteUris.contains(current.uri.toString()),
                    onToggleFavorite = {
                        onToggleFavorite(current, !favoriteUris.contains(current.uri.toString()))
                    },
                    onEdit = { onEdit(current) },
                    onDelete = { onDeleteToTrash(current); onBack() },
                    onShowDetails = { showDetails = true },
                    onPrint = { scope.launch { printItem(context, current) } }
                )
                Spacer(Modifier.height(12.dp))
                Filmstrip(list, pagerState.currentPage) { index ->
                    scope.launch { pagerState.scrollToPage(index) }
                }
            }
            if (showDetails) {
                DetailsSheet(
                    item = current,
                    tags = tags[current.uri.toString()] ?: emptySet(),
                    onAddTag = { tag -> onAddTag(current, tag) },
                    onRemoveTag = { tag -> onRemoveTag(current, tag) },
                    onDismiss = { showDetails = false }
                )
            }
        }
    }
}

@Composable
fun ZoomableImage(item: MediaItem) {
    val context = LocalContext.current
    var scale by remember(item.id) { mutableStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    AsyncImage(
        model = ImageRequest.Builder(context).data(item.uri).crossfade(false).build(),
        imageLoader = ThumbLoader.get(context),
        contentDescription = item.displayName,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale, scaleY = scale,
                translationX = offset.x, translationY = offset.y
            )
            .pointerInput(item.id) {
                awaitPointerEventScope {
                    var prevDist: Float? = null
                    var prevCentroid: Offset? = null
                    var prevSingle: Offset? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            val first = pressed[0].position
                            val second = pressed[1].position
                            val dist = kotlin.math.hypot(
                                (first.x - second.x).toDouble(),
                                (first.y - second.y).toDouble()
                            ).toFloat()
                            val centroid = (first + second) / 2f
                            if (prevDist != null && prevCentroid != null) {
                                scale = (scale * (dist / prevDist!!)).coerceIn(1f, 4f)
                                if (scale <= 1f) {
                                    offset = Offset.Zero
                                } else {
                                    offset += (centroid - prevCentroid!!)
                                }
                            }
                            prevDist = dist
                            prevCentroid = centroid
                            prevSingle = null
                            pressed.forEach { it.consume() }
                        } else if (pressed.size == 1) {
                            val pos = pressed[0].position
                            if (scale > 1f && prevSingle != null) {
                                offset += (pos - prevSingle!!)
                                pressed[0].consume()
                            }
                            prevSingle = pos
                            prevDist = null
                            prevCentroid = null
                        } else {
                            prevDist = null
                            prevCentroid = null
                            prevSingle = null
                        }
                    }
                }
            }
    )
}

@Composable
fun PillNav(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .width(340.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(PillColor)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val labels = listOf(R.string.pictures, R.string.albums, R.string.menu)
        val icons = listOf(Icons.Outlined.Image, Icons.Outlined.Album, Icons.Outlined.Menu)
        labels.forEachIndexed { i, label ->
            val isSel = i == selected
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (isSel) PillSelectedColor else Color.Transparent)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSelect(i) }) {
                    Icon(icons[i], null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Text(stringResource(label), color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}
