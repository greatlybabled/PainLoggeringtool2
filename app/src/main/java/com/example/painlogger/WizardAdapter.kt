package com.example.painlogger // Make sure this is your correct package

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.painlogger.fragments.BodyPartsFragment
import com.example.painlogger.fragments.GeneralFragment // Import the specific fragment class
import com.example.painlogger.fragments.TriggersFragment

// Adapter for the ViewPager2 to manage the fragments in the pain logging wizard
class WizardAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    // Define the total number of steps (fragments) in the wizard
    override fun getItemCount(): Int = 3 // General, Triggers, Body Parts

    // Create and return the fragment for the given position
    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> GeneralFragment() // First step: General Pain
            1 -> TriggersFragment() // Second step: Triggers
            2 -> BodyPartsFragment() // Third step: Body Parts and Detailed Pain
            else -> throw IllegalArgumentException("Invalid position") // Handle unexpected positions
        }
    }
}
