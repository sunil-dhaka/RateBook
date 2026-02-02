package com.example.ratebook.ui.categories

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
import com.example.ratebook.data.database.entity.Category
import com.example.ratebook.databinding.DialogCategoryEditBinding
import com.example.ratebook.databinding.FragmentCategoryListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class CategoryListFragment : Fragment() {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    private val app by lazy { requireActivity().application as RateBookApplication }

    private val viewModel: CategoryListViewModel by viewModels {
        CategoryListViewModel.Factory(app.categoryRepository)
    }

    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(
            onEditClick = { category -> showEditDialog(category) },
            onDeleteClick = { category -> showDeleteConfirmation(category) }
        )
        binding.categoriesRecyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.categories.collect { categories ->
                        adapter.submitList(categories)
                        binding.emptyTextView.isVisible = categories.isEmpty()
                        binding.categoriesRecyclerView.isVisible = categories.isNotEmpty()
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is CategoryListViewModel.CategoryEvent.SaveSuccess -> {
                                Toast.makeText(requireContext(), R.string.category_saved, Toast.LENGTH_SHORT).show()
                            }
                            is CategoryListViewModel.CategoryEvent.DeleteSuccess -> {
                                Toast.makeText(requireContext(), R.string.category_deleted, Toast.LENGTH_SHORT).show()
                            }
                            is CategoryListViewModel.CategoryEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    fun showAddDialog() {
        val dialogBinding = DialogCategoryEditBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_category)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = dialogBinding.categoryNameEditText.text?.toString()?.trim()
                if (!name.isNullOrBlank()) {
                    viewModel.addCategory(name)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditDialog(category: Category) {
        val dialogBinding = DialogCategoryEditBinding.inflate(layoutInflater)
        dialogBinding.categoryNameEditText.setText(category.name)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_category)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = dialogBinding.categoryNameEditText.text?.toString()?.trim()
                if (!name.isNullOrBlank()) {
                    viewModel.updateCategory(category, name)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirmation(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_category)
            .setMessage(R.string.delete_category_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteCategory(category)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
