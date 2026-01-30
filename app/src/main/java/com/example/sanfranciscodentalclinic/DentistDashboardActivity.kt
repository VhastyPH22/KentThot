package com.example.sanfranciscodentalclinic

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
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
    private val appointmentsRef = FirebaseDatabase.getInstance(DB_URL).getReference("Appointments")
    private val usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users")

    companion object {
        private const val DB_URL = "https://dental-clinic-f32da-default-rtdb.asia-southeast1.firebasedatabase.app"
    }

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
        adapter = DentistScheduleAdapter(mutableListOf())
        binding.recyclerView.adapter = adapter
    }

    private fun fetchConfirmedAppointments() {
        appointmentsRef.orderByChild("status").equalTo("Confirmed").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val confirmedAppointments = mutableListOf<Appointment>()
                val userFetchJobs = mutableListOf<Pair<Appointment, Task<DataSnapshot>>>()

                for (appointmentSnapshot in snapshot.children) {
                    val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                    if (appointment != null) {
                        // Fetch patient name and add it to a list of jobs
                        val task = usersRef.child(appointment.patientId).child("fullName").get()
                        userFetchJobs.add(Pair(appointment, task))
                        task.addOnSuccessListener {
                            appointment.patientName = it.value.toString()
                        }
                    }
                }
                
                // Once all user names are fetched, sort and update the adapter
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