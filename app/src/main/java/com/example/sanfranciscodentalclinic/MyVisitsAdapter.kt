package com.example.sanfranciscodentalclinic

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class MyVisitsAdapter(
    private var appointments: MutableList<Appointment>,
    private val onPayClick: ((Appointment) -> Unit)? = null
) : RecyclerView.Adapter<MyVisitsAdapter.VisitViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_my_visit, parent, false)
        return VisitViewHolder(view)
    }

    override fun onBindViewHolder(holder: VisitViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount() = appointments.size

    fun updateAppointments(newAppointments: List<Appointment>) {
        this.appointments.clear()
        this.appointments.addAll(newAppointments)
        notifyDataSetChanged()
    }

    inner class VisitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardVisit)
        private val tvProcedure: TextView = itemView.findViewById(R.id.tvProcedure)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val btnPay: Button = itemView.findViewById(R.id.btnPay)

        fun bind(appointment: Appointment) {
            tvProcedure.text = appointment.procedure
            tvDateTime.text = "${appointment.date} at ${appointment.time}"
            tvStatus.text = appointment.status
            
            val price = ProcedurePrices.getPrice(appointment.procedure)
            tvPrice.text = "₱${String.format("%.2f", price)}"

            // Set status color
            when (appointment.status) {
                "Pending" -> {
                    tvStatus.setTextColor(Color.parseColor("#FF9800"))
                    tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
                }
                "Confirmed" -> {
                    tvStatus.setTextColor(Color.parseColor("#2196F3"))
                    tvStatus.setBackgroundResource(R.drawable.status_confirmed_bg)
                }
                "Completed" -> {
                    tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                    tvStatus.setBackgroundResource(R.drawable.status_completed_bg)
                }
                "Cancelled" -> {
                    tvStatus.setTextColor(Color.parseColor("#F44336"))
                    tvStatus.setBackgroundResource(R.drawable.status_cancelled_bg)
                }
            }

            // Show pay button only for completed appointments
            if (appointment.status == "Completed" && onPayClick != null) {
                btnPay.visibility = View.VISIBLE
                btnPay.setOnClickListener { onPayClick.invoke(appointment) }
            } else {
                btnPay.visibility = View.GONE
            }
        }
    }
}
