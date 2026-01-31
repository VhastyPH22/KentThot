package com.example.sanfranciscodentalclinic

import com.google.firebase.database.Exclude
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Appointment(
    val appointmentId: String = "",
    val patientId: String = "",
    var patientName: String = "", // Will be populated later
    val procedure: String = "",
    var status: String = "", // Changed to var for simulation
    val timestamp: Long = 0L
) {
    @get:Exclude
    val date: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date(timestamp))
        }

    @get:Exclude
    val time: String
        get() {
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            return sdf.format(Date(timestamp))
        }
}