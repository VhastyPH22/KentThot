package com.example.sanfranciscodentalclinic

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sanfranciscodentalclinic.databinding.ItemAppointmentRequestBinding

class AppointmentRequestAdapter(
    private var appointments: MutableList<Appointment>,
    private val onAction: (Appointment, String) -> Unit
) : RecyclerView.Adapter<AppointmentRequestAdapter.AppointmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemAppointmentRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount() = appointments.size

    fun updateAppointments(newAppointments: List<Appointment>) {
        this.appointments.clear()
        this.appointments.addAll(newAppointments)
        notifyDataSetChanged()
    }

    inner class AppointmentViewHolder(private val binding: ItemAppointmentRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appointment: Appointment) {
            val price = ProcedurePrices.getPrice(appointment.procedure)
            
            binding.tvPatientName.text = appointment.patientName
            binding.tvProcedure.text = appointment.procedure
            binding.tvAppointmentDetails.text = "${appointment.date} at ${appointment.time}"
            binding.tvPrice.text = "₱${String.format("%.2f", price)}"

            binding.btnAccept.setOnClickListener { onAction(appointment, "Confirmed") }
            binding.btnDecline.setOnClickListener { onAction(appointment, "Cancelled") }
        }
    }
}