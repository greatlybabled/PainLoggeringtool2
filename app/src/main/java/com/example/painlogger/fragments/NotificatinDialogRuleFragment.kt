
package com.example.painlogger.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.painlogger.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.*

class NotificationRuleDialogFragment : DialogFragment() {

    private val includedDays = mutableSetOf<Int>() // 0=Sun, 1=Mon, ..., 6=Sat

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = android.app.AlertDialog.Builder(requireContext())
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_notification_rule, null)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        val dayInclusionContainer = view.findViewById<LinearLayout>(R.id.dayExclusionContainer) // Rename in XML to dayInclusionContainer for clarity
        val buttonCancel = view.findViewById<Button>(R.id.buttonCancel)
        val buttonSave = view.findViewById<Button>(R.id.buttonSave)

        // Set up ViewPager2 with two tabs
        val tabFragments = listOf(
            SpecificTimeTabFragment(),
            IntervalTabFragment()
        )
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = tabFragments.size
            override fun createFragment(position: Int): Fragment = tabFragments[position]
        }
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Specific Time" else "Interval"
        }.attach()

        // Day inclusion toggle buttons
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        for (i in 0..6) {
            val btn = ToggleButton(requireContext())
            btn.textOn = dayLabels[i]
            btn.textOff = dayLabels[i]
            btn.text = dayLabels[i]
            btn.isChecked = false
            btn.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) includedDays.add(i) else includedDays.remove(i)
            }
            val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            params.marginEnd = 4
            btn.layoutParams = params
            dayInclusionContainer.addView(btn)
        }

        buttonCancel.setOnClickListener { dismiss() }
        buttonSave.setOnClickListener {
            // --- Add this log to debug includedDays ---
            Log.d("NotificationRuleDialog", "includedDays selected: $includedDays")
            val type = arguments?.getString("type") ?: "detailed"
            val mode = if (viewPager.currentItem == 0) "specific" else "interval"
            val result = Bundle().apply {
                putString("type", type)
                putString("mode", mode)
                putIntegerArrayList("includedDays", ArrayList(includedDays))
                if (mode == "specific") {
                    val frag = childFragmentManager.findFragmentByTag("f0") as? SpecificTimeTabFragment
                    putInt("hour", frag?.selectedHour ?: 9)
                    putInt("minute", frag?.selectedMinute ?: 0)
                } else {
                    val frag = childFragmentManager.findFragmentByTag("f1") as? IntervalTabFragment
                    putInt("intervalHours", frag?.hours ?: 0)
                    putInt("intervalMinutes", frag?.minutes ?: 0)
                }
            }
            parentFragmentManager.setFragmentResult("add_notification_rule", result)
            dismiss()
        }

        builder.setView(view)
        return builder.create()
    }

    companion object {
        fun newInstance(type: String): NotificationRuleDialogFragment {
            val fragment = NotificationRuleDialogFragment()
            fragment.arguments = Bundle().apply { putString("type", type) }
            return fragment
        }
    }
}

// Tab 1: Specific Time Picker
class SpecificTimeTabFragment : Fragment() {
    var selectedHour = 9
    var selectedMinute = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.tab_specific_time, container, false)
        val buttonPickTime = view.findViewById<Button>(R.id.buttonPickTime)
        buttonPickTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
        buttonPickTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .setTitleText("Select Time")
                .setTheme(R.style.CustomTimePickerTheme)
                .build()
            picker.addOnPositiveButtonClickListener {
                selectedHour = picker.hour
                selectedMinute = picker.minute
                buttonPickTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
            }
            picker.show(parentFragmentManager, "time_picker")
        }
        return view
    }
}

// Tab 2: Interval Picker
class IntervalTabFragment : Fragment() {
    var hours = 0
    var minutes = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.tab_interval, container, false)
        val numberPickerHours = view.findViewById<NumberPicker>(R.id.numberPickerHours)
        val numberPickerMinutes = view.findViewById<NumberPicker>(R.id.numberPickerMinutes)

        numberPickerHours.minValue = 0
        numberPickerHours.maxValue = 23
        numberPickerHours.value = hours

        numberPickerMinutes.minValue = 0
        numberPickerMinutes.maxValue = 59
        numberPickerMinutes.value = minutes

        numberPickerHours.setOnValueChangedListener { _, _, newVal -> hours = newVal }
        numberPickerMinutes.setOnValueChangedListener { _, _, newVal -> minutes = newVal }

        return view
    }
}
