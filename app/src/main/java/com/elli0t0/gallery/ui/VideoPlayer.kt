package com.elli0t0.gallery.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.elli0t0.gallery.R
import com.elli0t0.gallery.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(item: MediaItem) {
    val context = LocalContext.current
    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(item.durationMs) }
    var frames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLandscape by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition
            if (player.duration > 0) duration = player.duration
            isPlaying = player.isPlaying
            delay(500)
        }
    }
    LaunchedEffect(item.uri) {
        frames = withContext(Dispatchers.IO) {
            val out = mutableListOf<Bitmap>()
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, item.uri)
                val totalUs = (item.durationMs * 1000).coerceAtLeast(1L)
                for (i in 0 until 8) {
                    val timeUs = totalUs * i / 8
                    val frame = retriever.getFrameAtTime(timeUs) ?: continue
                    val scaled = Bitmap.createScaledBitmap(frame, 160, 90, false)
                    if (scaled !== frame) frame.recycle()
                    out += scaled
                }
                retriever.release()
            } catch (_: Exception) {
            }
            out
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (player.isPlaying) player.pause() else player.play()
                    isPlaying = player.isPlaying
                }) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null, tint = Color.White, modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    "${formatDuration(position)} / ${formatDuration(duration)}",
                    color = Color.White, fontSize = 13.sp
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    isMuted = !isMuted
                    player.volume = if (isMuted) 1f else 0f
                }) {
                    Text(
                        if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute),
                        color = Color.White, fontSize = 13.sp
                    )
                }
                TextButton(onClick = {
                    val activity = context as? Activity
                    if (activity != null) {
                        isLandscape = !isLandscape
                        activity.requestedOrientation = if (isLandscape) {
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    } else {
                        Toast.makeText(context, R.string.rotate_unavailable, Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.rotate), color = Color.White, fontSize = 13.sp)
                }
            }
            Slider(
                value = position.toFloat(),
                onValueChange = { value ->
                    position = value.toLong()
                    player.seekTo(value.toLong())
                },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            if (frames.isNotEmpty()) {
                LazyRow(
                    Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(frames) { index, bitmap ->
                        Box(
                            Modifier
                                .size(width = 80.dp, height = 45.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val target = duration * index / frames.size
                                    player.seekTo(target)
                                    position = target
                                }
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
        if (!isPlaying) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { player.play() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
    }
}
