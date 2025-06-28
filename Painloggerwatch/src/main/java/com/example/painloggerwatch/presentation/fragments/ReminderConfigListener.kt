// File: app/src/main/java/com/example/painlogger/fragments/ReminderConfigListener.kt

package com.example.painloggerwatch.presentation.fragments

import com.example.painlogger.Reminder

interface ReminderConfigListener {
    fun onSaveReminder(reminder: Reminder)
    fun onDeleteReminder(reminderId: String) // Assuming you'll pass the ID for deletion
    // You might add other methods if needed, like onCancel()
}