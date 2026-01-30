package com.example.sanfranciscodentalclinic

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sanfranciscodentalclinic.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private val TAG = "DentalClinic_Debug" // Tag for Logcat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.etDateOfBirth.setOnClickListener {
            showDatePickerDialog()
        }

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePasswordStrength(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnRegister.setOnClickListener {
            performSignUp()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
                binding.etDateOfBirth.setText(selectedDate)
            },
            year,
            month,
            day
        )

        val sixMonthsAgo = Calendar.getInstance()
        sixMonthsAgo.add(Calendar.MONTH, -6)
        datePickerDialog.datePicker.maxDate = sixMonthsAgo.timeInMillis
        datePickerDialog.show()
    }

    private fun updatePasswordStrength(password: String) {
        val strengthMeter = binding.passwordStrengthMeter
        val strengthText = binding.passwordStrengthText
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++

        when (score) {
            0, 1 -> {
                strengthMeter.progress = 25
                strengthMeter.progressDrawable.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN)
                strengthText.text = "Weak"
                strengthText.setTextColor(Color.RED)
            }
            2 -> {
                strengthMeter.progress = 50
                strengthMeter.progressDrawable.setColorFilter(Color.YELLOW, android.graphics.PorterDuff.Mode.SRC_IN)
                strengthText.text = "Medium"
                strengthText.setTextColor(Color.YELLOW)
            }
            3 -> {
                strengthMeter.progress = 75
                strengthMeter.progressDrawable.setColorFilter(Color.BLUE, android.graphics.PorterDuff.Mode.SRC_IN)
                strengthText.text = "Strong"
                strengthText.setTextColor(Color.BLUE)
            }
            4 -> {
                strengthMeter.progress = 100
                strengthMeter.progressDrawable.setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN)
                strengthText.text = "Very Strong"
                strengthText.setTextColor(Color.GREEN)
            }
        }
    }

    private fun performSignUp() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val dateOfBirth = binding.etDateOfBirth.text.toString().trim()
        val selectedGenderId = binding.rgGender.checkedRadioButtonId

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || dateOfBirth.isEmpty() || selectedGenderId == -1) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || !email.contains("@")) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            return
        }

        if (phone.length != 9) {
            Toast.makeText(this, "Phone number must be 9 digits", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val gender = findViewById<RadioButton>(selectedGenderId).text.toString()

        Log.d(TAG, "Step 1: Starting Auth for $email")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    Log.d(TAG, "Step 2: Auth Successful. UID: $userId")

                    saveUserToDatabase(userId, firstName, lastName, email, phone, dateOfBirth, gender)
                } else {
                    Log.e(TAG, "Step 2 Failed: ${task.exception?.message}")
                    Toast.makeText(this, "Auth Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToDatabase(uid: String?, firstName: String, lastName: String, email: String, phone: String, dateOfBirth: String, gender: String) {
        if (uid == null) return

        val database = FirebaseDatabase.getInstance("https://dental-clinic-f32da-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users")
        val userObject = Patient(
            uid = uid,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = "+639$phone",
            dateOfBirth = dateOfBirth,
            gender = gender,
            role = "patient",
            pendingBalance = 0.0,
            createdAt = System.currentTimeMillis()
        )

        Log.d(TAG, "Step 3: Sending data object to Firebase...")

        database.child(uid).setValue(userObject)
            .addOnSuccessListener {
                Log.d(TAG, "Step 4: Database Write SUCCESS")
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_LONG).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Step 4 Failed: ${e.message}")
                Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
