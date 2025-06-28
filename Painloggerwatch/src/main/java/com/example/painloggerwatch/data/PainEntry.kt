package com.example.painloggerwatch.data

import kotlinx.serialization.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Serializable
@Parcelize
data class PainEntry(
    val timestamp: Long,
    val generalPainLevel: Int,
    val detailedEntries: List<DetailedPainEntry> = emptyList(),
    val notes: String = "",
    val deviceSource: String = "watch" // "watch" or "phone"
) : Parcelable

@Serializable
@Parcelize
data class DetailedPainEntry(
    val bodyPart: String,
    val intensity: Int = 0,
    val notes: String = ""
) : Parcelable