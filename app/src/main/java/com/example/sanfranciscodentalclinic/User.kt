package com.example.sanfranciscodentalclinic

data class User(
    val uid: String = "",
    val fullName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val pendingBalance: Double = 0.0,
    val createdAt: Long = 0
) {
    fun getDisplayName(): String {
        return when {
            name.isNotEmpty() -> name
            fullName.isNotEmpty() -> fullName
            firstName.isNotEmpty() || lastName.isNotEmpty() -> "$firstName $lastName".trim()
            else -> "User"
        }
    }
}