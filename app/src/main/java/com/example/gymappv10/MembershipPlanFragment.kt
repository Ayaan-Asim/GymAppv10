package com.example.gymappv10

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class MembershipPlanFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val planList = mutableListOf<PlansModel>()
    private lateinit var adapter: PlansAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_membership_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvPlans)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = PlansAdapter(planList) { selectedPlan ->
            val bundle = Bundle().apply {
                putString("planName", selectedPlan.name)
                putString("planDuration", selectedPlan.duration)
                putString("planFeatures", selectedPlan.features)
                putString("planPrice", selectedPlan.price.toString())
                putString("planOfferPrice", selectedPlan.offerPrice.toString())
                putString("planOfferDuration", selectedPlan.offerDuration)
            }

            val fragment = PlanPaymentFragment()
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerView.adapter = adapter

        loadPlansFromFirebase()
    }

    private fun loadPlansFromFirebase() {
        db.collection("membership_plans")
            .get()
            .addOnSuccessListener { result ->
                planList.clear()
                for (doc in result) {
                    val plan = doc.toObject(PlansModel::class.java)
                    planList.add(plan)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Handle error
            }
    }
}
