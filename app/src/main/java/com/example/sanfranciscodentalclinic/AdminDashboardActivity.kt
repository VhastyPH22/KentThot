package com.example.sanfranciscodentalclinic

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.sanfranciscodentalclinic.databinding.ActivityAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var adapter: DentistScheduleAdapter
    
    private val database = FirebaseDatabase.getInstance("https://dental-clinic-f32da-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val usersRef = database.getReference("Users")
    private val appointmentsRef = database.getReference("Appointments")
    private val transactionsRef = database.getReference("Transactions")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setSupportActionBar(binding.toolbar)
        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.open, R.string.close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupRecyclerView()
        fetchAdminData()
        fetchDashboardStats()
        fetchRecentAppointments()
        setupNavigation()
    }

    private fun setupRecyclerView() {
        // Admin can also mark appointments as complete
        adapter = DentistScheduleAdapter(mutableListOf()) { appointment ->
            completeAppointment(appointment)
        }
        binding.recyclerViewAppointments.adapter = adapter
    }
    
    private fun completeAppointment(appointment: Appointment) {
        val price = ProcedurePrices.getPrice(appointment.procedure)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Complete Appointment")
            .setMessage("Mark as completed and charge ₱${String.format("%.2f", price)} to patient?")
            .setPositiveButton("Complete") { _, _ ->
                appointmentsRef.child(appointment.appointmentId).child("status").setValue("Completed")
                    .addOnSuccessListener {
                        addChargeToPatient(appointment.patientId, price, appointment.procedure)
                        Toast.makeText(this, "Appointment completed", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addChargeToPatient(patientId: String, amount: Double, procedure: String) {
        usersRef.child(patientId).child("pendingBalance").get().addOnSuccessListener { snapshot ->
            val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0
            val newBalance = currentBalance + amount
            usersRef.child(patientId).child("pendingBalance").setValue(newBalance)
            
            // Record transaction
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

    private fun fetchAdminData() {
        val uid = auth.currentUser?.uid ?: return
        usersRef.child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").value?.toString() ?: "Admin"
                binding.tvWelcomeAdmin.text = "Hello, $name 👋"

                // Update nav header
                val headerView = binding.navView.getHeaderView(0)
                headerView.findViewById<TextView>(R.id.tvHeaderName).text = name
                headerView.findViewById<TextView>(R.id.tvHeaderEmail).text = auth.currentUser?.email
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminDashboardActivity, "Failed to load admin data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchDashboardStats() {
        // Fetch user stats
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalUsers = 0
                var totalPatients = 0

                for (userSnapshot in snapshot.children) {
                    totalUsers++
                    val role = userSnapshot.child("role").value?.toString()
                    if (role == "patient") {
                        totalPatients++
                    }
                }

                binding.tvTotalUsers.text = totalUsers.toString()
                binding.tvTotalPatients.text = totalPatients.toString()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Fetch appointment stats
        appointmentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var pendingCount = 0
                var confirmedCount = 0

                for (appointmentSnapshot in snapshot.children) {
                    when (appointmentSnapshot.child("status").value?.toString()) {
                        "Pending" -> pendingCount++
                        "Confirmed" -> confirmedCount++
                    }
                }

                binding.tvPendingAppointments.text = pendingCount.toString()
                binding.tvConfirmedAppointments.text = confirmedCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchRecentAppointments() {
        appointmentsRef.limitToLast(10).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val appointments = mutableListOf<Appointment>()
                val totalCount = snapshot.childrenCount.toInt()
                var processedCount = 0

                if (totalCount == 0) {
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
                                else -> "Unknown"
                            }
                            appointments.add(appointment)
                            processedCount++
                            
                            if (processedCount == totalCount) {
                                val sortedAppointments = appointments.sortedWith(compareByDescending<Appointment> { it.date }.thenByDescending { it.time })
                                adapter.updateAppointments(sortedAppointments)
                            }
                        }.addOnFailureListener {
                            processedCount++
                            if (processedCount == totalCount) {
                                val sortedAppointments = appointments.sortedWith(compareByDescending<Appointment> { it.date }.thenByDescending { it.time })
                                adapter.updateAppointments(sortedAppointments)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminDashboardActivity, "Failed to load appointments", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_admin_dashboard -> { /* Stay here */ }
                R.id.nav_manage_users -> {
                    Toast.makeText(this, "Manage Users - Coming Soon", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_all_appointments -> {
                    Toast.makeText(this, "All Appointments - Coming Soon", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_reports -> {
                    Toast.makeText(this, "Reports - Coming Soon", Toast.LENGTH_SHORT).show()
                }
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
