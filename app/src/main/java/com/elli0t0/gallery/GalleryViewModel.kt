package com.elli0t0.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elli0t0.gallery.data.Album
import com.elli0t0.gallery.data.MediaItem
import com.elli0t0.gallery.data.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _items.value = MediaRepository.loadAll(getApplication())
            _loading.value = false
        }
    }

    fun albums(): List<Album> = MediaRepository.albums(_items.value)
    fun essentialAlbums(): List<Album> = MediaRepository.essentialAlbums(_items.value)
}
