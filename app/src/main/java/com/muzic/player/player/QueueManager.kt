package com.muzic.player.player

import com.muzic.player.data.model.Song

class QueueManager {
    private var _originalQueue: List<Song> = emptyList()
    private var _currentQueue: List<Song> = emptyList()
    private var _currentIndex: Int = -1
    private var _isShuffled: Boolean = false

    val currentQueue: List<Song> get() = _currentQueue
    val currentIndex: Int get() = _currentIndex
    val currentSong: Song? get() = _currentQueue.getOrNull(_currentIndex)
    val isShuffled: Boolean get() = _isShuffled
    val hasNext: Boolean get() = _currentIndex < _currentQueue.size - 1
    val hasPrevious: Boolean get() = _currentIndex > 0
    val isEmpty: Boolean get() = _currentQueue.isEmpty()
    val size: Int get() = _currentQueue.size

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _originalQueue = songs.toList()
        _currentQueue = songs.toList()
        _currentIndex = startIndex.coerceIn(0, songs.size - 1)
        _isShuffled = false
    }

    fun addToQueue(song: Song) {
        _originalQueue = _originalQueue + song
        _currentQueue = _currentQueue + song
    }

    fun addNextInQueue(song: Song) {
        val insertIndex = _currentIndex + 1
        _originalQueue = _originalQueue.toMutableList().apply { add(insertIndex, song) }
        _currentQueue = _currentQueue.toMutableList().apply { add(insertIndex, song) }
    }

    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= _currentQueue.size) return

        val song = _currentQueue[index]
        _currentQueue = _currentQueue.toMutableList().apply { removeAt(index) }
        _originalQueue = _originalQueue.toMutableList().apply { remove(song) }

        when {
            index < _currentIndex -> _currentIndex--
            index == _currentIndex -> {
                if (_currentIndex >= _currentQueue.size) {
                    _currentIndex = _currentQueue.size - 1
                }
            }
        }
    }

    fun moveToNext(): Song? {
        if (!hasNext) return null
        _currentIndex++
        return currentSong
    }

    fun moveToPrevious(): Song? {
        if (!hasPrevious) return null
        _currentIndex--
        return currentSong
    }

    fun moveToIndex(index: Int): Song? {
        if (index < 0 || index >= _currentQueue.size) return null
        _currentIndex = index
        return currentSong
    }

    fun shuffle() {
        if (_currentQueue.size <= 1) return

        val current = currentSong
        val shuffled = _currentQueue.toMutableList()
        shuffled.removeAt(_currentIndex)
        shuffled.shuffle()

        _currentQueue = if (current != null) {
            listOf(current) + shuffled
        } else {
            shuffled
        }
        _currentIndex = 0
        _isShuffled = true
    }

    fun unshuffle() {
        val current = currentSong
        _currentQueue = _originalQueue.toList()
        _currentIndex = if (current != null) {
            _currentQueue.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        } else {
            0
        }
        _isShuffled = false
    }

    fun toggleShuffle() {
        if (_isShuffled) unshuffle() else shuffle()
    }

    fun clear() {
        _originalQueue = emptyList()
        _currentQueue = emptyList()
        _currentIndex = -1
        _isShuffled = false
    }
}
