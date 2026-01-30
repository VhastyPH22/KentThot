package com.example.sanfranciscodentalclinic

object ProcedurePrices {
    val prices = mapOf(
        "Cleaning" to 500.0,
        "Whitening" to 2500.0,
        "Extraction" to 1500.0,
        "Consultation" to 300.0,
        "Root Canal" to 8000.0,
        "Filling" to 800.0,
        "Braces Adjustment" to 1000.0,
        "Dental X-Ray" to 400.0
    )

    fun getPrice(procedure: String): Double {
        return prices[procedure] ?: 0.0
    }

    fun getAllProcedures(): List<String> {
        return prices.keys.toList()
    }
}
