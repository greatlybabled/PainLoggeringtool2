// File: app/src/main/java/com/example/painlogger/fragments/ReminderConfigDialogFragment.kt

package com.example.painlogger.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.text
import androidx.fragment.app.DialogFragment
import com.example.painlogger.Reminder
import com.example.painlogger.ReminderCategory // Ensure this is imported
import com.example.painlogger.data.model.ReminderConfig
import com.example.painlogger.ReminderType
import com.example.painlogger.databinding.DialogReminderConfigBinding // Assuming your binding class name
import java.util.UUID

import com.example.painlogger.fragments.ReminderConfigListener // Import the listener

class ReminderConfigDialogFragment : DialogFragment() {

    private var _binding: DialogReminderConfigBinding? = null
    private val binding get() = _binding!!

    private var existingReminder: Reminder? = null
    private var initialCategory: ReminderCategory? = null

    private var listener: ReminderConfigListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = android.app.AlertDialog.Builder(requireActivity())
        _binding = DialogReminderConfigBinding.inflate(LayoutInflater.from(context))
        builder.setView(binding.root)

        arguments?.let {
            existingReminder = it.getParcelable(ARG_REMINDER)
            initialCategory = it.getParcelable(ARG_CATEGORY)
        }

        // Set up the Category Spinner
        setupCategorySpinner()

        // Populate fields if editing an existing reminder
        existingReminder?.let { reminder ->
            binding.editTextTitle.setText(reminder.title) // Use editTextTitle
            setSelectedCategoryInSpinner(reminder.category) // Select the category in the spinner
            binding.toggleEnabled.isChecked = reminder.isEnabled // Use toggleEnabled
            // Populate other fields as needed
        } ?: run {
            // Handle case for adding a new reminder
            initialCategory?.let { category ->
                setSelectedCategoryInSpinner(category) // Set initial category in spinner
            }
            // Set default values for new reminder
            binding.toggleEnabled.isChecked = true // Use toggleEnabled, or your default
        }

        // Set up listeners for buttons
        binding.buttonSave.setOnClickListener { // Use buttonSave
            saveReminder()
        }

        binding.buttonCancel.setOnClickListener { // Use buttonCancel
            dismiss()
        }

        // Add delete button listener if applicable
        if (existingReminder != null) {
            binding.buttonDelete.visibility = View.VISIBLE // Use buttonDelete
            binding.buttonDelete.setOnClickListener { // Use buttonDelete
                deleteReminder()
            }
        } else {
            binding.buttonDelete.visibility = View.GONE // Use buttonDelete
        }

        return builder.create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        if (parentFragment is ReminderConfigListener) {
            listener = parentFragment as ReminderConfigListener
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupCategorySpinner() {
        // Create an ArrayAdapter using the ReminderCategory enum values
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ReminderCategory.values() // Get all enum values
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter // Use categorySpinner
    }

    private fun setSelectedCategoryInSpinner(category: ReminderCategory) {
        val adapter = binding.categorySpinner.adapter as ArrayAdapter<ReminderCategory>
        val position = adapter.getPosition(category)
        if (position >= 0) {
            binding.categorySpinner.setSelection(position)
        }
    }

    private fun saveReminder() {
        val title = binding.editTextTitle.text.toString().trim() // Use editTextTitle
        val selectedCategory = binding.categorySpinner.selectedItem as ReminderCategory // Use categorySpinner
        val isEnabled = binding.toggleEnabled.isChecked // Use toggleEnabled
        // Get other configuration details (type, active days, etc.) - You'll need to add views for these and retrieve their values

        if (title.isEmpty()) {
            binding.editTextTitle.error = "Title cannot be empty" // Use editTextTitle
            return
        }

        val reminderToSave = existingReminder?.copy(
            title = title,
            category = selectedCategory,
            isEnabled = isEnabled
            // Update other properties
        ) ?: Reminder(
            id = generateUniqueId(),
            title = title,
            category = selectedCategory,
            // Provide default values for other properties for a new reminder
            // IMPORTANT: You need to add views to your layout and retrieve these values
            type = ReminderType.DETAILED, // Assuming you have a way to select this or a default
            isEnabled = isEnabled,
            activeDays = setOf(), // Assuming you have a way to select days or a default
            config = null // Assuming you have a way to configure this or a default
        )

        listener?.onSaveReminder(reminderToSave)
        dismiss()
    }

    private fun deleteReminder() {
        existingReminder?.let {
            listener?.onDeleteReminder(it.id.toString())
        }
        dismiss()
    }

    private fun generateUniqueId(): UUID {
        return UUID.randomUUID()
    }

    companion object {
        private const val ARG_REMINDER = "reminder"
        private const val ARG_CATEGORY = "category"

        fun newInstance(reminder: Reminder? = null, initialCategory: ReminderCategory? = null): ReminderConfigDialogFragment {
            val fragment = ReminderConfigDialogFragment()
            val args = Bundle().apply {
                putParcelable(ARG_REMINDER, reminder)
                putParcelable(ARG_CATEGORY, initialCategory)
            }
            fragment.arguments = args
            return fragment
        }
    }
}