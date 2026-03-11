package com.muzic.player.data.repository

import com.muzic.player.data.local.dao.SmartMixDao
import com.muzic.player.data.local.dao.SongDao
import com.muzic.player.data.local.dao.SongStatsDao
import com.muzic.player.data.local.entity.SmartMixEntity
import com.muzic.player.data.local.entity.SongStatsEntity
import com.muzic.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.roundToInt

@Singleton
class SmartMixRepository @Inject constructor(
    private val songDao: SongDao,
    private val songStatsDao: SongStatsDao,
    private val smartMixDao: SmartMixDao,
    private val playbackHistoryDao: com.muzic.player.data.local.dao.PlaybackHistoryDao,
    private val musicRepository: MusicRepository
) {
    private var memoryCachedMix: List<Song>? = null
    private var lastCacheTime: Long = 0L
    suspend fun generateSmartMix(): List<Long> = withContext(Dispatchers.IO) {
        val allSongs = musicRepository.getAllSongs().firstOrNull() ?: return@withContext emptyList()
        val allStats = songStatsDao.getAllStats().associateBy { it.songId }
        
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // 1. Get songs played in the current time window for boosting
        val timeWindowSongs = when {
            hour in 6..11 -> playbackHistoryDao.getSongsPlayedInHourRange("06", "11").toSet()
            hour in 12..17 -> playbackHistoryDao.getSongsPlayedInHourRange("12", "17").toSet()
            else -> (playbackHistoryDao.getSongsPlayedInHourRange("18", "23") + 
                     playbackHistoryDao.getSongsPlayedInHourRange("00", "05")).toSet()
        }
        
        // 2. Calculate scores for all songs
        val scoredSongs = allSongs.map { song ->
            val stats = allStats[song.id] ?: SongStatsEntity(songId = song.id)
            val isTimeBoosted = timeWindowSongs.contains(song.id)
            val score = calculateScore(stats, now, isTimeBoosted)
            song.id to score
        }.sortedByDescending { it.second }
        
        if (scoredSongs.isEmpty()) return@withContext emptyList()
        
        // 3. Determine mix size based on library size
        val mixSize = when {
            allSongs.size < 200 -> 20
            allSongs.size <= 1000 -> 30
            else -> 40
        }
        
        // 4. Composition: 70% favorite, 20% medium, 10% rare
        val favoriteCount = (mixSize * 0.70).roundToInt()
        val mediumCount = (mixSize * 0.20).roundToInt()
        val rareCount = (mixSize - favoriteCount - mediumCount).coerceAtLeast(1)
        
        val favorites = scoredSongs.take(scoredSongs.size / 4) // Top 25% are favorites
        val medium = scoredSongs.drop(scoredSongs.size / 4).take(scoredSongs.size / 2) // Middle 50%
        val rare = scoredSongs.drop(3 * scoredSongs.size / 4) // Bottom 25%
        
        val mixSongs = mutableListOf<Long>()
        
        // Category distribution with internal shuffling
        mixSongs.addAll(favorites.shuffled().take(favoriteCount).map { it.first })
        mixSongs.addAll(medium.shuffled().take(mediumCount).map { it.first })
        mixSongs.addAll(rare.shuffled().take(rareCount).map { it.first })
        
        // Final shuffle
        val finalMix = mixSongs.shuffled()
        
        // 5. Persist mix in history
        if (finalMix.isNotEmpty()) {
            val dateStr = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}"
            smartMixDao.insertMix(
                SmartMixEntity(
                    title = "Daily Smart Mix - $dateStr",
                    songIds = finalMix
                )
            )
        }
        
        finalMix
    }

    private fun calculateScore(stats: SongStatsEntity, now: Long, isTimeBoosted: Boolean): Float {
        // User Formula: score = (playCount × 2.0) + (completionRate × 1.5) - (skipCount × 2.5) + recencyWeight
        // Note: completionRate is 0.0 to 1.0, so we normalize completionRate to have more weight if it was intended to be 0-100, 
        // but following literal instructions:
        var score = (stats.playCount * 2.0f) + (stats.completionRate * 1.5f) - (stats.skipCount * 2.5f)
        
        // Recency Weight
        val diffMs = now - stats.lastPlayedTimestamp
        val oneDayMs = 24 * 60 * 60 * 1000L
        val oneWeekMs = 7 * oneDayMs
        
        if (stats.lastPlayedTimestamp > 0) {
            score += when {
                diffMs < oneDayMs -> 5f        // Played today
                diffMs < oneWeekMs -> 3f       // Played this week
                else -> 0f
            }
        }
        
        // Repeat Pattern Detection: > 3 times in 7 days
        if (stats.last7DaysPlayCount >= 3) {
            score += 10f // Significant score boost
        }
        
        // Time window boost
        if (isTimeBoosted) {
            score += 4f
        }
        
        return score
    }

    suspend fun getLatestMixSongs(forceRefresh: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        
        // 1. Check memory cache (valid for 1 hour)
        if (!forceRefresh && memoryCachedMix != null && (now - lastCacheTime) < (1 * 60 * 60 * 1000)) {
            return@withContext memoryCachedMix!!
        }

        val latestMix = smartMixDao.getLatestMix()
        
        // Refresh every 6 hours or if forced
        val needsRefresh = latestMix == null || (now - latestMix.generatedTimestamp) > (6 * 60 * 60 * 1000)
        
        val songIds = if (needsRefresh || forceRefresh) {
            generateSmartMix()
        } else {
            latestMix?.songIds ?: emptyList()
        }
        
        val allSongs = musicRepository.getAllSongs().firstOrNull() ?: return@withContext emptyList()
        val result = songIds.mapNotNull { id -> allSongs.find { it.id == id } }
        
        // Update memory cache
        memoryCachedMix = result
        lastCacheTime = now
        
        result
    }

    // Historical mix access
    fun getMixHistory() = smartMixDao.getAllMixes()
    
    suspend fun getTodaysMix(): List<Long>? {
        val all = smartMixDao.getAllMixes().firstOrNull() ?: return null
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        return all.find { it.generatedTimestamp >= startOfToday }?.songIds
    }

    suspend fun getYesterdaysMix(): List<Long>? {
        val all = smartMixDao.getAllMixes().firstOrNull() ?: return null
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val startOfYesterday = startOfToday - (24 * 60 * 60 * 1000L)
        
        return all.find { it.generatedTimestamp in startOfYesterday until startOfToday }?.songIds
    }

    suspend fun getWeeklyMix(): List<Long>? {
        val all = smartMixDao.getAllMixes().firstOrNull() ?: return null
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val startOfLastWeek = startOfToday - (7 * 24 * 60 * 60 * 1000L)
        
        // Return latest mix from the last week
        return all.filter { it.generatedTimestamp >= startOfLastWeek }
            .sortedByDescending { it.generatedTimestamp }
            .firstOrNull()?.songIds
    }
}
