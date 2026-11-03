package com.elli0t0.gallery

import android.app.Application
import android.database.ContentObserver
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elli0t0.gallery.data.Album
import com.elli0t0.gallery.data.MediaItem
import com.elli0t0.gallery.data.MediaRepository
import com.elli0t0.gallery.data.Store
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites
    private val _trash = MutableStateFlow<Map<String, Long>>(emptyMap())
    val trash: StateFlow<Map<String, Long>> = _trash
    private val _settings = MutableStateFlow(Store.Settings())
    val settings: StateFlow<Store.Settings> = _settings
    private val _tags = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val tags: StateFlow<Map<String, Set<String>>> = _tags
    private val _secureItems = MutableStateFlow<List<Store.SecureItem>>(emptyList())
    val secureItems: StateFlow<List<Store.SecureItem>> = _secureItems
    private val _securePin = MutableStateFlow<String?>(null)
    val securePin: StateFlow<String?> = _securePin

    private var loadJob: Job? = null
    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            load()
        }
    }

    init {
        val context = getApplication<Application>()
        viewModelScope.launch { Store.favorites(context).collect { _favorites.value = it } }
        viewModelScope.launch { Store.trash(context).collect { _trash.value = it } }
        viewModelScope.launch { Store.settings(context).collect { _settings.value = it } }
        viewModelScope.launch { Store.tags(context).collect { _tags.value = it } }
        viewModelScope.launch { Store.secureItems(context).collect { _secureItems.value = it } }
        viewModelScope.launch { Store.securePin(context).collect { _securePin.value = it } }
        val resolver = context.contentResolver
        resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mediaObserver)
        resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, mediaObserver)
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaObserver)
        super.onCleared()
    }

    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _loading.value = true
            val context = getApplication<Application>()
            val all = MediaRepository.loadAll(context)
            val now = System.currentTimeMillis()
            val expired = _trash.value.filter { (_, ts) -> now - ts > 30L * 24 * 60 * 60 * 1000 }
            if (expired.isNotEmpty()) {
                val expiredItems = all.filter { expired.containsKey(it.uri.toString()) }
                MediaRepository.deletePermanently(context, expiredItems)
                expired.keys.forEach { Store.setTrash(context, it, null) }
                _items.value = MediaRepository.loadAll(context)
            } else {
                _items.value = all
            }
            _loading.value = false
        }
    }

    fun visibleItems(): List<MediaItem> {
        val trashed = _trash.value.keys
        return _items.value.filterNot { trashed.contains(it.uri.toString()) }
    }

    fun albums(): List<Album> = MediaRepository.albums(visibleItems(), _settings.value.mergeAlbums)
    fun essentialAlbums(): List<Album> = MediaRepository.essentialAlbums(visibleItems())

    fun setFavorites(items: List<MediaItem>, add: Boolean) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            items.forEach { Store.setFavorite(context, it.uri.toString(), add) }
        }
    }

    fun moveToTrash(items: List<MediaItem>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val now = System.currentTimeMillis()
            items.forEach { Store.setTrash(context, it.uri.toString(), now) }
        }
    }

    fun restoreFromTrash(items: List<MediaItem>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            items.forEach { Store.setTrash(context, it.uri.toString(), null) }
        }
    }

    fun deletePermanently(items: List<MediaItem>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            MediaRepository.deletePermanently(context, items)
            items.forEach { Store.setTrash(context, it.uri.toString(), null) }
            load()
        }
    }

    fun copyToAlbum(items: List<MediaItem>, albumName: String, move: Boolean) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            MediaRepository.copyToAlbum(context, items, albumName, move)
            load()
        }
    }

    fun editDateTime(items: List<MediaItem>, dateMs: Long) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val updated = MediaRepository.updateDateTime(context, items, dateMs)
            if (updated > 0) {
                load()
                Toast.makeText(context, R.string.updated, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.update_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun editLocation(items: List<MediaItem>, lat: Double, lng: Double) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val updated = MediaRepository.updateLocation(context, items, lat, lng)
            if (updated > 0) {
                load()
                Toast.makeText(context, R.string.updated, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.update_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setTag(item: MediaItem, tag: String, add: Boolean) {
        viewModelScope.launch {
            Store.setTag(getApplication(), item.uri.toString(), tag, add)
        }
    }

    fun removeTag(tag: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            _tags.value.filter { (_, tags) -> tags.contains(tag) }.forEach { (uri, _) ->
                Store.setTag(context, uri, tag, false)
            }
        }
    }

    fun renameTag(oldTag: String, newTag: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            _tags.value.filter { (_, tags) -> tags.contains(oldTag) }.forEach { (uri, _) ->
                Store.setTag(context, uri, oldTag, false)
                Store.setTag(context, uri, newTag, true)
            }
        }
    }

    fun setSetting(key: String, value: Boolean) {
        viewModelScope.launch {
            Store.setSetting(getApplication(), key, value)
        }
    }

    fun setSecurePin(pin: String) {
        viewModelScope.launch {
            Store.setSecurePin(getApplication(), com.elli0t0.gallery.ui.SecureCrypto.hashPin(pin))
        }
    }

    fun moveToSecure(items: List<MediaItem>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            var moved = 0
            items.forEach { item ->
                try {
                    val bytes = context.contentResolver.openInputStream(item.uri)?.use { it.readBytes() }
                        ?: return@forEach
                    val file = com.elli0t0.gallery.ui.SecureCrypto.encrypt(context, bytes)
                    // Verify the encrypted file decrypts before deleting the original.
                    val check = com.elli0t0.gallery.ui.SecureCrypto.decrypt(context, file.name)
                    if (!check.contentEquals(bytes)) {
                        file.delete()
                        return@forEach
                    }
                    Store.setSecureItem(
                        context,
                        Store.SecureItem(file.name, item.displayName, item.mimeType, item.dateTakenMs),
                        true
                    )
                    MediaRepository.deletePermanently(context, listOf(item))
                    val stillThere = try {
                        context.contentResolver.openInputStream(item.uri)?.use { true } ?: false
                    } catch (_: Exception) {
                        false
                    }
                    if (stillThere) {
                        // Original could not be deleted (e.g. needs consent): roll back.
                        Store.setSecureItem(
                            context,
                            Store.SecureItem(file.name, item.displayName, item.mimeType, item.dateTakenMs),
                            false
                        )
                        com.elli0t0.gallery.ui.SecureCrypto.delete(context, file.name)
                    } else {
                        moved++
                    }
                } catch (_: Exception) {
                }
            }
            if (moved > 0) load()
        }
    }

    fun restoreSecure(item: Store.SecureItem) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                val bytes = com.elli0t0.gallery.ui.SecureCrypto.decrypt(context, item.encName)
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Restored")
                }
                val target = if (item.mimeType.startsWith("video/")) {
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val uri = context.contentResolver.insert(target, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    Store.setSecureItem(context, item, false)
                    com.elli0t0.gallery.ui.SecureCrypto.delete(context, item.encName)
                    load()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun deleteSecure(item: Store.SecureItem) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            Store.setSecureItem(context, item, false)
            com.elli0t0.gallery.ui.SecureCrypto.delete(context, item.encName)
        }
    }
}
