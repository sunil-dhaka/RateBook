package com.example.ratebook.ui.units

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ratebook.data.database.entity.MeasurementUnit
import com.example.ratebook.databinding.ItemUnitBinding

class UnitAdapter(
    private val onEditClick: (MeasurementUnit) -> Unit,
    private val onDeleteClick: (MeasurementUnit) -> Unit
) : ListAdapter<MeasurementUnit, UnitAdapter.UnitViewHolder>(UnitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
        val binding = ItemUnitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UnitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UnitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UnitViewHolder(
        private val binding: ItemUnitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(unit: MeasurementUnit) {
            binding.unitSymbol.text = unit.symbol
            binding.unitName.text = unit.name
            binding.unitCode.text = "Code: ${unit.code}"
            binding.editButton.setOnClickListener { onEditClick(unit) }
            binding.deleteButton.setOnClickListener { onDeleteClick(unit) }
        }
    }

    private class UnitDiffCallback : DiffUtil.ItemCallback<MeasurementUnit>() {
        override fun areItemsTheSame(oldItem: MeasurementUnit, newItem: MeasurementUnit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MeasurementUnit, newItem: MeasurementUnit): Boolean {
            return oldItem == newItem
        }
    }
}
