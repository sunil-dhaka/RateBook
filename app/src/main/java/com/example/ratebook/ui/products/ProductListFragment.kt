package com.example.ratebook.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.ratebook.R
import com.example.ratebook.RateBookApplication
import com.example.ratebook.data.database.entity.Category
import com.example.ratebook.databinding.FragmentProductListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ProductListFragment : Fragment() {

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!

    private val app by lazy { requireActivity().application as RateBookApplication }

    private val viewModel: ProductListViewModel by viewModels {
        ProductListViewModel.Factory(app.productRepository, app.categoryRepository)
    }

    private lateinit var adapter: ProductAdapter
    private var categories: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupFilterAndSort()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(app.photoManager) { product ->
            val bundle = bundleOf("productId" to product.id)
            findNavController().navigate(R.id.nav_product_edit, bundle)
        }
        binding.productsRecyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun setupFilterAndSort() {
        binding.filterChip.setOnClickListener {
            showCategoryFilterDialog()
        }

        binding.sortChip.setOnClickListener {
            showSortDialog()
        }
    }

    private fun showCategoryFilterDialog() {
        val categoryNames = mutableListOf(getString(R.string.all_categories))
        categoryNames.addAll(categories.map { it.name })

        val currentIndex = viewModel.selectedCategoryId.value?.let { selectedId ->
            categories.indexOfFirst { it.id == selectedId } + 1
        } ?: 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filter_by_category)
            .setSingleChoiceItems(categoryNames.toTypedArray(), currentIndex) { dialog, which ->
                val categoryId = if (which == 0) null else categories[which - 1].id
                viewModel.setCategory(categoryId)
                updateFilterChipText(categoryId)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            getString(R.string.sort_newest),
            getString(R.string.sort_oldest),
            getString(R.string.sort_name_asc),
            getString(R.string.sort_name_desc),
            getString(R.string.sort_cost_asc),
            getString(R.string.sort_cost_desc),
            getString(R.string.sort_price_asc),
            getString(R.string.sort_price_desc),
            getString(R.string.sort_sku_asc),
            getString(R.string.sort_sku_desc)
        )

        val sortOptionValues = listOf(
            SortOption.NEWEST,
            SortOption.OLDEST,
            SortOption.NAME_ASC,
            SortOption.NAME_DESC,
            SortOption.COST_PRICE_ASC,
            SortOption.COST_PRICE_DESC,
            SortOption.SELLING_PRICE_ASC,
            SortOption.SELLING_PRICE_DESC,
            SortOption.SKU_ASC,
            SortOption.SKU_DESC
        )

        val currentIndex = sortOptionValues.indexOf(viewModel.sortOption.value)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(sortOptions, currentIndex) { dialog, which ->
                viewModel.setSortOption(sortOptionValues[which])
                updateSortChipText(sortOptionValues[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateFilterChipText(categoryId: Long?) {
        val text = if (categoryId == null) {
            getString(R.string.filter)
        } else {
            categories.find { it.id == categoryId }?.name ?: getString(R.string.filter)
        }
        binding.filterChip.text = text
    }

    private fun updateSortChipText(sortOption: SortOption) {
        val text = when (sortOption) {
            SortOption.NAME_ASC -> getString(R.string.sort_name_asc)
            SortOption.NAME_DESC -> getString(R.string.sort_name_desc)
            SortOption.COST_PRICE_ASC -> getString(R.string.sort_cost_asc)
            SortOption.COST_PRICE_DESC -> getString(R.string.sort_cost_desc)
            SortOption.SELLING_PRICE_ASC -> getString(R.string.sort_price_asc)
            SortOption.SELLING_PRICE_DESC -> getString(R.string.sort_price_desc)
            SortOption.SKU_ASC -> getString(R.string.sort_sku_asc)
            SortOption.SKU_DESC -> getString(R.string.sort_sku_desc)
            SortOption.NEWEST -> getString(R.string.sort_newest)
            SortOption.OLDEST -> getString(R.string.sort_oldest)
        }
        binding.sortChip.text = text
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.products.collect { products ->
                        adapter.submitList(products)
                        binding.emptyTextView.isVisible = products.isEmpty()
                        binding.productsRecyclerView.isVisible = products.isNotEmpty()
                    }
                }

                launch {
                    viewModel.categories.collect { cats ->
                        categories = cats
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
