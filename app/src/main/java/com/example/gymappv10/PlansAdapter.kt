package com.example.gymappv10

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlansAdapter(
    private val planList: List<PlansModel>,
    private val onItemClick: (PlansModel) -> Unit
) : RecyclerView.Adapter<PlansAdapter.PlanViewHolder>() {

    class PlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvClassName)
        val tvDuration: TextView = itemView.findViewById(R.id.tvPlanDuration)
        val tvFeatures: TextView = itemView.findViewById(R.id.tvPlanFeatures)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPlanPrice)
        val tvOffer: TextView = itemView.findViewById(R.id.tvPlanOffer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plan_card, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = planList[position]

        holder.tvName.text = plan.name
        holder.tvDuration.text = "Duration: ${plan.duration}"
        holder.tvFeatures.text = "Features: ${plan.features}"
        holder.tvPrice.text = "Price: ₹${plan.price}"
        holder.tvOffer.text = "Offer: ₹${plan.offerPrice} for ${plan.offerDuration}"

        holder.itemView.setOnClickListener {
            onItemClick(plan)
        }
    }

    override fun getItemCount(): Int = planList.size
}
