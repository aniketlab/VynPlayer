package com.muzic.player.data.local.dao

import androidx.room.*
import com.muzic.player.data.local.entity.SmartMixEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartMixDao {
    @Query("SELECT * FROM smart_mix_history ORDER BY generatedTimestamp DESC")
    fun getAllMixes(): Flow<List<SmartMixEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMix(mix: SmartMixEntity): Long

    @Query("SELECT * FROM smart_mix_history WHERE mixId = :id")
    suspend fun getMixById(id: Long): SmartMixEntity?
    
    @Query("SELECT * FROM smart_mix_history ORDER BY generatedTimestamp DESC LIMIT 1")
    suspend fun getLatestMix(): SmartMixEntity?
}
