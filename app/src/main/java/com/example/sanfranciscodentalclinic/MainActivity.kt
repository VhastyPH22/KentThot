package com.example.sanfranciscodentalclinic

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sanfranciscodentalclinic.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            fetchUserRoleAndRedirect()
                        } else {
                            Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Enter your email first to reset password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchUserRoleAndRedirect() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance("https://dental-clinic-f32da-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val role = snapshot.child("role").value?.toString()
                Toast.makeText(this@MainActivity, "Login Successful", Toast.LENGTH_SHORT).show()

                val intent = when (role) {
                    "patient" -> Intent(this@MainActivity, DashboardActivity::class.java)
                    "admin" -> Intent(this@MainActivity, AdminDashboardActivity::class.java)
                    "assistant" -> Intent(this@MainActivity, AssistantDashboardActivity::class.java)
                    "dentist" -> Intent(this@MainActivity, DentistDashboardActivity::class.java)
                    else -> {
                        Toast.makeText(this@MainActivity, "Unknown role: $role", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                startActivity(intent)
                finish()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to get user role: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
