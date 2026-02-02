package com.example.ratebook.data.backup

import android.content.Context
import android.net.Uri
import android.os.Build
import com.example.ratebook.data.database.entity.Category
import com.example.ratebook.data.database.entity.MeasurementUnit
import com.example.ratebook.data.database.entity.Product
import com.example.ratebook.data.repository.CategoryRepository
import com.example.ratebook.data.repository.MeasurementUnitRepository
import com.example.ratebook.data.repository.ProductRepository
import com.example.ratebook.domain.util.PhotoManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val measurementUnitRepository: MeasurementUnitRepository,
    private val photoManager: PhotoManager
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun createBackup(): Result<File> {
        return try {
            val timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            )
            val backupFileName = "ratebook_backup_$timestamp.zip"
            val backupFile = File(context.cacheDir, backupFileName)

            val products = productRepository.getAllProductsSync()
            val categories = categoryRepository.getAllCategoriesSync()
            val units = measurementUnitRepository.getAllUnitsSync()

            val photoPaths = products.mapNotNull { it.photoPath }

            val manifest = createManifest(
                productCount = products.size,
                categoryCount = categories.size,
                unitCount = units.size,
                photoCount = photoPaths.size
            )

            ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile))).use { zip ->
                addJsonEntry(zip, MANIFEST_FILE, json.encodeToString(manifest))
                addJsonEntry(zip, PRODUCTS_FILE, json.encodeToString(products))
                addJsonEntry(zip, CATEGORIES_FILE, json.encodeToString(categories))
                addJsonEntry(zip, UNITS_FILE, json.encodeToString(units))

                for (photoPath in photoPaths) {
                    val photoFile = photoManager.getPhotoFile(photoPath)
                    if (photoFile.exists()) {
                        addFileEntry(zip, "$PHOTOS_DIR/$photoPath", photoFile)
                    }
                }
            }

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(uri: Uri): Result<BackupCounts> {
        return try {
            val tempDir = File(context.cacheDir, "backup_import_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val file = File(tempDir, entry.name)
                            if (entry.isDirectory) {
                                file.mkdirs()
                            } else {
                                file.parentFile?.mkdirs()
                                FileOutputStream(file).use { out ->
                                    zip.copyTo(out)
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                } ?: return Result.failure(Exception("Cannot open backup file"))

                val manifestFile = File(tempDir, MANIFEST_FILE)
                if (!manifestFile.exists()) {
                    return Result.failure(Exception("Invalid backup: missing manifest"))
                }

                val manifest = json.decodeFromString<BackupManifest>(manifestFile.readText())
                if (manifest.version > BackupManifest.CURRENT_VERSION) {
                    return Result.failure(
                        Exception("Backup version ${manifest.version} is not supported")
                    )
                }

                val productsFile = File(tempDir, PRODUCTS_FILE)
                val categoriesFile = File(tempDir, CATEGORIES_FILE)
                val unitsFile = File(tempDir, UNITS_FILE)

                if (!productsFile.exists() || !categoriesFile.exists() || !unitsFile.exists()) {
                    return Result.failure(Exception("Invalid backup: missing data files"))
                }

                val products = json.decodeFromString<List<Product>>(productsFile.readText())
                val categories = json.decodeFromString<List<Category>>(categoriesFile.readText())
                val units = json.decodeFromString<List<MeasurementUnit>>(unitsFile.readText())

                productRepository.deleteAll()
                categoryRepository.deleteAll()
                measurementUnitRepository.deleteAll()

                categoryRepository.insertAll(categories)
                measurementUnitRepository.insertAll(units)
                productRepository.insertAll(products)

                val photosDir = File(tempDir, PHOTOS_DIR)
                var importedPhotos = 0
                if (photosDir.exists()) {
                    photosDir.walkTopDown().filter { it.isFile }.forEach { photoFile ->
                        val relativePath = photoFile.relativeTo(photosDir).path
                        if (photoManager.importPhotoFromBackup(photoFile, relativePath)) {
                            importedPhotos++
                        }
                    }
                }

                Result.success(
                    BackupCounts(
                        products = products.size,
                        categories = categories.size,
                        measurementUnits = units.size,
                        photos = importedPhotos
                    )
                )
            } finally {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createManifest(
        productCount: Int,
        categoryCount: Int,
        unitCount: Int,
        photoCount: Int
    ): BackupManifest {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersion = packageInfo.versionName ?: "unknown"

        return BackupManifest(
            deviceInfo = DeviceInfo(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,
                appVersion = appVersion
            ),
            counts = BackupCounts(
                products = productCount,
                categories = categoryCount,
                measurementUnits = unitCount,
                photos = photoCount
            )
        )
    }

    private fun addJsonEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun addFileEntry(zip: ZipOutputStream, name: String, file: File) {
        zip.putNextEntry(ZipEntry(name))
        FileInputStream(file).use { input ->
            input.copyTo(zip)
        }
        zip.closeEntry()
    }

    companion object {
        private const val MANIFEST_FILE = "manifest.json"
        private const val PRODUCTS_FILE = "data/products.json"
        private const val CATEGORIES_FILE = "data/categories.json"
        private const val UNITS_FILE = "data/measurement_units.json"
        private const val PHOTOS_DIR = "photos"
    }
}
