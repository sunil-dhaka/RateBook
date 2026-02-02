package com.example.ratebook.ui.products

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.ratebook.R
import com.example.ratebook.RateBookApplication
import com.example.ratebook.data.database.entity.Category
import com.example.ratebook.data.database.entity.MeasurementUnit
import com.example.ratebook.data.database.entity.Product
import com.example.ratebook.databinding.FragmentProductEditBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

class ProductEditFragment : Fragment() {

    private var _binding: FragmentProductEditBinding? = null
    private val binding get() = _binding!!

    private val productId: Long by lazy {
        arguments?.getLong("productId", 0L) ?: 0L
    }

    private val app by lazy { requireActivity().application as RateBookApplication }

    private val viewModel: ProductEditViewModel by viewModels {
        ProductEditViewModel.Factory(
            app.productRepository,
            app.categoryRepository,
            app.measurementUnitRepository,
            app.photoManager
        )
    }

    private var categories: List<Category> = emptyList()
    private var units: List<MeasurementUnit> = emptyList()
    private var selectedCategoryId: Long? = null
    private var selectedUnitId: Long? = null
    private var tempPhotoUri: Uri? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.setPhotoFromUri(tempPhotoUri!!)
            binding.productImageView.load(tempPhotoUri)
            binding.addPhotoIcon.isVisible = false
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setPhotoFromUri(it)
            binding.productImageView.load(it)
            binding.addPhotoIcon.isVisible = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeViewModel()

        if (productId > 0) {
            viewModel.loadProduct(productId)
        }
    }

    private fun setupViews() {
        binding.photoCard.setOnClickListener {
            showPhotoSourceDialog()
        }

        binding.categoryAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = if (position == 0) null else categories.getOrNull(position - 1)?.id
        }

        binding.unitAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedUnitId = units.getOrNull(position)?.id
        }

        binding.saveButton.setOnClickListener {
            saveProduct()
        }

        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.deleteButton.isVisible = productId > 0
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.categories.collect { cats ->
                        categories = cats
                        setupCategoryDropdown()
                    }
                }

                launch {
                    viewModel.units.collect { unitList ->
                        units = unitList
                        setupUnitDropdown()
                    }
                }

                launch {
                    viewModel.product.collect { product ->
                        product?.let { populateForm(it) }
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is ProductEditViewModel.ProductEditEvent.SaveSuccess -> {
                                Toast.makeText(requireContext(), R.string.product_saved, Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is ProductEditViewModel.ProductEditEvent.DeleteSuccess -> {
                                Toast.makeText(requireContext(), R.string.product_deleted, Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is ProductEditViewModel.ProductEditEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupCategoryDropdown() {
        val items = listOf(getString(R.string.no_category)) + categories.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        binding.categoryAutoComplete.setAdapter(adapter)
    }

    private fun setupUnitDropdown() {
        val items = units.map { "${it.name} (${it.symbol})" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        binding.unitAutoComplete.setAdapter(adapter)
    }

    private fun populateForm(product: Product) {
        binding.nameEditText.setText(product.name)
        binding.costPriceEditText.setText(product.costPrice.toString())
        binding.sellingPriceEditText.setText(product.sellingPrice.toString())
        binding.notesEditText.setText(product.notes ?: "")

        selectedCategoryId = product.categoryId
        selectedUnitId = product.measurementUnitId

        product.photoPath?.let { path ->
            val file = viewModel.getPhotoFile(path)
            if (file.exists()) {
                binding.productImageView.load(file)
                binding.addPhotoIcon.isVisible = false
            }
        }

        val category = categories.find { it.id == product.categoryId }
        category?.let {
            binding.categoryAutoComplete.setText(it.name, false)
        } ?: binding.categoryAutoComplete.setText(getString(R.string.no_category), false)

        val unit = units.find { it.id == product.measurementUnitId }
        unit?.let {
            binding.unitAutoComplete.setText("${it.name} (${it.symbol})", false)
        }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf(getString(R.string.camera), getString(R.string.gallery))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_photo_source)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val photoFile = File(requireContext().cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
        tempPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(tempPhotoUri)
    }

    private fun saveProduct() {
        val name = binding.nameEditText.text?.toString()?.trim()
        val costPriceStr = binding.costPriceEditText.text?.toString()
        val sellingPriceStr = binding.sellingPriceEditText.text?.toString()
        val notes = binding.notesEditText.text?.toString()?.trim()

        if (name.isNullOrBlank()) {
            binding.nameLayout.error = getString(R.string.field_required)
            return
        }
        binding.nameLayout.error = null

        val costPrice = costPriceStr?.toDoubleOrNull()
        if (costPrice == null) {
            binding.costPriceLayout.error = getString(R.string.invalid_price)
            return
        }
        binding.costPriceLayout.error = null

        val sellingPrice = sellingPriceStr?.toDoubleOrNull()
        if (sellingPrice == null) {
            binding.sellingPriceLayout.error = getString(R.string.invalid_price)
            return
        }
        binding.sellingPriceLayout.error = null

        viewModel.saveProduct(
            name = name,
            costPrice = costPrice,
            sellingPrice = sellingPrice,
            categoryId = selectedCategoryId,
            unitId = selectedUnitId,
            notes = notes
        )
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setMessage(R.string.delete_product_confirm)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteProduct() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
