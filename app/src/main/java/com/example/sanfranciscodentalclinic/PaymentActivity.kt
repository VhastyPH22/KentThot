package com.example.sanfranciscodentalclinic

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sanfranciscodentalclinic.databinding.ActivityPaymentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var auth: FirebaseAuth

    private var appointmentId: String = ""
    private var procedure: String = ""
    private var amount: Double = 0.0

    private val database = FirebaseDatabase.getInstance("https://dental-clinic-f32da-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val transactionsRef = database.getReference("Transactions")
    private val usersRef = database.getReference("Users")
    private val appointmentsRef = database.getReference("Appointments")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Get data from intent
        appointmentId = intent.getStringExtra("appointmentId") ?: ""
        procedure = intent.getStringExtra("procedure") ?: ""
        amount = intent.getDoubleExtra("amount", 0.0)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tvProcedure.text = procedure
        binding.tvAmount.text = "₱${String.format("%.2f", amount)}"
    }

    private fun setupListeners() {
        binding.rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbGcash -> {
                    binding.cardGcashDetails.visibility = View.VISIBLE
                    binding.cardCardDetails.visibility = View.GONE
                }
                R.id.rbCash -> {
                    binding.cardGcashDetails.visibility = View.GONE
                    binding.cardCardDetails.visibility = View.GONE
                }
                R.id.rbCard -> {
                    binding.cardGcashDetails.visibility = View.GONE
                    binding.cardCardDetails.visibility = View.VISIBLE
                }
            }
        }

        binding.btnPay.setOnClickListener {
            processPayment()
        }
    }

    private fun processPayment() {
        val paymentMethod = when (binding.rgPaymentMethod.checkedRadioButtonId) {
            R.id.rbGcash -> "gcash"
            R.id.rbCash -> "cash"
            R.id.rbCard -> "card"
            else -> "unknown"
        }

        // Validate payment details based on method
        when (paymentMethod) {
            "gcash" -> {
                val gcashNumber = binding.etGcashNumber.text.toString()
                if (gcashNumber.length != 11) {
                    Toast.makeText(this, "Please enter a valid GCash number", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            "card" -> {
                val cardNumber = binding.etCardNumber.text.toString()
                val expiry = binding.etExpiry.text.toString()
                val cvv = binding.etCvv.text.toString()

                if (cardNumber.length != 16) {
                    Toast.makeText(this, "Please enter a valid card number", Toast.LENGTH_SHORT).show()
                    return
                }
                if (expiry.length != 5) {
                    Toast.makeText(this, "Please enter expiry in MM/YY format", Toast.LENGTH_SHORT).show()
                    return
                }
                if (cvv.length != 3) {
                    Toast.makeText(this, "Please enter a valid CVV", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        // Show processing dialog
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Processing Payment")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        // Simulate payment processing
        Handler(Looper.getMainLooper()).postDelayed({
            progressDialog.dismiss()
            
            if (paymentMethod == "cash") {
                // For cash, just record the intent to pay
                recordTransaction(paymentMethod, "pending")
                showSuccessDialog("Payment Recorded", "Please pay ₱${String.format("%.2f", amount)} at the clinic.")
            } else {
                // For digital payments, simulate success
                recordTransaction(paymentMethod, "completed")
                updateUserBalance()
                markAppointmentPaid()
                showSuccessDialog("Payment Successful", "Your payment of ₱${String.format("%.2f", amount)} has been processed.")
            }
        }, 2000)
    }

    private fun recordTransaction(paymentMethod: String, status: String) {
        val uid = auth.currentUser?.uid ?: return
        val transactionId = transactionsRef.push().key ?: return

        val transaction = Transaction(
            transactionId = transactionId,
            patientId = uid,
            appointmentId = appointmentId,
            amount = amount,
            type = "payment",
            description = "Payment for $procedure",
            paymentMethod = paymentMethod,
            status = status,
            createdAt = System.currentTimeMillis(),
            processedBy = if (status == "completed") "system" else ""
        )

        transactionsRef.child(transactionId).setValue(transaction)
    }

    private fun updateUserBalance() {
        val uid = auth.currentUser?.uid ?: return

        usersRef.child(uid).child("pendingBalance").get().addOnSuccessListener { snapshot ->
            val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0
            val newBalance = (currentBalance - amount).coerceAtLeast(0.0)
            usersRef.child(uid).child("pendingBalance").setValue(newBalance)
        }
    }

    private fun markAppointmentPaid() {
        if (appointmentId.isNotEmpty()) {
            appointmentsRef.child(appointmentId).child("paid").setValue(true)
        }
    }

    private fun showSuccessDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
