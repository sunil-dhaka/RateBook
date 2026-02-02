package com.example.ratebook.ui.units

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ratebook.R
import com.example.ratebook.RateBookApplication
import com.example.ratebook.data.database.entity.MeasurementUnit
import com.example.ratebook.databinding.DialogUnitEditBinding
import com.example.ratebook.databinding.FragmentUnitListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class UnitListFragment : Fragment() {

    private var _binding: FragmentUnitListBinding? = null
    private val binding get() = _binding!!

    private val app by lazy { requireActivity().application as RateBookApplication }

    private val viewModel: UnitListViewModel by viewModels {
        UnitListViewModel.Factory(app.measurementUnitRepository)
    }

    private lateinit var adapter: UnitAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUnitListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = UnitAdapter(
            onEditClick = { unit -> showEditDialog(unit) },
            onDeleteClick = { unit -> showDeleteConfirmation(unit) }
        )
        binding.unitsRecyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.units.collect { units ->
                        adapter.submitList(units)
                        binding.emptyTextView.isVisible = units.isEmpty()
                        binding.unitsRecyclerView.isVisible = units.isNotEmpty()
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is UnitListViewModel.UnitEvent.SaveSuccess -> {
                                Toast.makeText(requireContext(), R.string.unit_saved, Toast.LENGTH_SHORT).show()
                            }
                            is UnitListViewModel.UnitEvent.DeleteSuccess -> {
                                Toast.makeText(requireContext(), R.string.unit_deleted, Toast.LENGTH_SHORT).show()
                            }
                            is UnitListViewModel.UnitEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    fun showAddDialog() {
        val dialogBinding = DialogUnitEditBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_unit)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val code = dialogBinding.unitCodeEditText.text?.toString()?.trim()
                val name = dialogBinding.unitNameEditText.text?.toString()?.trim()
                val symbol = dialogBinding.unitSymbolEditText.text?.toString()?.trim()

                if (!code.isNullOrBlank() && !name.isNullOrBlank() && !symbol.isNullOrBlank()) {
                    viewModel.addUnit(code, name, symbol)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditDialog(unit: MeasurementUnit) {
        val dialogBinding = DialogUnitEditBinding.inflate(layoutInflater)
        dialogBinding.unitCodeEditText.setText(unit.code)
        dialogBinding.unitNameEditText.setText(unit.name)
        dialogBinding.unitSymbolEditText.setText(unit.symbol)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_unit)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val code = dialogBinding.unitCodeEditText.text?.toString()?.trim()
                val name = dialogBinding.unitNameEditText.text?.toString()?.trim()
                val symbol = dialogBinding.unitSymbolEditText.text?.toString()?.trim()

                if (!code.isNullOrBlank() && !name.isNullOrBlank() && !symbol.isNullOrBlank()) {
                    viewModel.updateUnit(unit, code, name, symbol)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirmation(unit: MeasurementUnit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_unit)
            .setMessage(R.string.delete_unit_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteUnit(unit)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
