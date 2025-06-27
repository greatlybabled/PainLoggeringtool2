// File: ReminderEntity.kt
package com.example.painlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

enum class ReminderType {
    INTERVAL,
    SPECIFIC_TIME
}

@Entity(tableName = "reminders")
@TypeConverters(ReminderConverters::class)
data class ReminderEntity(
    @PrimaryKey
    val id: UUID,
    val title: String,
    val category: String,
    val type: ReminderType,
    val isEnabled: Boolean,
    val activeDays: Set<Int>, // Store days as Set of integers (1-7)
    val config: String // JSON string for storing either IntervalConfig or SpecificTimeConfig
)

class ReminderConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromUUID(uuid: UUID): String = uuid.toString()

    @TypeConverter
    fun toUUID(value: String): UUID = UUID.fromString(value)

    @TypeConverter
    fun fromReminderType(value: ReminderType): String = value.name

    @TypeConverter
    fun toReminderType(value: String): ReminderType = ReminderType.valueOf(value)

    @TypeConverter
    fun fromActiveDays(value: Set<Int>): String = gson.toJson(value)

    @TypeConverter
    fun toActiveDays(value: String): Set<Int> {
        val type = object : TypeToken<Set<Int>>() {}.type
        return gson.fromJson(value, type)
    }
}