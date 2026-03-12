package com.vyn.player.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String): List<Long> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").map { it.toLong() }
    }

    @TypeConverter
    fun fromList(list: List<Long>): String {
        return list.joinToString(",")
    }
}
