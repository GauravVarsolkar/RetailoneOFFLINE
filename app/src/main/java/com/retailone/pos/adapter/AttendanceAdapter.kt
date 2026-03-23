package com.retailone.pos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.databinding.AttendanceItemLayoutBinding
import com.retailone.pos.models.AttendanceModel.MonthlyAttendance
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.Sale

class AttendanceAdapter(private val attendancelist: List<MonthlyAttendance>, val context: Context)  : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> (){


    class AttendanceViewHolder (val binding:AttendanceItemLayoutBinding) : RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        return AttendanceViewHolder(AttendanceItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {

        val attendance_item = attendancelist[position]

        holder.binding.month.text = attendance_item?.month+" "+attendance_item?.year
        holder.binding.workingDays.text = attendance_item?.total_attendance.toString()

    }


    override fun getItemCount(): Int {
        return attendancelist.size
    }




}