package com.example.sanfranciscodentalclinic

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.sanfranciscodentalclinic.databinding.ActivityAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var adapter: DentistScheduleAdapter

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
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Use onResume to refresh the lists every time the screen is viewed
        fetchDashboardStats()
        fetchRecentAppointments()
    }

    private fun setupRecyclerView() {
        adapter = DentistScheduleAdapter(mutableListOf()) { appointment ->
            completeAppointment(appointment)
        }
        binding.recyclerViewAppointments.adapter = adapter
    }

    private fun completeAppointment(appointment: Appointment) {
        val price = ProcedurePrices.getPrice(appointment.procedure)

        AlertDialog.Builder(this)
            .setTitle("Complete Appointment")
            .setMessage("Mark as completed and charge ₱${String.format("%.2f", price)} to patient?")
            .setPositiveButton("Complete") { _, _ ->
                // In our simulation, this changes the status
                appointment.status = "Completed"
                Toast.makeText(this, "${appointment.patientName}'s appointment completed", Toast.LENGTH_SHORT).show()
                fetchRecentAppointments() // Refresh the list
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchAdminData() {
        // Simulated admin data
        binding.tvWelcomeAdmin.text = "Hello, Admin 👋"
        val headerView = binding.navView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.tvHeaderName).text = "Admin"
        headerView.findViewById<TextView>(R.id.tvHeaderEmail).text = "admin@example.com"
    }

    private fun fetchDashboardStats() {
        // --- SIMULATION ONLY ---
        val pendingCount = SimulatedData.appointments.count { it.status == "Pending" }
        val confirmedCount = SimulatedData.appointments.count { it.status == "Confirmed" }
        
        // These stats are no longer relevant in the simulation but we'll clear them
        binding.tvTotalUsers.text = "-"
        binding.tvTotalPatients.text = "-"
        binding.tvPendingAppointments.text = pendingCount.toString()
        binding.tvConfirmedAppointments.text = confirmedCount.toString()
        // --- END SIMULATION ---
    }

    private fun fetchRecentAppointments() {
        // --- SIMULATION ONLY ---
        // Read from the shared, in-memory list of appointments.
        val confirmedAppointments = SimulatedData.appointments.filter { it.status == "Confirmed" }

        if (confirmedAppointments.isEmpty()) {
            binding.tvNoAppointments.visibility = View.VISIBLE
            binding.recyclerViewAppointments.visibility = View.GONE
        } else {
            binding.tvNoAppointments.visibility = View.GONE
            binding.recyclerViewAppointments.visibility = View.VISIBLE
            adapter.updateAppointments(confirmedAppointments.sortedByDescending { it.timestamp })
        }
        // --- END SIMULATION ---
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