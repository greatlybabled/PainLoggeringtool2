package com.example.painloggerwatch.presentation

import android.util.Log
import androidx.room.TypeConverter
import com.example.painlogger.data.model.ReminderConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.util.UUID
import com.example.painlogger.data.model.ReminderConfig.IntervalConfig
import com.example.painlogger.data.model.ReminderConfig.SpecificTimeConfig

// Ensure these are imported if they are in a different package
// import com.example.painlogger.domain.ReminderCategory // Example if in domain package
// import com.example.painlogger.domain.ReminderType     // Example if in domain package


// Type converters for Room Database
class ReminderConverters {

    private val gson = Gson() // Use a single Gson instance

    // Converter for UUID to String and vice-versa
    @TypeConverter
    fun fromUuid(uuid: UUID?): String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun toUuid(uuidString: String?): UUID? {
        return uuidString?.let { UUID.fromString(it) }
    }

    // Converter for Set<Int> (for activeDays) to String and vice-versa
    // Changed to use Gson for consistency with ReminderConfig
    @TypeConverter
    fun fromIntSet(intSet: Set<Int>?): String? {
        return gson.toJson(intSet)
    }

    @TypeConverter
    fun toIntSet(intSetString: String?): Set<Int>? {
        return if (intSetString.isNullOrEmpty()) {
            emptySet() // Return emptySet() for null or empty string
        } else {
            gson.fromJson(intSetString, object : TypeToken<Set<Int>>() {}.type)
        }
    }

    // Converter for ReminderConfig (using a JSON representation with explicit type information)
    @TypeConverter
    fun fromReminderConfig(config: ReminderConfig?): String? {
        if (config == null) return null
        // Create a JsonObject and add the type and the config data
        val jsonObject = JsonObject()
        when (config) {
            is SpecificTimeConfig -> {
                jsonObject.addProperty("type", "specific_time")
                jsonObject.add("data", gson.toJsonTree(config))
            }
            is IntervalConfig -> {
                jsonObject.addProperty("type", "interval")
                jsonObject.add("data", gson.toJsonTree(config))
            }
            else -> {
                // Handle any other potential subtypes of ReminderConfig
                // Log an error or return null, as this indicates an unexpected type
                Log.e("ReminderConverters", "Unhandled ReminderConfig type during serialization: ${config::class.java.name}")
                return null
            }
        }
        return jsonObject.toString()
    }

    @TypeConverter
    fun toReminderConfig(json: String?): ReminderConfig? {
        if (json.isNullOrEmpty()) return null // Use isNullOrEmpty for safety
        return try {
            val jsonObject = JsonParser.parseString(json).asJsonObject
            val type = jsonObject.get("type")?.asString // Use safe access (?)
            val data = jsonObject.get("data")

            when (type) {
                "specific_time" -> gson.fromJson(data, SpecificTimeConfig::class.java)
                "interval" -> gson.fromJson(data, IntervalConfig::class.java)
                else -> {
                    // Log an error or handle unexpected types
                    Log.e("ReminderConverters", "Unknown ReminderConfig type: $type in JSON: $json")
                    null // Return null or a default on unknown type
                }
            }
        } catch (e: Exception) {
            // Log the error for debugging
            Log.e("ReminderConverters", "Error deserializing ReminderConfig: ${e.message}", e)
            null // Return null or a default on error
        }
    }

    // Converter for ReminderCategory to String and vice-versa
    @TypeConverter
    fun fromReminderCategory(category: ReminderCategory?): String? {
        return category?.name
    }

    @TypeConverter
    fun toReminderCategory(categoryString: String?): ReminderCategory? {
        // Use safe access and let for potential null string
        return categoryString?.let {
            try {
                ReminderCategory.valueOf(it)
            } catch (e: IllegalArgumentException) {
                Log.e("ReminderConverters", "Invalid ReminderCategory string: $it", e)
                null // Return null or a default category on error
            }
        }
    }

    // Converter for ReminderType to String and vice-versa
    @TypeConverter
    fun fromReminderType(type: ReminderType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toReminderType(typeString: String?): ReminderType? {
        // Use safe access and let for potential null string
        return typeString?.let {
            try {
                ReminderType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                Log.e("ReminderConverters", "Invalid ReminderType string: $it", e)
                null // Return null or a default type on error
            }
        }
    }
}