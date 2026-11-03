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
}
