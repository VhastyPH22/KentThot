package com.example.sanfranciscodentalclinic

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sanfranciscodentalclinic.databinding.ItemAppointmentScheduleBinding

class DentistScheduleAdapter(
    private var appointments: MutableList<Appointment>,
    private val onCompleteClick: ((Appointment) -> Unit)? = null
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
            
            val price = ProcedurePrices.getPrice(appointment.procedure)
            binding.tvPrice.text = "₱${String.format("%.2f", price)}"
            
            if (appointment.status == "Confirmed" && onCompleteClick != null) {
                binding.btnComplete.visibility = View.VISIBLE
                binding.btnComplete.setOnClickListener { onCompleteClick.invoke(appointment) }
                binding.tvStatus.text = "Confirmed"
                binding.tvStatus.setTextColor(Color.parseColor("#2196F3"))
            } else {
                binding.btnComplete.visibility = View.GONE
                binding.tvStatus.text = appointment.status
                when (appointment.status) {
                    "Completed" -> binding.tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                    "Cancelled" -> binding.tvStatus.setTextColor(Color.parseColor("#F44336"))
                    else -> binding.tvStatus.setTextColor(Color.parseColor("#666666"))
                }
            }
        }
    }
}
