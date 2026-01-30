package com.example.sanfranciscodentalclinic

data class Appointment(
    val appointmentId: String = "",
    val patientId: String = "",
    var patientName: String = "", // Will be populated later
    val date: String = "",
    val time: String = "",
    val procedure: String = "",
    val status: String = ""
)