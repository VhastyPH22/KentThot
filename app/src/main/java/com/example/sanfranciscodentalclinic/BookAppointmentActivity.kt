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
import java.text.SimpleDateFormat
import java.util.*

// --- SIMULATION ONLY ---
// This companion object holds a fake in-memory database for the simulation.
object SimulatedData { 
    val appointments = mutableListOf<Appointment>() 
}
// --- END SIMULATION ---

class BookAppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookAppointmentBinding
    private lateinit var auth: FirebaseAuth
    private val calendar = Calendar.getInstance()
    
    private val procedures = arrayOf("Cleaning", "Whitening", "Extraction", "Consultation", "Root Canal", "Filling", "Braces Adjustment", "Dental X-Ray")

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
        val timeSlots = arrayOf("09:00 AM", "10:00 AM", "11:00 AM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM")
        val timeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeSlots)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTime.adapter = timeAdapter

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
            
            val myFormat = "yyyy-MM-dd"
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            binding.tvSelectedDate.text = sdf.format(calendar.time)
        }
        
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

    private fun submitAppointment() {
        val patientId = auth.currentUser?.uid ?: "simulated_user"

        if (binding.tvSelectedDate.text == "No date selected") {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedTime = binding.spinnerTime.selectedItem.toString()
        val selectedProcedure = procedures[binding.spinnerProcedure.selectedItemPosition]
        
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        val date = sdf.parse(selectedTime)
        val timeCalendar = Calendar.getInstance()
        if (date != null) {
            timeCalendar.time = date
        }
        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))

        // --- SIMULATION ONLY ---
        val newAppointment = Appointment(
            appointmentId = UUID.randomUUID().toString(),
            patientId = patientId,
            patientName = "Simulated Patient", // In a real app, you'd get this from user data
            procedure = selectedProcedure,
            status = "Pending",
            timestamp = calendar.timeInMillis
        )

        SimulatedData.appointments.add(newAppointment)
        
        Toast.makeText(this, "Appointment booked successfully! (Simulation)", Toast.LENGTH_SHORT).show()
        finish()
        // --- END SIMULATION ---
    }
}