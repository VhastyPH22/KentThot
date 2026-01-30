package com.example.sanfranciscodentalclinic

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sanfranciscodentalclinic.databinding.ItemAppointmentScheduleBinding

class DentistScheduleAdapter(
    private var appointments: MutableList<Appointment>
) : RecyclerView.Adapter<DentistScheduleAdapter.ScheduleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemAppointmentScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount() = appointments.size

    fun updateAppointments(newAppointments: List<Appointment>) {
        this.appointments.clear()
        this.appointments.addAll(newAppointments)
        notifyDataSetChanged()
    }

    inner class ScheduleViewHolder(private val binding: ItemAppointmentScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appointment: Appointment) {
            binding.tvPatientName.text = appointment.patientName
            binding.tvAppointmentDetails.text = "${appointment.date} at ${appointment.time} for ${appointment.procedure}"
        }
    }
}