package com.example.painloggerwatch.presentation.data.model

import java.io.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

import com.example.painlogger.data.model.ReminderConfig

// This class is now deprecated as we're using ReminderConfig.IntervalConfig instead
// Keeping this file for reference but it should not be used
@Deprecated("Use ReminderConfig.IntervalConfig instead")
@Parcelize
data class IntervalConfig(
    override val id: String,
    override val title: String,
    override val message: String,
    val duration: Int,
    val uninitializedProperty: Int,
    val unit: IntervalUnit
) : ReminderConfig(), Serializable {

    enum class IntervalUnit {
        MINUTES, HOURS
    }

    override fun displayString(): String {
        return "$duration ${unit.name.lowercase()} interval"
    }
}