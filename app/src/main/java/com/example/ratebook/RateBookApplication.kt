package com.example.ratebook

import android.app.Application
import com.example.ratebook.data.backup.BackupManager
import com.example.ratebook.data.database.RateBookDatabase
import com.example.ratebook.data.repository.CategoryRepository
import com.example.ratebook.data.repository.MeasurementUnitRepository
import com.example.ratebook.data.repository.ProductRepository
import com.example.ratebook.domain.util.PhotoManager
import com.example.ratebook.domain.util.SkuGenerator

class RateBookApplication : Application() {

    val database: RateBookDatabase by lazy {
        RateBookDatabase.getDatabase(this)
    }

    val photoManager: PhotoManager by lazy {
        PhotoManager(this)
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao())
    }

    val measurementUnitRepository: MeasurementUnitRepository by lazy {
        MeasurementUnitRepository(database.measurementUnitDao())
    }

    val skuGenerator: SkuGenerator by lazy {
        SkuGenerator(database.productDao())
    }

    val productRepository: ProductRepository by lazy {
        ProductRepository(database.productDao(), skuGenerator, photoManager)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(
            this,
            productRepository,
            categoryRepository,
            measurementUnitRepository,
            photoManager
        )
    }
}
