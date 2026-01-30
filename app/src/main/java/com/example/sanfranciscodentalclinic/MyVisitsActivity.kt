package com.example.sanfranciscodentalclinic

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sanfranciscodentalclinic.databinding.ActivityMyVisitsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyVisitsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyVisitsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: MyVisitsAdapter
    private val allAppointments = mutableListOf<Appointment>()
    
    private val appointmentsRef = FirebaseDatabase.getInstance("https://dental-clinic-f32da-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Appointments")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyVisitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        fetchAppointments()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = MyVisitsAdapter(mutableListOf()) { appointment ->
            // Navigate to payment
            val intent = Intent(this, PaymentActivity::class.java)
            intent.putExtra("appointmentId", appointment.appointmentId)
            intent.putExtra("procedure", appointment.procedure)
            intent.putExtra("amount", ProcedurePrices.getPrice(appointment.procedure))
            startActivity(intent)
        }
        binding.recyclerViewVisits.adapter = adapter
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipAll -> filterAppointments(null)
                R.id.chipPending -> filterAppointments("Pending")
                R.id.chipConfirmed -> filterAppointments("Confirmed")
                R.id.chipCompleted -> filterAppointments("Completed")
            }
        }
    }

    private fun fetchAppointments() {
        val uid = auth.currentUser?.uid ?: return

        appointmentsRef.orderByChild("patientId").equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allAppointments.clear()

                    for (appointmentSnapshot in snapshot.children) {
                        val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                        if (appointment != null) {
                            allAppointments.add(appointment)
                        }
                    }

                    // Sort by date (newest first)
                    allAppointments.sortByDescending { it.date }
                    filterAppointments(null)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MyVisitsActivity, "Failed to load appointments", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun filterAppointments(status: String?) {
        val filtered = if (status == null) {
            allAppointments
        } else {
            allAppointments.filter { it.status == status }
        }

        if (filtered.isEmpty()) {
            binding.tvNoAppointments.visibility = View.VISIBLE
            binding.recyclerViewVisits.visibility = View.GONE
        } else {
            binding.tvNoAppointments.visibility = View.GONE
            binding.recyclerViewVisits.visibility = View.VISIBLE
            adapter.updateAppointments(filtered)
        }
    }
}