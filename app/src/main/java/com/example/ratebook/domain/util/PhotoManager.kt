package com.example.ratebook.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PhotoManager(private val context: Context) {

    private val photosDir: File
        get() = File(context.filesDir, PHOTOS_DIR)

    fun savePhoto(uri: Uri, productId: Long): String? {
        return try {
            val bitmap = loadAndCompressBitmap(uri) ?: return null
            val relativePath = generatePhotoPath(productId)
            val file = getPhotoFile(relativePath)

            file.parentFile?.mkdirs()

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            bitmap.recycle()
            relativePath
        } catch (e: Exception) {
            null
        }
    }

    fun savePhoto(bitmap: Bitmap, productId: Long): String? {
        return try {
            val compressedBitmap = compressBitmap(bitmap)
            val relativePath = generatePhotoPath(productId)
            val file = getPhotoFile(relativePath)

            file.parentFile?.mkdirs()

            FileOutputStream(file).use { out ->
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            if (compressedBitmap != bitmap) {
                compressedBitmap.recycle()
            }

            relativePath
        } catch (e: Exception) {
            null
        }
    }

    fun getPhotoFile(relativePath: String): File {
        return File(photosDir, relativePath)
    }

    fun getAbsolutePath(relativePath: String): String {
        return getPhotoFile(relativePath).absolutePath
    }

    fun deletePhoto(relativePath: String): Boolean {
        return try {
            val file = getPhotoFile(relativePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun copyPhotoToExportDir(relativePath: String, exportDir: File): Boolean {
        return try {
            val sourceFile = getPhotoFile(relativePath)
            if (!sourceFile.exists()) return false

            val destFile = File(exportDir, relativePath)
            destFile.parentFile?.mkdirs()
            sourceFile.copyTo(destFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun importPhotoFromBackup(backupFile: File, relativePath: String): Boolean {
        return try {
            val destFile = getPhotoFile(relativePath)
            destFile.parentFile?.mkdirs()
            backupFile.copyTo(destFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearAllPhotos(): Boolean {
        return try {
            photosDir.deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun loadAndCompressBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            compressBitmap(original).also {
                if (it != original) original.recycle()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val maxDimension = MAX_DIMENSION
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val scale = if (width > height) {
            maxDimension.toFloat() / width
        } else {
            maxDimension.toFloat() / height
        }

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun generatePhotoPath(productId: Long): String {
        val now = LocalDate.now()
        val year = now.format(DateTimeFormatter.ofPattern("yyyy"))
        val month = now.format(DateTimeFormatter.ofPattern("MM"))
        val timestamp = System.currentTimeMillis()

        return "$year/$month/product_${productId}_$timestamp.jpg"
    }

    companion object {
        private const val PHOTOS_DIR = "photos"
        private const val MAX_DIMENSION = 1024
        private const val JPEG_QUALITY = 85
    }
}
