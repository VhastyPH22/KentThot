package com.example.sanfranciscodentalclinic

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sanfranciscodentalclinic.databinding.ActivityBookAppointmentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class BookAppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookAppointmentBinding
    private lateinit var auth: FirebaseAuth
    private val calendar = Calendar.getInstance()
    
    private val procedures = arrayOf("Cleaning", "Whitening", "Extraction", "Consultation", "Root Canal", "Filling", "Braces Adjustment", "Dental X-Ray")

    val appointmentsRef = FirebaseDatabase.getInstance("https://dental-clinic-f32da-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Appointments")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnBack.setOnClickListener { finish() }

        setupSpinners()
        updatePriceDisplay()

        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSubmitAppointment.setOnClickListener {
            submitAppointment()
        }
    }

    private fun setupSpinners() {
        // Time Spinner
        val timeSlots = arrayOf("09:00 AM", "10:00 AM", "11:00 AM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM")
        val timeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeSlots)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTime.adapter = timeAdapter

        // Procedure Spinner with prices
        val proceduresWithPrices = procedures.map { 
            "$it - ₱${String.format("%.2f", ProcedurePrices.getPrice(it))}"
        }.toTypedArray()
        val procedureAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, proceduresWithPrices)
        procedureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProcedure.adapter = procedureAdapter
        
        binding.spinnerProcedure.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updatePriceDisplay()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun updatePriceDisplay() {
        val selectedProcedure = procedures[binding.spinnerProcedure.selectedItemPosition]
        val price = ProcedurePrices.getPrice(selectedProcedure)
        binding.tvEstimatedPrice.text = "₱${String.format("%.2f", price)}"
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }
        
        // Set minimum date to today
        val datePicker = DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun updateDateInView() {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.tvSelectedDate.text = sdf.format(calendar.time)
    }

    private fun submitAppointment() {
        val patientId = auth.currentUser?.uid
        if (patientId == null) {
            Toast.makeText(this, "You must be logged in to book an appointment", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDate = binding.tvSelectedDate.text.toString()
        if (selectedDate == "No date selected") {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedTime = binding.spinnerTime.selectedItem.toString()
        val selectedProcedure = procedures[binding.spinnerProcedure.selectedItemPosition]

        val appointmentId = appointmentsRef.push().key

        if (appointmentId == null) {
            Toast.makeText(this, "Failed to create appointment. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        val appointment = hashMapOf(
            "appointmentId" to appointmentId,
            "patientId" to patientId,
            "date" to selectedDate,
            "time" to selectedTime,
            "procedure" to selectedProcedure,
            "status" to "Pending"
        )

        appointmentsRef.child(appointmentId).setValue(appointment)
            .addOnSuccessListener {
                Toast.makeText(this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
                finish() // Go back to the dashboard
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to book appointment: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}