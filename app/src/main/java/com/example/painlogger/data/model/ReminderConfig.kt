package com.example.painlogger.data.model

import kotlinx.serialization.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.IgnoredOnParcel

@Serializable
@Parcelize
sealed class ReminderConfig : Parcelable {
    abstract val id: String        // Added abstract property
    abstract val title: String     // Added abstract property
    abstract val message: String   // Added abstract property

    @Serializable
    @Parcelize
    data class Time(val hour: Int, val minute: Int) : Parcelable

    @Serializable
    @Parcelize
    data class SpecificTimeConfig(
        override val id: String,           // Implementing abstract property
        override val title: String,        // Implementing abstract property
        override val message: String,      // Implementing abstract property
        val times: List<Time>
    ) : ReminderConfig() {
        init {
            require(times.isNotEmpty()) { "At least one time must be specified" }
        }
    }

    @Serializable
    @Parcelize
    data class IntervalConfig(
        override val id: String,           // Implementing abstract property
        override val title: String,        // Implementing abstract property
        override val message: String,      // Implementing abstract property
        val intervalHours: Int = 0,
        val intervalMinutes: Int = 0
    ) : ReminderConfig() {
        init {
            require(intervalHours >= 0 && intervalMinutes >= 0) {
                "Interval values cannot be negative"
            }
            require(intervalHours > 0 || intervalMinutes > 0) {
                "Interval must be greater than zero"
            }
            require(intervalMinutes < 60) {
                "Minutes must be less than 60"
            }
        }
    }

    open fun displayString(): String {
        return when (this) {
            is SpecificTimeConfig -> formatSpecificTimes()
            is IntervalConfig -> formatInterval()
            else -> "Unknown Config"
        }
    }

    private fun SpecificTimeConfig.formatSpecificTimes(): String {
        return if (times.isEmpty()) "No times set" else {
            times.joinToString(", ") { time ->
                "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
            }
        }
    }

    private fun IntervalConfig.formatInterval(): String {
        val parts = mutableListOf<String>()
        if (intervalHours > 0) parts.add("${intervalHours}h")
        if (intervalMinutes > 0) parts.add("${intervalMinutes}m")
        return if (parts.isEmpty()) "Invalid interval" else parts.joinToString(" ")
    }
}