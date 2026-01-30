package com.example.sanfranciscodentalclinic

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.sanfranciscodentalclinic.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var toggle: ActionBarDrawerToggle

    private val DB_URL = "https://dental-clinic-f32da-default-rtdb.asia-southeast1.firebasedatabase.app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setSupportActionBar(binding.toolbar)
        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.open, R.string.close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        fetchDashboardData()
        fetchNextAppointment()

        binding.btnBookAppointment.setOnClickListener {
            startActivity(Intent(this, BookAppointmentActivity::class.java))
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> { /* Stay here */ }
                R.id.nav_book_appointment -> {
                    startActivity(Intent(this, BookAppointmentActivity::class.java))
                }
                R.id.nav_my_visits -> {
                    startActivity(Intent(this, MyVisitsActivity::class.java))
                }
                R.id.nav_profile -> {
                    Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
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

    private fun fetchDashboardData() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("fullName").value?.toString() ?: "User"
                binding.tvWelcomeUser.text = "Hello, ${name.lowercase()} 👋"
                binding.tvBalance.text = "₱${snapshot.child("pendingBalance").value ?: "0"}"

                val headerView = binding.navView.getHeaderView(0)
                headerView.findViewById<TextView>(R.id.tvHeaderName).text = name
                headerView.findViewById<TextView>(R.id.tvHeaderEmail).text = auth.currentUser?.email
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchNextAppointment() {
        val uid = auth.currentUser?.uid ?: return
        val appointmentsRef = FirebaseDatabase.getInstance(DB_URL).getReference("Appointments")

        val query = appointmentsRef.orderByChild("patientId").equalTo(uid)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var latestAppointment: DataSnapshot? = null
                for (appointmentSnapshot in snapshot.children) {
                    if (appointmentSnapshot.child("status").value == "Confirmed") {
                        if (latestAppointment == null || 
                            appointmentSnapshot.child("date").value.toString() > latestAppointment.child("date").value.toString() ||
                            (appointmentSnapshot.child("date").value.toString() == latestAppointment.child("date").value.toString() &&
                             appointmentSnapshot.child("time").value.toString() > latestAppointment.child("time").value.toString())) {
                            latestAppointment = appointmentSnapshot
                        }
                    }
                }

                if (latestAppointment != null) {
                    val date = latestAppointment.child("date").value.toString()
                    val time = latestAppointment.child("time").value.toString()
                    binding.tvNextAppointment.text = "$date at $time"
                } else {
                    binding.tvNextAppointment.text = "No upcoming visits"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.tvNextAppointment.text = "Error fetching appointments"
            }
        })
    }
}