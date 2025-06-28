// File: app/src/main/java/com/example/painlogger/ReminderCategory.kt

package com.example.painloggerwatch.presentation // Or your domain package

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class ReminderCategory : Parcelable {
    DETAILED,
    GENERAL
}