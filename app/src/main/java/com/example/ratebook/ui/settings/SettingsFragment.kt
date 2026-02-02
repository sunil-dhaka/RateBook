package com.example.ratebook.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ratebook.R
import com.example.ratebook.RateBookApplication
import com.example.ratebook.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val app by lazy { requireActivity().application as RateBookApplication }

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(app.backupManager)
    }

    private var pendingBackupFile: File? = null

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    private val saveFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { destUri ->
            pendingBackupFile?.let { sourceFile ->
                saveBackupToUri(sourceFile, destUri)
            }
        }
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

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.exportLayout.setOnClickListener {
            viewModel.exportBackup()
        }

        binding.importLayout.setOnClickListener {
            showImportConfirmation()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading
                        binding.exportLayout.isClickable = !isLoading
                        binding.importLayout.isClickable = !isLoading
                    }
                }

                launch {
                    viewModel.statusMessage.collect { message ->
                        binding.statusTextView.isVisible = message != null
                        binding.statusTextView.text = message
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is SettingsViewModel.SettingsEvent.ExportSuccess -> {
                                showExportOptionsDialog(event.file)
                            }
                            is SettingsViewModel.SettingsEvent.ImportSuccess -> {
                                val counts = event.counts
                                val message = "Imported: ${counts.products} products, " +
                                        "${counts.categories} categories, " +
                                        "${counts.measurementUnits} units, " +
                                        "${counts.photos} photos"
                                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                            }
                            is SettingsViewModel.SettingsEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showImportConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_backup)
            .setMessage(R.string.import_warning)
            .setPositiveButton(R.string.import_backup) { _, _ ->
                pickFileLauncher.launch("application/zip")
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showExportOptionsDialog(file: File) {
        val options = arrayOf(
            getString(R.string.save_to_device),
            getString(R.string.share_backup)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.export_success)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> saveBackupToDevice(file)
                    1 -> shareBackupFile(file)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveBackupToDevice(file: File) {
        pendingBackupFile = file
        saveFileLauncher.launch(file.name)
    }

    private fun saveBackupToUri(sourceFile: File, destUri: android.net.Uri) {
        try {
            requireContext().contentResolver.openOutputStream(destUri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Toast.makeText(requireContext(), R.string.backup_saved, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.error_saving_backup, Toast.LENGTH_LONG).show()
        }
    }

    private fun shareBackupFile(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_backup)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
