package com.example.sanfranciscodentalclinic

data class Transaction(
    val transactionId: String = "",
    val patientId: String = "",
    val appointmentId: String = "",
    val amount: Double = 0.0,
    val type: String = "", // "charge" or "payment"
    val description: String = "",
    val paymentMethod: String = "", // "cash", "gcash", "card"
    val status: String = "", // "pending", "completed", "failed"
    val createdAt: Long = System.currentTimeMillis(),
    val processedBy: String = "" // UID of assistant/admin who processed
)
