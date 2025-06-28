


package com.example.painloggerwatch.presentation.di// Or wherever your data classes are

import android.util.Log
import androidx.room.TypeConverter
import com.example.painlogger.data.model.ReminderConfig
import com.example.painlogger.data.model.ReminderConfig.IntervalConfig
import com.example.painlogger.data.model.ReminderConfig.SpecificTimeConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken


class ReminderTypeConverters {

    private val gson = Gson()

    // Converter for List<Int> (active days)
    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toIntList(json: String?): List<Int>? {
        return if (json == null) null else gson.fromJson(json, object : TypeToken<List<Int>>() {}.type)
    }

    // Converter for ReminderConfig (using a simple JSON representation with type information)
    // This approach adds a "type" field to the JSON to distinguish between SpecificTimeConfig and IntervalConfig
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
                // This 'else' branch handles any other possible subtypes of ReminderConfig
                // If you encounter this, it means there's a ReminderConfig type
                // that you haven't explicitly added a 'is' branch for.
                // You should log an error or handle this case appropriately.
                Log.e("ReminderTypeConverters", "Unhandled ReminderConfig type during serialization: ${config::class.java.name}")
                // Returning null here means this unhandled type won't be serialized correctly
                return null
            }
        }
        return jsonObject.toString()
    }

    @TypeConverter
    fun toReminderConfig(json: String?): ReminderConfig? {
        if (json == null) return null
        return try {
            val jsonObject = JsonParser.parseString(json).asJsonObject
            val type = jsonObject.get("type").asString
            val data = jsonObject.get("data")

            when (type) {
                "specific_time" -> gson.fromJson(data, SpecificTimeConfig::class.java)
                "interval" -> gson.fromJson(data, IntervalConfig::class.java)
                else -> null // Handle unknown types
            }
        } catch (e: Exception) {
            // Log the error or handle it appropriately
            e.printStackTrace()
            null // Return null or a default value on error
        }
    }
}