package com.example.sanfranciscodentalclinic

data class Patient(
    val uid: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val role: String? = null,
    val pendingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
