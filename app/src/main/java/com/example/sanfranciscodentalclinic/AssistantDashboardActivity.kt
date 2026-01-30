package com.example.sanfranciscodentalclinic

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.sanfranciscodentalclinic.databinding.ActivityAssistantDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AssistantDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssistantDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var adapter: AppointmentRequestAdapter
    
    private val database = FirebaseDatabase.getInstance("https://dental-clinic-f32da-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val appointmentsRef = database.getReference("Appointments")
    private val usersRef = database.getReference("Users")
    private val transactionsRef = database.getReference("Transactions")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssistantDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setSupportActionBar(binding.toolbar)
        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.open, R.string.close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupRecyclerView()
        fetchAppointmentRequests()
        setupNavigation()
    }

    private fun setupRecyclerView() {
        adapter = AppointmentRequestAdapter(mutableListOf()) { appointment, action ->
            updateAppointmentStatus(appointment, action)
        }
        binding.recyclerView.adapter = adapter
    }

    private fun fetchAppointmentRequests() {
        appointmentsRef.orderByChild("status").equalTo("Pending").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val appointmentRequests = mutableListOf<Appointment>()
                val pendingCount = snapshot.childrenCount.toInt()
                var processedCount = 0
                
                if (pendingCount == 0) {
                    adapter.updateAppointments(emptyList())
                    return
                }
                
                for (appointmentSnapshot in snapshot.children) {
                    val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                    if (appointment != null) {
                        usersRef.child(appointment.patientId).get().addOnSuccessListener { userSnapshot ->
                            val firstName = userSnapshot.child("firstName").value?.toString() ?: ""
                            val lastName = userSnapshot.child("lastName").value?.toString() ?: ""
                            val fullName = userSnapshot.child("name").value?.toString() ?: ""
                            
                            appointment.patientName = when {
                                fullName.isNotEmpty() -> fullName
                                firstName.isNotEmpty() || lastName.isNotEmpty() -> "$firstName $lastName".trim()
                                else -> "Unknown Patient"
                            }
                            appointmentRequests.add(appointment)
                            processedCount++
                            
                            if (processedCount == pendingCount) {
                                adapter.updateAppointments(appointmentRequests.sortedWith(compareBy({ it.date }, { it.time })))
                            }
                        }.addOnFailureListener {
                            processedCount++
                            if (processedCount == pendingCount) {
                                adapter.updateAppointments(appointmentRequests.sortedWith(compareBy({ it.date }, { it.time })))
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AssistantDashboardActivity, "Failed to load requests", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateAppointmentStatus(appointment: Appointment, newStatus: String) {
        val action = if (newStatus == "Confirmed") "confirm" else "decline"
        
        AlertDialog.Builder(this)
            .setTitle("${if (newStatus == "Confirmed") "Confirm" else "Decline"} Appointment")
            .setMessage("Are you sure you want to $action this appointment?\n\nPatient: ${appointment.patientName}\nProcedure: ${appointment.procedure}\nDate: ${appointment.date} at ${appointment.time}")
            .setPositiveButton("Yes") { _, _ ->
                appointmentsRef.child(appointment.appointmentId).child("status").setValue(newStatus)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Appointment ${newStatus.lowercase()}", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update appointment", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_requests -> { /* Already here */ }
                R.id.nav_billing -> showBillingDialog()
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun showBillingDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Process a Payment")

        val view = layoutInflater.inflate(R.layout.dialog_billing, null)
        val emailInput = view.findViewById<EditText>(R.id.etPatientEmail)
        val amountInput = view.findViewById<EditText>(R.id.etAmount)
        builder.setView(view)

        builder.setPositiveButton("Process") { dialog, _ ->
            val email = emailInput.text.toString().trim()
            val amount = amountInput.text.toString().toDoubleOrNull()

            if (email.isNotBlank() && amount != null && amount > 0) {
                processPayment(email, amount)
            } else {
                Toast.makeText(this, "Please enter valid email and amount", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
    
    private fun processPayment(email: String, amount: Double) {
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val patientId = userSnapshot.key ?: continue
                        val currentBalance = userSnapshot.child("pendingBalance").getValue(Double::class.java) ?: 0.0
                        
                        if (amount > currentBalance) {
                            Toast.makeText(this@AssistantDashboardActivity, 
                                "Amount exceeds balance. Current balance: ₱${String.format("%.2f", currentBalance)}", 
                                Toast.LENGTH_SHORT).show()
                            return
                        }
                        
                        val newBalance = currentBalance - amount
                        usersRef.child(patientId).child("pendingBalance").setValue(newBalance)
                            .addOnSuccessListener { 
                                // Record transaction
                                recordPaymentTransaction(patientId, amount)
                                Toast.makeText(this@AssistantDashboardActivity, 
                                    "Payment of ₱${String.format("%.2f", amount)} processed!\nNew balance: ₱${String.format("%.2f", newBalance)}", 
                                    Toast.LENGTH_LONG).show() 
                            }
                            .addOnFailureListener { 
                                Toast.makeText(this@AssistantDashboardActivity, "Payment Failed!", Toast.LENGTH_SHORT).show() 
                            }
                    }
                } else {
                    Toast.makeText(this@AssistantDashboardActivity, "Patient not found!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AssistantDashboardActivity, "Database error!", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun recordPaymentTransaction(patientId: String, amount: Double) {
        val transactionId = transactionsRef.push().key ?: return
        
        val transaction = Transaction(
            transactionId = transactionId,
            patientId = patientId,
            appointmentId = "",
            amount = amount,
            type = "payment",
            description = "In-office payment",
            paymentMethod = "cash",
            status = "completed",
            createdAt = System.currentTimeMillis(),
            processedBy = auth.currentUser?.uid ?: ""
        )
        
        transactionsRef.child(transactionId).setValue(transaction)
    }
}