package com.example.sanfranciscodentalclinic

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.sanfranciscodentalclinic.databinding.ActivityAssistantDashboardBinding
import com.google.firebase.auth.FirebaseAuth

class AssistantDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssistantDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var adapter: AppointmentRequestAdapter

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
        setupNavigation() // Restore navigation
    }

    override fun onResume() {
        super.onResume()
        // Use onResume to refresh the list every time the screen is viewed
        fetchAppointmentRequests()
    }

    private fun setupRecyclerView() {
        adapter = AppointmentRequestAdapter(mutableListOf()) { appointment, action ->
            // In our simulation, clicking "Confirm" will change the status
            // and the item will disappear from this list on the next refresh.
            appointment.status = action // Sets status to "Confirmed"
            Toast.makeText(this, "${appointment.patientName} ${action.lowercase()}", Toast.LENGTH_SHORT).show()
            fetchAppointmentRequests() // Refresh the list immediately after action
        }
        binding.recyclerView.adapter = adapter
    }

    private fun fetchAppointmentRequests() {
        // --- SIMULATION ONLY ---
        // Read from the shared, in-memory list of appointments.
        val pendingAppointments = SimulatedData.appointments.filter { it.status == "Pending" }

        if (pendingAppointments.isEmpty()) {
            binding.tvNoAppointments.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvNoAppointments.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            adapter.updateAppointments(pendingAppointments.sortedBy { it.timestamp })
        }
        // --- END SIMULATION ---
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
                Toast.makeText(this, "Simulated payment for $email of ₱$amount", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter valid email and amount", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}