package com.example.painloggerwatch.presentation // Or wherever your domain Reminder is

import android.os.Parcel
import android.os.Parcelable
import com.example.painlogger.data.model.ReminderConfig
import com.example.painlogger.ReminderCategory
import com.example.painlogger.ReminderType

import java.util.UUID
import java.util.HashSet // Use HashSet for mutable Set handling in Parceling

// Remove @Parcelize from here
data class Reminder(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val category: ReminderCategory,
    val type: ReminderType,
    val isEnabled: Boolean,
    val activeDays: Set<Int>,
    val config: ReminderConfig? // This is the property we need to handle manually
) : Parcelable {

    // 1. Implement writeToParcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        // Write each property
        parcel.writeSerializable(id) // UUID is Serializable
        parcel.writeString(title)
        parcel.writeSerializable(category) // Assuming ReminderCategory is Serializable
        parcel.writeSerializable(type) // Assuming ReminderType is Serializable
        parcel.writeByte(if (isEnabled) 1 else 0) // Booleans are written as bytes

        // Parceling Set<Int>: Convert to List for easy parceling
        parcel.writeList(activeDays.toList() as List<*>) // Write the list

        // Manually handle the nullable ReminderConfig?
        if (config == null) {
            parcel.writeByte(0) // Write 0 if config is null
        } else {
            parcel.writeByte(1) // Write 1 if config is not null
            // Write the ReminderConfig object (which is Parcelable)
            // This requires ReminderConfig and its subtypes to be Parcelable (@Parcelize)
            parcel.writeParcelable(config as Parcelable, flags) // Write the actual object
        }
    }

    // 2. Implement describeContents
    override fun describeContents(): Int {
        return 0 // Usually 0
    }

    // 3. Implement the CREATOR object
    companion object CREATOR : Parcelable.Creator<Reminder> {
        override fun createFromParcel(parcel: Parcel): Reminder {
            return Reminder(parcel)
        }

        override fun newArray(size: Int): Array<Reminder?> {
            return arrayOfNulls(size)
        }
    }

    // 4. Secondary constructor to read from Parcel
    private constructor(parcel: Parcel) : this(
        id = parcel.readSerializable() as UUID, // Read UUID
        title = parcel.readString() ?: "", // Read String, provide default if null
        category = parcel.readSerializable() as ReminderCategory, // Read Category
        type = parcel.readSerializable() as ReminderType, // Read Type
        isEnabled = parcel.readByte().toInt() != 0, // Read boolean
        // Read List<Int> using readArrayList and convert back to Set<Int>
        activeDays = parcel.readArrayList(Int::class.java.classLoader)?.filterIsInstance<Int>()?.toHashSet() ?: HashSet(), // Corrected reading logic
        // Manually handle nullable ReminderConfig?
        config = if (parcel.readByte().toInt() == 0) null else {
            // Read the Parcelable ReminderConfig object
            parcel.readParcelable(ReminderConfig::class.java.classLoader) as? ReminderConfig // Read the object
        }
    )
}