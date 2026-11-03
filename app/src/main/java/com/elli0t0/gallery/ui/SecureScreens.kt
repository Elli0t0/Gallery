package com.elli0t0.gallery.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.elli0t0.gallery.R
import com.elli0t0.gallery.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SecureFolderScreen(
    items: List<Store.SecureItem>,
    onBack: () -> Unit,
    onRestore: (Store.SecureItem) -> Unit,
    onDelete: (Store.SecureItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Store.SecureItem?>(null) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Lock on background: going to ON_STOP while this screen is open drops
    // the unlocked state in the caller, so returning needs unlock again.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) onBack()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
            }
            Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.secure_folder), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.secure_empty), color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.encName }) { item ->
                    SecureThumb(item) {
                        selected = item
                        scope.launch {
                            bitmap = withContext(Dispatchers.IO) {
                                try {
                                    val bytes = SecureCrypto.decrypt(context, item.encName)
                                    if (item.mimeType.startsWith("image/")) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                                } catch (_: Exception) {
                                    null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (selected != null) {
        val item = selected!!
        AlertDialog(
            onDismissRequest = { selected = null; bitmap = null },
            title = { Text(item.displayName) },
            text = {
                Column {
                    val bmp = bitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        Text(stringResource(R.string.secure_preview_unavailable), color = Color.Gray, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onRestore(item); selected = null; bitmap = null }) {
                            Text(stringResource(R.string.restore))
                        }
                        Button(onClick = { onDelete(item); selected = null; bitmap = null }) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selected = null; bitmap = null }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@Composable
fun SecureThumb(item: Store.SecureItem, onClick: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(item.encName) { mutableStateOf<android.graphics.Bitmap?>(null) }
    androidx.compose.runtime.LaunchedEffect(item.encName) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                val bytes = SecureCrypto.decrypt(context, item.encName)
                if (item.mimeType.startsWith("image/")) {
                    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Filled.Lock, null, tint = Color(0xFF555555))
        }
    }
}

@Composable
fun PinDialog(
    titleRes: Int,
    confirmRes: Int = R.string.confirm,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            TextField(
                value = pin,
                onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
                label = { Text(stringResource(R.string.pin)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = pin.length >= 4, onClick = { onConfirm(pin) }) {
                Text(stringResource(confirmRes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
