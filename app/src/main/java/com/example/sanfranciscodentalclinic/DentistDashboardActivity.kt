package com.example.sanfranciscodentalclinic

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.sanfranciscodentalclinic.databinding.ActivityDentistDashboardBinding
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DentistDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDentistDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var adapter: DentistScheduleAdapter
    
    private val database = FirebaseDatabase.getInstance("https://dental-clinic-f32da-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val appointmentsRef = database.getReference("Appointments")
    private val usersRef = database.getReference("Users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDentistDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setSupportActionBar(binding.toolbar)
        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.open, R.string.close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupRecyclerView()
        fetchConfirmedAppointments()
        setupNavigation()
    }

    private fun setupRecyclerView() {
        adapter = DentistScheduleAdapter(mutableListOf()) { appointment ->
            showCompleteConfirmation(appointment)
        }
        binding.recyclerView.adapter = adapter
    }

    private fun showCompleteConfirmation(appointment: Appointment) {
        val price = ProcedurePrices.getPrice(appointment.procedure)
        
        AlertDialog.Builder(this)
            .setTitle("Complete Appointment")
            .setMessage("Mark this appointment as completed?\n\nPatient: ${appointment.patientName}\nProcedure: ${appointment.procedure}\nCharge: ₱${String.format("%.2f", price)}\n\nThis will add ₱${String.format("%.2f", price)} to the patient's balance.")
            .setPositiveButton("Complete") { _, _ ->
                completeAppointment(appointment, price)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun completeAppointment(appointment: Appointment, price: Double) {
        // Update appointment status
        appointmentsRef.child(appointment.appointmentId).child("status").setValue("Completed")
            .addOnSuccessListener {
                // Add charge to patient's balance
                addChargeToPatient(appointment.patientId, price, appointment.procedure)
                Toast.makeText(this, "Appointment marked as completed", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update appointment", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addChargeToPatient(patientId: String, amount: Double, procedure: String) {
        usersRef.child(patientId).child("pendingBalance").get().addOnSuccessListener { snapshot ->
            val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0
            val newBalance = currentBalance + amount
            usersRef.child(patientId).child("pendingBalance").setValue(newBalance)
            
            // Record transaction
            val transactionsRef = database.getReference("Transactions")
            val transactionId = transactionsRef.push().key ?: return@addOnSuccessListener
            
            val transaction = Transaction(
                transactionId = transactionId,
                patientId = patientId,
                appointmentId = "",
                amount = amount,
                type = "charge",
                description = "Charge for $procedure",
                paymentMethod = "",
                status = "pending",
                createdAt = System.currentTimeMillis(),
                processedBy = auth.currentUser?.uid ?: ""
            )
            transactionsRef.child(transactionId).setValue(transaction)
        }
    }

    private fun fetchConfirmedAppointments() {
        appointmentsRef.orderByChild("status").equalTo("Confirmed").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val confirmedAppointments = mutableListOf<Appointment>()
                val userFetchJobs = mutableListOf<Pair<Appointment, Task<DataSnapshot>>>()

                for (appointmentSnapshot in snapshot.children) {
                    val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                    if (appointment != null) {
                        val task = usersRef.child(appointment.patientId).get()
                        userFetchJobs.add(Pair(appointment, task))
                        task.addOnSuccessListener { userSnapshot ->
                            val firstName = userSnapshot.child("firstName").value?.toString() ?: ""
                            val lastName = userSnapshot.child("lastName").value?.toString() ?: ""
                            val fullName = userSnapshot.child("name").value?.toString() ?: ""
                            
                            appointment.patientName = when {
                                fullName.isNotEmpty() -> fullName
                                firstName.isNotEmpty() || lastName.isNotEmpty() -> "$firstName $lastName".trim()
                                else -> "Unknown Patient"
                            }
                        }
                    }
                }

                Tasks.whenAll(userFetchJobs.map { it.second }).addOnSuccessListener {
                    val sortedAppointments = userFetchJobs.map { it.first }.sortedWith(compareBy({ it.date }, { it.time }))
                    adapter.updateAppointments(sortedAppointments)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DentistDashboardActivity, "Failed to load schedule", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_schedule -> { /* Do nothing */ }
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
}