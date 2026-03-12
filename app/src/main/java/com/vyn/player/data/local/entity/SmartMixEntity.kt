package com.vyn.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smart_mix_history")
data class SmartMixEntity(
    @PrimaryKey(autoGenerate = true)
    val mixId: Long = 0,
    val title: String, // e.g. "Today's Mix"
    val generatedTimestamp: Long = System.currentTimeMillis(),
    val songIds: List<Long> // We'll need a TypeConverter for this
)
