package com.example.sanfranciscodentalclinic

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "",
    val pendingBalance: Double = 0.0
)