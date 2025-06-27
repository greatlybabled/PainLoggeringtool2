
package com.example.painlogger.util

import com.example.painlogger.Reminder
import com.example.painlogger.ReminderCategory
import com.example.painlogger.ReminderType
import com.example.painlogger.data.model.ReminderConfig
import com.example.painlogger.data.NotificationRule
import java.util.UUID

object NotificationRuleMapper {
    fun toReminder(rule: NotificationRule): Reminder {
        val category = when (rule.type.lowercase()) {
            "detailed" -> ReminderCategory.DETAILED
            "general" -> ReminderCategory.GENERAL
            else -> ReminderCategory.GENERAL // fallback
        }
        val type = when (rule.mode.lowercase()) {
            "specific" -> ReminderType.SPECIFIC_TIME
            "interval" -> ReminderType.INTERVAL
            else -> ReminderType.SPECIFIC_TIME // fallback
        }
        val config: ReminderConfig? = when (type) {
            ReminderType.SPECIFIC_TIME -> {
                ReminderConfig.SpecificTimeConfig(
                    id = rule.reminderId?.toString() ?: UUID.randomUUID().toString(),
                    title = "${category.name} Reminder",
                    message = "It's time for your ${category.name.lowercase()} pain assessment",
                    times = listOf(
                        ReminderConfig.Time(
                            hour = rule.timeHour ?: 0,
                            minute = rule.timeMinute ?: 0
                        )
                    )
                )
            }
            ReminderType.INTERVAL -> {
                ReminderConfig.IntervalConfig(
                    id = rule.reminderId?.toString() ?: UUID.randomUUID().toString(),
                    title = "${category.name} Interval Reminder",
                    message = "It's time for your ${category.name.lowercase()} pain assessment",
                    intervalHours = rule.intervalHours ?: 0,
                    intervalMinutes = rule.intervalMinutes ?: 0
                )
            }
            else -> null
        }
        // Use the rule's reminderId if available, otherwise generate a new UUID
        val reminderId = rule.reminderId ?: UUID.randomUUID()
        // Compute activeDays as all days minus excludedDays
        val allDays = (1..7).toSet() // Calendar.SUNDAY=1 ... Calendar.SATURDAY=7
        val activeDays = allDays - rule.excludedDays

        return Reminder(
            id = reminderId,
            title = "${category.name} Reminder",
            category = category,
            type = type,
            isEnabled = true, // Default to enabled; adjust if you add this to NotificationRule
            activeDays = activeDays,
            config = config
        )
    }
}
