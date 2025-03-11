package com.example.sofrehmessina.util

import android.os.Parcel

/**
 * Utility functions for safely working with Parcelable objects
 */
object ParcelUtils {
    /**
     * Safely writes a potentially null string to a Parcel
     * @param parcel The Parcel to write to
     * @param value The string value which may be null
     */
    fun writeStringToParcel(parcel: Parcel, value: String?) {
        parcel.writeString(value ?: "")
    }
    
    /**
     * Safely reads a string from a Parcel, handling null values
     * @param parcel The Parcel to read from
     * @return A non-null string (empty string if the value was null)
     */
    fun readStringFromParcel(parcel: Parcel): String {
        return parcel.readString() ?: ""
    }
    
    /**
     * Safely handles Date objects in Parcels by converting to Long
     * @param parcel The Parcel to write to
     * @param date The date which may be null
     */
    fun writeDateToParcel(parcel: Parcel, date: java.util.Date?) {
        if (date == null) {
            parcel.writeLong(-1)
        } else {
            parcel.writeLong(date.time)
        }
    }
    
    /**
     * Safely reads a Date from a Parcel
     * @param parcel The Parcel to read from
     * @return A Date object or null if no valid date was stored
     */
    fun readDateFromParcel(parcel: Parcel): java.util.Date? {
        val time = parcel.readLong()
        return if (time == -1L) null else java.util.Date(time)
    }
} 