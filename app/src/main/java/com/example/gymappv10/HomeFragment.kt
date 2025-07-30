package com.example.gymappv10

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.findViewTreeViewModelStoreOwner

class HomeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { return inflater.inflate(R.layout.fragment_home, container, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    val btnRenewMembership = view.findViewById<Button>(R.id.btnRenew)
        btnRenewMembership.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container,MembershipPlanFragment() )
                .addToBackStack(null)
                .commit()
        }
    }
  }