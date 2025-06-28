package com.example.painloggerwatch.presentation.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.painlogger.R
import com.example.painlogger.databinding.FragmentBodyPartsEditorBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class BodyPartsEditorFragment : Fragment() {

    private var _binding: FragmentBodyPartsEditorBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: BodyPartsAdapter
    private lateinit var prefs: SharedPreferences
    private val bodyPartsList = mutableListOf<String>()
    
    companion object {
        private const val PREFS_NAME = "BodyPartsPrefs"
        private const val KEY_BODY_PARTS = "body_parts_list"
        
        // Default body parts list
        val DEFAULT_BODY_PARTS = arrayOf(
            "HD: Head - General", "FRN: Forehead", "TMP-L: Temple (Left)", "TMP-R: Temple (Right)",
            "EYE-L: Eye (Left)", "EYE-R: Eye (Right)", "EAR-L: Ear (Left)", "EAR-R: Ear (Right)",
            "JAW-L: Jaw (Left)", "JAW-R: Jaw (Right)", "NECK: Neck", "SHD-L: Shoulder (Left)",
            "SHD-R: Shoulder (Right)", "ARM-L: Arm (Left)", "ARM-R: Arm (Right)",
            "ELB-L: Elbow (Left)", "ELB-R: Elbow (Right)", "WRS-L: Wrist (Left)",
            "WRS-R: Wrist (Right)", "HND-L: Hand (Left)", "HND-R: Hand (Right)",
            "CHT: Chest", "ABD: Abdomen", "BCK-U: Back (Upper)", "BCK-M: Back (Middle)",
            "BCK-L: Back (Lower)", "HIP-L: Hip (Left)", "HIP-R: Hip (Right)",
            "LEG-L: Leg (Left)", "LEG-R: Leg (Right)", "KNE-L: Knee (Left)",
            "KNE-R: Knee (Right)", "ANK-L: Ankle (Left)", "ANK-R: Ankle (Right)",
            "FT-L: Foot (Left)", "FT-R: Foot (Right)", "OTH: Other", "GEN: General"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBodyPartsEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load saved body parts or use defaults
        loadBodyParts()
        
        // Set up RecyclerView
        adapter = BodyPartsAdapter(
            bodyPartsList,
            onEditClick = { position -> showEditDialog(position) },
            onDeleteClick = { position -> deleteBodyPart(position) }
        )
        
        binding.bodyPartsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.bodyPartsRecyclerView.adapter = adapter
        
        // Set up drag and drop reordering
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                
                // Swap items in the list
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition) {
                        val temp = bodyPartsList[i]
                        bodyPartsList[i] = bodyPartsList[i + 1]
                        bodyPartsList[i + 1] = temp
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        val temp = bodyPartsList[i]
                        bodyPartsList[i] = bodyPartsList[i - 1]
                        bodyPartsList[i - 1] = temp
                    }
                }
                
                adapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used for drag and drop
            }
            
            override fun isLongPressDragEnabled(): Boolean {
                return true
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.bodyPartsRecyclerView)
        
        // Set up button listeners
        binding.addBodyPartButton.setOnClickListener { showAddDialog() }
        binding.resetToDefaultButton.setOnClickListener { resetToDefault() }
        binding.saveButton.setOnClickListener { saveChanges() }
    }
    
    private fun loadBodyParts() {
        val json = prefs.getString(KEY_BODY_PARTS, null)
        if (json != null) {
            val type: Type = object : TypeToken<List<String>>() {}.type
            val loadedList = Gson().fromJson<List<String>>(json, type)
            bodyPartsList.clear()
            bodyPartsList.addAll(loadedList)
        } else {
            // Use default list if nothing is saved
            bodyPartsList.clear()
            bodyPartsList.addAll(DEFAULT_BODY_PARTS)
        }
    }
    
    private fun saveBodyParts() {
        val json = Gson().toJson(bodyPartsList)
        prefs.edit().putString(KEY_BODY_PARTS, json).apply()
    }
    
    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_body_part, null)
        val codeEditText = dialogView.findViewById<EditText>(R.id.codeEditText)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.descriptionEditText)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Add Body Part")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val code = codeEditText.text.toString().trim()
                val description = descriptionEditText.text.toString().trim()
                
                if (code.isNotEmpty() && description.isNotEmpty()) {
                    val bodyPart = "$code: $description"
                    bodyPartsList.add(bodyPart)
                    adapter.notifyItemInserted(bodyPartsList.size - 1)
                } else {
                    Toast.makeText(requireContext(), "Both fields are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEditDialog(position: Int) {
        val bodyPart = bodyPartsList[position]
        val parts = bodyPart.split(":", limit = 2)
        
        val code = parts[0].trim()
        val description = if (parts.size > 1) parts[1].trim() else ""
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_body_part, null)
        val codeEditText = dialogView.findViewById<EditText>(R.id.codeEditText)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.descriptionEditText)
        
        codeEditText.setText(code)
        descriptionEditText.setText(description)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Body Part")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newCode = codeEditText.text.toString().trim()
                val newDescription = descriptionEditText.text.toString().trim()
                
                if (newCode.isNotEmpty() && newDescription.isNotEmpty()) {
                    val newBodyPart = "$newCode: $newDescription"
                    bodyPartsList[position] = newBodyPart
                    adapter.notifyItemChanged(position)
                } else {
                    Toast.makeText(requireContext(), "Both fields are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteBodyPart(position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Body Part")
            .setMessage("Are you sure you want to delete this body part?")
            .setPositiveButton("Delete") { _, _ ->
                bodyPartsList.removeAt(position)
                adapter.notifyItemRemoved(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun resetToDefault() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset to Default")
            .setMessage("This will replace your current list with the default body parts list. Continue?")
            .setPositiveButton("Reset") { _, _ ->
                bodyPartsList.clear()
                bodyPartsList.addAll(DEFAULT_BODY_PARTS)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Reset to default list", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun saveChanges() {
        AlertDialog.Builder(requireContext())
            .setTitle("Save Changes")
            .setMessage("Save changes to the body parts list? This will affect how body parts are displayed in detailed logging.")
            .setPositiveButton("Save") { _, _ ->
                saveBodyParts()
                Toast.makeText(requireContext(), "Changes saved", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // RecyclerView Adapter
    inner class BodyPartsAdapter(
        private val items: List<String>,
        private val onEditClick: (Int) -> Unit,
        private val onDeleteClick: (Int) -> Unit
    ) : RecyclerView.Adapter<BodyPartsAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val bodyPartTextView = view.findViewById<android.widget.TextView>(R.id.bodyPartTextView)
            val editButton = view.findViewById<android.widget.ImageButton>(R.id.editButton)
            val deleteButton = view.findViewById<android.widget.ImageButton>(R.id.deleteButton)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_editable_body_part, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bodyPartTextView.text = items[position]
            holder.editButton.setOnClickListener { onEditClick(position) }
            holder.deleteButton.setOnClickListener { onDeleteClick(position) }
        }
        
        override fun getItemCount() = items.size
    }
}