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
    private val appointmentsRef = FirebaseDatabase.getInstance(DB_URL).getReference("Appointments")
    private val usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users")

    companion object {
        private const val DB_URL = "https://dental-clinic-f32da-default-rtdb.asia-southeast1.firebasedatabase.app"
    }

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
                for (appointmentSnapshot in snapshot.children) {
                    val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                    if (appointment != null) {
                        // Fetch patient name
                        usersRef.child(appointment.patientId).child("fullName").get().addOnSuccessListener {
                            appointment.patientName = it.value.toString()
                            appointmentRequests.add(appointment)
                            adapter.updateAppointments(appointmentRequests)
                        }
                    }
                }
                if (appointmentRequests.isEmpty()) {
                    adapter.updateAppointments(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AssistantDashboardActivity, "Failed to load requests", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateAppointmentStatus(appointment: Appointment, newStatus: String) {
        appointmentsRef.child(appointment.appointmentId).child("status").setValue(newStatus)
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_requests -> { /* Do nothing */ }
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
            val email = emailInput.text.toString()
            val amount = amountInput.text.toString().toDoubleOrNull()

            if (email.isNotBlank() && amount != null) {
                processPayment(email, amount)
            } else {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
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
                        val user = userSnapshot.getValue(User::class.java)
                        if (user != null) {
                            val currentBalance = user.pendingBalance
                            val newBalance = currentBalance - amount
                            usersRef.child(user.uid).child("pendingBalance").setValue(newBalance)
                                .addOnSuccessListener { Toast.makeText(this@AssistantDashboardActivity, "Payment Processed!", Toast.LENGTH_SHORT).show() }
                                .addOnFailureListener { Toast.makeText(this@AssistantDashboardActivity, "Payment Failed!", Toast.LENGTH_SHORT).show() }
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
}