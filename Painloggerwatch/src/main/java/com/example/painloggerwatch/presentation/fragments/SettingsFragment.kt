package com.example.painloggerwatch.presentation.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.painlogger.R
import com.example.painlogger.WorkScheduler
import com.example.painlogger.ReminderRepository
import com.example.painlogger.data.NotificationRule
import com.example.painlogger.databinding.FragmentSettingsBinding
import com.example.painlogger.util.NotificationRuleMapper
import com.example.painlogger.viewmodel.NotificationRulesViewModel
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val notificationRulesViewModel: NotificationRulesViewModel by activityViewModels()

    @Inject
    lateinit var workScheduler: WorkScheduler

    @Inject
    lateinit var reminderRepository: ReminderRepository

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: SharedPreferences

    companion object {
        private const val TAG = "SettingsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
        setupListeners()
        observeRules()
    }

    private fun initializeViews() {
        with(binding) {
            // Initialize notification sections visibility
            detailedNotificationSection.visibility =
                if (detailedLoggingSwitch.isChecked) View.VISIBLE else View.GONE
            generalNotificationSection.visibility =
                if (generalLoggingSwitch.isChecked) View.VISIBLE else View.GONE

            // Initialize dark mode
            prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
            val isDarkMode = prefs.getBoolean("dark_mode", true)
            darkModeSwitch.isChecked = isDarkMode
            setNightMode(isDarkMode)
        }
    }

    private fun setupListeners() {
        with(binding) {
            // Logging switches
            detailedLoggingSwitch.setOnCheckedChangeListener { _, isChecked ->
                detailedNotificationSection.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            generalLoggingSwitch.setOnCheckedChangeListener { _, isChecked ->
                generalNotificationSection.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            // Row click listeners
            detailedLoggingRow.setOnClickListener {
                detailedLoggingSwitch.isChecked = !detailedLoggingSwitch.isChecked
            }

            generalLoggingRow.setOnClickListener {
                generalLoggingSwitch.isChecked = !generalLoggingSwitch.isChecked
            }

            // Add notification buttons
            addDetailedNotificationRow.setOnClickListener {
                NotificationRuleDialogFragment.newInstance("detailed")
                    .show(parentFragmentManager, "add_detailed_notification")
            }

            addGeneralNotificationRow.setOnClickListener {
                NotificationRuleDialogFragment.newInstance("general")
                    .show(parentFragmentManager, "add_general_notification")
            }
            
            // Edit body parts list button
            editBodyPartsRow.setOnClickListener {
                navigateToBodyPartsEditor()
            }

            // Dark mode switch
            darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
                setNightMode(isChecked)
                prefs.edit().putBoolean("dark_mode", isChecked).apply()
                Toast.makeText(
                    requireContext(),
                    if (isChecked) "Dark mode enabled" else "Light mode enabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        setupNotificationResultListener()
    }
    
    private fun navigateToBodyPartsEditor() {
        findNavController().navigate(R.id.action_settingsFragment_to_bodyPartsEditorFragment)
    }

    private fun setupNotificationResultListener() {
        parentFragmentManager.setFragmentResultListener(
            "add_notification_rule",
            viewLifecycleOwner
        ) { _, bundle ->
            val type = bundle.getString("type") ?: return@setFragmentResultListener
            val mode = bundle.getString("mode") ?: return@setFragmentResultListener
            val timeHour = bundle.getInt("hour", -1).takeIf { it >= 0 }
            val timeMinute = bundle.getInt("minute", -1).takeIf { it >= 0 }
            val intervalHours = bundle.getInt("intervalHours", -1).takeIf { it >= 0 }
            val intervalMinutes = bundle.getInt("intervalMinutes", -1).takeIf { it >= 0 }

            val includedDays = bundle.getIntegerArrayList("includedDays")?.toSet() ?: emptySet()
            val calendarDays = includedDays.map {
                if (it == 0) Calendar.SUNDAY else it + Calendar.SUNDAY
            }.toSet()

            val allCalendarDays = (Calendar.SUNDAY..Calendar.SATURDAY).toSet()
            val excludedDays = allCalendarDays - calendarDays

            val rule = NotificationRule(
                type = type,
                mode = mode,
                timeHour = timeHour,
                timeMinute = timeMinute,
                intervalHours = intervalHours,
                intervalMinutes = intervalMinutes,
                excludedDays = excludedDays
            )

            handleNewRule(rule)
        }
    }

    private fun handleNewRule(rule: NotificationRule) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val reminder = NotificationRuleMapper.toReminder(rule)
                val reminderId = reminderRepository.upsertReminder(reminder)
                if (reminderId > 0) {
                    val savedReminder = reminder.copy(id = rule.reminderId ?: UUID.randomUUID())
                    savedReminder.config?.let { config ->
                        workScheduler.scheduleReminder(config)
                    }

                    notificationRulesViewModel.addRule(rule.copy(reminderId = savedReminder.id))
                    Toast.makeText(requireContext(), "Notification rule added!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding notification rule", e)
                Toast.makeText(requireContext(), "Error adding notification rule.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeRules() {
        notificationRulesViewModel.rules.observe(viewLifecycleOwner) { rules ->
            updateRuleLists(rules)
        }
    }

    private fun updateRuleLists(rules: List<NotificationRule>) {
        binding.detailedNotificationRulesList.removeAllViews()
        binding.generalNotificationRulesList.removeAllViews()

        rules.forEach { rule ->
            val ruleView = createRuleView(rule)
            if (rule.type == "detailed") {
                binding.detailedNotificationRulesList.addView(ruleView)
            } else if (rule.type == "general") {
                binding.generalNotificationRulesList.addView(ruleView)
            }
        }
    }

    private fun createRuleView(rule: NotificationRule): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            background = ContextCompat.getDrawable(context, R.drawable.notification_rule_bg)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
            gravity = android.view.Gravity.CENTER_VERTICAL

            // Add rule description
            addView(TextView(context).apply {
                text = formatRuleText(rule)
                textSize = 16f
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            // Add delete button
            addView(ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setBackgroundResource(android.R.color.transparent)
                contentDescription = "Delete"
                setOnClickListener { deleteRule(rule) }
            })
        }
    }

    private fun formatRuleText(rule: NotificationRule): String {
        val days = ((1..7).toSet() - rule.excludedDays)
            .map { dayIdx ->
                arrayOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")[
                    if (dayIdx == Calendar.SUNDAY) 0 else dayIdx - Calendar.SUNDAY
                ]
            }
            .joinToString(",")

        return when (rule.mode) {
            "specific" -> "At %02d:%02d, Include: %s".format(
                rule.timeHour ?: 0,
                rule.timeMinute ?: 0,
                days
            )
            "interval" -> "Every %dh %dm, Include: %s".format(
                rule.intervalHours ?: 0,
                rule.intervalMinutes ?: 0,
                days
            )
            else -> "Unknown rule"
        }
    }

    private fun deleteRule(rule: NotificationRule) {
        rule.reminderId?.let { reminderUUID ->
            workScheduler.cancelReminder(reminderUUID.toString())
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    reminderRepository.deleteReminder(reminderUUID)
                    notificationRulesViewModel.removeRule(rule.id)
                    Toast.makeText(requireContext(), "Notification rule deleted", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting rule", e)
                    Toast.makeText(requireContext(), "Error deleting rule", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setNightMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}