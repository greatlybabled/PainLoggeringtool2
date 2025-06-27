package com.example.painlogger.data

import java.util.UUID

data class NotificationRule(
    val id: Long = System.currentTimeMillis(),
    val type: String, // "detailed" or "general"
    val mode: String, // "specific" or "interval"
    val timeHour: Int? = null,
    val timeMinute: Int? = null,
    val intervalHours: Int? = null,
    val intervalMinutes: Int? = null,
    val excludedDays: Set<Int> = emptySet(),
    val reminderId: UUID? = null

)