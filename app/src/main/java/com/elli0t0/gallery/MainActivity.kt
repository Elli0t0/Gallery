package com.elli0t0.gallery

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.elli0t0.gallery.data.Album
import com.elli0t0.gallery.data.MediaItem
import com.elli0t0.gallery.ui.AlbumsScreen
import com.elli0t0.gallery.ui.GalleryTheme
import com.elli0t0.gallery.ui.LocationsScreen
import com.elli0t0.gallery.ui.MenuDestination
import com.elli0t0.gallery.ui.MenuPopup
import com.elli0t0.gallery.ui.PicturesScreen
import com.elli0t0.gallery.ui.PillNav
import com.elli0t0.gallery.ui.SearchScreen
import com.elli0t0.gallery.ui.SettingsScreen
import com.elli0t0.gallery.ui.TrashScreen
import com.elli0t0.gallery.ui.ViewerScreen
import com.elli0t0.gallery.ui.launchExternalEditor

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()

    private sealed interface Screen {
        data object Home : Screen
        data class AlbumDetail(val album: Album) : Screen
        data class Viewer(val list: List<MediaItem>, val index: Int) : Screen
        data class ListScreen(val titleRes: Int, val items: List<MediaItem>) : Screen
        data object Search : Screen
        data object Trash : Screen
        data object Locations : Screen
        data object Settings : Screen
    }

    override fun onResume() {
        super.onResume()
        if (hasMediaPermission()) viewModel.load()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GalleryTheme {
                val items by viewModel.items.collectAsState()
                val loading by viewModel.loading.collectAsState()
                val favorites by viewModel.favorites.collectAsState()
                val tags by viewModel.tags.collectAsState()
                val trash by viewModel.trash.collectAsState()
                val settings by viewModel.settings.collectAsState()
                var granted by remember { mutableStateOf(hasMediaPermission()) }
                var showMenu by remember { mutableStateOf(false) }
                var tab by remember { mutableStateOf(0) }
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                var selectionActive by remember { mutableStateOf(false) }
                val homeListState = rememberLazyListState()
                val activityContext = androidx.compose.ui.platform.LocalContext.current

                BackHandler(enabled = screen != Screen.Home || tab != 0) {
                    screen = Screen.Home
                    tab = 0
                }

                val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    granted = hasMediaPermission() || result.values.any { it }
                    if (granted) viewModel.load()
                }

                if (!granted) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.permission_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.permission_text), color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { launcher.launch(mediaPermissions()) }) {
                                Text(stringResource(R.string.allow_access))
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                            }) {
                                Text(stringResource(R.string.permission_open_settings))
                            }
                        }
                    }
                    return@GalleryTheme
                }

                val visible = viewModel.visibleItems()
                val albumNames = viewModel.albums().map { it.name }
                fun openMenuDestination(dest: MenuDestination) {
                    screen = when (dest) {
                        MenuDestination.VIDEOS -> Screen.ListScreen(R.string.videos, visible.filter { it.isVideo })
                        MenuDestination.FAVORITES -> Screen.ListScreen(R.string.favorites, visible.filter { favorites.contains(it.uri.toString()) })
                        MenuDestination.RECENT -> {
                            val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                            Screen.ListScreen(R.string.recent, visible.filter { it.dateTakenMs >= cutoff })
                        }
                        MenuDestination.LOCATIONS -> Screen.Locations
                        MenuDestination.TRASH -> Screen.Trash
                        MenuDestination.SETTINGS -> Screen.Settings
                    }
                }
                LaunchedEffect(screen) { selectionActive = false }

                when (val current = screen) {
                    is Screen.Viewer -> ViewerScreen(
                        list = current.list,
                        startIndex = current.index,
                        onBack = { screen = Screen.Home; tab = 0 },
                        favoriteUris = favorites,
                        tags = tags,
                        onToggleFavorite = { item, add -> viewModel.setFavorites(listOf(item), add) },
                        onDeleteToTrash = { item -> viewModel.moveToTrash(listOf(item)) },
                        onAddTag = { item, tag -> viewModel.setTag(item, tag, true) },
                        onRemoveTag = { item, tag -> viewModel.setTag(item, tag, false) },
                        onEdit = { launchExternalEditor(activityContext, it) }
                    )
                    is Screen.AlbumDetail -> {
                        Box(Modifier.fillMaxSize()) {
                            PicturesScreen(
                                items = current.album.items,
                                onOpen = { l, idx -> screen = Screen.Viewer(l, idx) },
                                onSearch = { screen = Screen.Search },
                                onRefresh = { viewModel.load() },
                                titleText = current.album.name,
                                albumNames = albumNames,
                                isLoading = loading,
                                onMoveToTrash = { viewModel.moveToTrash(it) },
                                onCopyToAlbum = { selected, name, move -> viewModel.copyToAlbum(selected, name, move) },
                                onToggleFavorites = { selected, add -> viewModel.setFavorites(selected, add) },
                                onSelectionChanged = { selectionActive = it }
                            )
                            if (!selectionActive) {
                                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
                                    PillNav(tab) { i ->
                                        tab = i
                                        screen = Screen.Home
                                        if (i == 2) showMenu = true
                                    }
                                }
                            }
                        }
                    }
                    is Screen.ListScreen -> {
                        Box(Modifier.fillMaxSize()) {
                            PicturesScreen(
                                items = current.items,
                                onOpen = { l, idx -> screen = Screen.Viewer(l, idx) },
                                onSearch = { screen = Screen.Search },
                                onRefresh = { viewModel.load() },
                                titleRes = current.titleRes,
                                albumNames = albumNames,
                                isLoading = loading,
                                onMoveToTrash = { viewModel.moveToTrash(it) },
                                onCopyToAlbum = { selected, name, move -> viewModel.copyToAlbum(selected, name, move) },
                                onToggleFavorites = { selected, add -> viewModel.setFavorites(selected, add) },
                                onSelectionChanged = { selectionActive = it }
                            )
                            if (!selectionActive) {
                                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
                                    PillNav(tab) { i ->
                                        tab = i
                                        screen = Screen.Home
                                        if (i == 2) showMenu = true
                                    }
                                }
                            }
                        }
                    }
                    Screen.Search -> SearchScreen(visible, { l, idx -> screen = Screen.Viewer(l, idx) }) { screen = Screen.Home }
                    Screen.Trash -> {
                        val trashedItems = items.filter { trash.containsKey(it.uri.toString()) }
                            .sortedByDescending { trash[it.uri.toString()] ?: 0L }
                        TrashScreen(
                            items = trashedItems,
                            trash = trash,
                            onRestore = { viewModel.restoreFromTrash(it) },
                            onDeletePermanently = { viewModel.deletePermanently(it) },
                            onEmptyTrash = { viewModel.deletePermanently(trashedItems) },
                            onBack = { screen = Screen.Home }
                        )
                    }
                    Screen.Locations -> LocationsScreen(visible, { l, idx -> screen = Screen.Viewer(l, idx) }) { screen = Screen.Home }
                    Screen.Settings -> SettingsScreen(
                        settings = settings,
                        onToggle = { key, value -> viewModel.setSetting(key, value) },
                        onOpenPermissions = {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                        },
                        versionName = BuildConfig.VERSION_NAME,
                        onBack = { screen = Screen.Home }
                    )
                    Screen.Home -> {
                        Box(Modifier.fillMaxSize()) {
                            if (tab == 0) {
                                PicturesScreen(
                                    items = visible,
                                    onOpen = { l, idx -> screen = Screen.Viewer(l, idx) },
                                    onSearch = { screen = Screen.Search },
                                    onRefresh = { viewModel.load() },
                                    albumNames = albumNames,
                                    isLoading = loading,
                                    externalListState = homeListState,
                                    onMoveToTrash = { viewModel.moveToTrash(it) },
                                    onCopyToAlbum = { selected, name, move -> viewModel.copyToAlbum(selected, name, move) },
                                    onToggleFavorites = { selected, add -> viewModel.setFavorites(selected, add) },
                                    onSelectionChanged = { selectionActive = it }
                                )
                            } else {
                                AlbumsScreen(
                                    essential = viewModel.essentialAlbums(),
                                    all = viewModel.albums(),
                                    onOpenAlbum = { album -> screen = Screen.AlbumDetail(album) },
                                    onSearch = { screen = Screen.Search },
                                    showEssential = settings.essentialAlbums
                                )
                            }
                            if (!selectionActive) {
                                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
                                    PillNav(tab) { i ->
                                        tab = i
                                        if (i == 2) {
                                            showMenu = true
                                            tab = 0
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showMenu) {
                    MenuPopup(
                        onDismiss = { showMenu = false },
                        onSelect = { dest ->
                            selectionActive = false
                            openMenuDestination(dest)
                        }
                    )
                }
            }
        }
    }

    private fun mediaPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 34) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun hasMediaPermission(): Boolean {
        return mediaPermissions().any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
