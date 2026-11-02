package com.elli0t0.gallery

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.elli0t0.gallery.ui.MenuPopup
import com.elli0t0.gallery.ui.PicturesScreen
import com.elli0t0.gallery.ui.PillNav
import com.elli0t0.gallery.ui.ViewerScreen

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()

    private sealed interface Screen {
        data object Home : Screen
        data class AlbumDetail(val album: Album) : Screen
        data class Viewer(val list: List<MediaItem>, val index: Int) : Screen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GalleryTheme {
                val items by viewModel.items.collectAsState()
                var granted by remember { mutableStateOf(hasMediaPermission()) }
                var showMenu by remember { mutableStateOf(false) }
                var tab by remember { mutableStateOf(0) }
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }

                val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    granted = hasMediaPermission() || result.values.any { it }
                    if (granted) viewModel.load()
                }

                LaunchedEffect(granted) {
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

                when (val current = screen) {
                    is Screen.Viewer -> ViewerScreen(current.list, current.index) { screen = Screen.Home }
                    is Screen.AlbumDetail -> {
                        Box(Modifier.fillMaxSize()) {
                            PicturesScreen(
                                items = current.album.items,
                                onOpen = { list, idx -> screen = Screen.Viewer(list, idx) },
                                onMenu = { showMenu = true }
                            )
                            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
                                PillNav(tab) { i ->
                                    tab = i
                                    screen = Screen.Home
                                    if (i == 2) showMenu = true
                                }
                            }
                        }
                    }
                    Screen.Home -> {
                        Box(Modifier.fillMaxSize()) {
                            if (tab == 0) {
                                PicturesScreen(
                                    items = items,
                                    onOpen = { list, idx -> screen = Screen.Viewer(list, idx) },
                                    onMenu = { showMenu = true }
                                )
                            } else {
                                AlbumsScreen(
                                    essential = viewModel.essentialAlbums(),
                                    all = viewModel.albums(),
                                    onOpenAlbum = { album -> screen = Screen.AlbumDetail(album) }
                                )
                            }
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

                if (showMenu) {
                    MenuPopup(onDismiss = { showMenu = false })
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
