package com.example.painlogger.adapters
import com.example.painlogger.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.semantics.text
import androidx.recyclerview.widget.RecyclerView
import com.example.painlogger.Reminder
import com.example.painlogger.data.model.ReminderConfig // Import ReminderConfig


class ReminderAdapter(
    private val reminders: List<Reminder>,
    private val onEditClick: (Reminder) -> Unit, // Lambda for edit click
    private val onDeleteClick: (Reminder) -> Unit // Lambda for delete click
    // Add a lambda for toggle enable/disable if needed
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.reminder_title_text_view) // You'll need item layout views
        val configTextView: TextView = itemView.findViewById(R.id.reminder_config_text_view)
        // Add other views like delete button, toggle switch, etc.
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        // Inflate your individual reminder item layout here
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false) // You'll need to create item_reminder.xml
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]
        holder.titleTextView.text = reminder.title

        // Correctly handle nullable config for the text view
        holder.configTextView.text = reminder.config?.displayString() ?: "No config set" // Provide a default string

        // Set click listeners separately
        holder.itemView.setOnClickListener { onEditClick(reminder) } // Handle item click for editing
        // Add setOnClickListener for delete button if you have one
        // holder.deleteButton.setOnClickListener { onDeleteClick(reminder) }
    }

    override fun getItemCount(): Int {
        return reminders.size
    }
}