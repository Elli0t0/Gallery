package com.elli0t0.gallery.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
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

@Composable
fun Thumb(item: MediaItem, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(item.uri).size(360).crossfade(false).build(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PicturesScreen(
    items: List<MediaItem>,
    onOpen: (List<MediaItem>, Int) -> Unit,
    onMenu: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var columns by remember { mutableStateOf(4) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var dragLabel by remember { mutableStateOf("") }

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
            LargeTopAppBar(
                title = { Text(stringResource(R.string.pictures), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { Toast.makeText(context, R.string.coming_phase2, Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = { Toast.makeText(context, R.string.coming_phase2, Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Black),
                scrollBehavior = scrollBehavior
            )
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
                    Text(stringResource(R.string.empty_gallery), color = Color.Gray)
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
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(0.5.dp)
                                            .clickable {
                                                val idx = items.indexOf(item)
                                                if (idx >= 0) onOpen(items, idx)
                                            }
                                    ) {
                                        Thumb(item, Modifier.fillMaxSize())
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    essential: List<Album>,
    all: List<Album>,
    onOpenAlbum: (Album) -> Unit
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
                    IconButton(onClick = { Toast.makeText(context, R.string.coming_phase2, Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                    IconButton(onClick = { Toast.makeText(context, R.string.coming_phase2, Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = { Toast.makeText(context, R.string.coming_phase2, Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
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
            if (showAll) {
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

@Composable
fun MenuPopup(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(CardColor)
                .padding(24.dp)
        ) {
            val items = listOf(
                Triple(R.string.videos, Icons.Filled.PlayCircle, Icons.Filled.PlayCircle),
                Triple(R.string.favorites, Icons.Filled.FavoriteBorder, Icons.Filled.FavoriteBorder),
                Triple(R.string.recent, Icons.Filled.AccessTime, Icons.Filled.AccessTime),
                Triple(R.string.locations, Icons.Filled.Place, Icons.Filled.Place),
                Triple(R.string.trash, Icons.Filled.Delete, Icons.Filled.Delete),
                Triple(R.string.settings, Icons.Filled.Settings, Icons.Filled.Settings)
            )
            items.chunked(4).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    rowItems.forEach { (label, icon, _) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    Toast.makeText(context, R.string.coming_phase2, Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))) { list.size }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = list[page]
            var scale by remember(item.id) { mutableStateOf(1f) }
            var offset by remember(item.id) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 4f)
                if (scale > 1f) offset += panChange else offset = androidx.compose.ui.geometry.Offset.Zero
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(item.uri).crossfade(false).build(),
                    contentDescription = item.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale, scaleY = scale,
                            translationX = offset.x, translationY = offset.y
                        )
                        .transformable(transformState)
                        .clickable(enabled = item.isVideo) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(item.uri, item.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, R.string.coming_phase2, Toast.LENGTH_SHORT).show()
                            }
                        }
                )
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
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
        }
    }
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
