package com.elli0t0.gallery.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elli0t0.gallery.R
import com.elli0t0.gallery.data.MediaItem
import com.elli0t0.gallery.data.Store

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(items: List<MediaItem>, onOpen: (List<MediaItem>, Int) -> Unit, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, items) {
        if (query.isBlank()) emptyList()
        else items.filter {
            it.displayName.contains(query, ignoreCase = true) ||
                it.bucket.contains(query, ignoreCase = true)
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Black,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.search), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Black),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (query.isNotBlank() && results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_results), color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(results) { item ->
                        Box(
                            Modifier.aspectRatio(1f).padding(0.5.dp).clickable {
                                val idx = results.indexOf(item)
                                if (idx >= 0) onOpen(results, idx)
                            }
                        ) {
                            Thumb(item, Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    items: List<MediaItem>,
    trash: Map<String, Long>,
    onRestore: (List<MediaItem>) -> Unit,
    onDeletePermanently: (List<MediaItem>) -> Unit,
    onEmptyTrash: () -> Unit,
    onBack: () -> Unit
) {
    var showDeleteAll by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Black,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.trash), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAll = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.empty_trash), tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Black),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.trash_empty), color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(items) { item ->
                    val ts = trash[item.uri.toString()] ?: now
                    val daysLeft = ((30L * 24 * 60 * 60 * 1000 - (now - ts)) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Thumb(item, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(item.displayName, color = Color.White, fontSize = 15.sp, maxLines = 1)
                            Text(stringResource(R.string.days_left, daysLeft.toInt()), color = Color.Gray, fontSize = 13.sp)
                        }
                        TextButton(onClick = { onRestore(listOf(item)) }) { Text(stringResource(R.string.restore)) }
                        TextButton(onClick = { onDeletePermanently(listOf(item)) }) { Text(stringResource(R.string.delete)) }
                    }
                }
            }
        }
        if (showDeleteAll) {
            AlertDialog(
                onDismissRequest = { showDeleteAll = false },
                title = { Text(stringResource(R.string.empty_trash)) },
                text = { Text(stringResource(R.string.delete_permanently_text)) },
                confirmButton = {
                    TextButton(onClick = { showDeleteAll = false; onEmptyTrash() }) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAll = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(items: List<MediaItem>, onOpen: (List<MediaItem>, Int) -> Unit, onBack: () -> Unit) {
    val groups = remember(items) {
        items.filter { it.latitude != null && it.longitude != null }
            .groupBy { item -> "%.1f, %.1f".format(item.latitude, item.longitude) }
            .toList().sortedByDescending { it.second.size }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Black,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.locations), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Black),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_location_items), color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                groups.forEach { (label, groupItems) ->
                    item {
                        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp))
                    }
                    groupItems.chunked(4).forEach { rowItems ->
                        item {
                            Row(Modifier.fillMaxWidth()) {
                                rowItems.forEach { item ->
                                    Box(
                                        Modifier.weight(1f).aspectRatio(1f).padding(0.5.dp).clickable {
                                            val idx = groupItems.indexOf(item)
                                            if (idx >= 0) onOpen(groupItems, idx)
                                        }
                                    ) {
                                        Thumb(item, Modifier.fillMaxSize())
                                    }
                                }
                                repeat(4 - rowItems.size) {
                                    Spacer(Modifier.weight(1f).aspectRatio(1f).padding(0.5.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Store.Settings,
    onToggle: (String, Boolean) -> Unit,
    onOpenPermissions: () -> Unit,
    versionName: String,
    onBack: () -> Unit
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Black,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Black),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.auto_play_motion), color = Color.White, fontSize = 16.sp)
                    Switch(checked = settings.motionAutoplay, onCheckedChange = { onToggle("motion", it) })
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.essential_albums), color = Color.White, fontSize = 16.sp)
                    Switch(checked = settings.essentialAlbums, onCheckedChange = { onToggle("essential", it) })
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.merge_albums), color = Color.White, fontSize = 16.sp)
                    Switch(checked = settings.mergeAlbums, onCheckedChange = { onToggle("merge", it) })
                }
            }
            item {
                Text(stringResource(R.string.privacy_policy), color = Color.White, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable { dialog = "privacy" }.padding(horizontal = 16.dp, vertical = 16.dp))
            }
            item {
                Text(stringResource(R.string.permissions), color = Color.White, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable { onOpenPermissions() }.padding(horizontal = 16.dp, vertical = 16.dp))
            }
            item {
                Text(stringResource(R.string.about), color = Color.White, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable { dialog = "about" }.padding(horizontal = 16.dp, vertical = 16.dp))
            }
        }
        if (dialog != null) {
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = {
                    Text(if (dialog == "privacy") stringResource(R.string.privacy_policy) else stringResource(R.string.about))
                },
                text = {
                    Text(if (dialog == "privacy") stringResource(R.string.privacy_text) else stringResource(R.string.about_text, versionName))
                },
                confirmButton = {
                    TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.confirm)) }
                }
            )
        }
    }
}
