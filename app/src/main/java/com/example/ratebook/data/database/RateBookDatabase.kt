package com.example.ratebook.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ratebook.data.database.dao.CategoryDao
import com.example.ratebook.data.database.dao.MeasurementUnitDao
import com.example.ratebook.data.database.dao.ProductDao
import com.example.ratebook.data.database.entity.Category
import com.example.ratebook.data.database.entity.MeasurementUnit
import com.example.ratebook.data.database.entity.Product
import com.example.ratebook.domain.util.TextNormalizer
import java.util.concurrent.Executors

@Database(
    entities = [Product::class, Category::class, MeasurementUnit::class],
    version = 1,
    exportSchema = false
)
abstract class RateBookDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun measurementUnitDao(): MeasurementUnitDao

    companion object {
        private const val DATABASE_NAME = "ratebook.db"

        @Volatile
        private var INSTANCE: RateBookDatabase? = null

        fun getDatabase(context: Context): RateBookDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): RateBookDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                RateBookDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Executors.newSingleThreadExecutor().execute {
                            val database = getDatabase(context)
                            database.runInTransaction {
                                prepopulateCategories(database.categoryDao())
                                prepopulateMeasurementUnits(database.measurementUnitDao())
                            }
                        }
                    }
                })
                .build()
        }

        private fun prepopulateCategories(categoryDao: CategoryDao) {
            val categories = listOf(
                Category(
                    name = "गुटखा / Gutkha",
                    nameNormalized = TextNormalizer.normalize("गुटखा / Gutkha"),
                    displayOrder = 1
                ),
                Category(
                    name = "सिगरेट / Cigarette",
                    nameNormalized = TextNormalizer.normalize("सिगरेट / Cigarette"),
                    displayOrder = 2
                ),
                Category(
                    name = "राशन (चाय, चीनी, चावल) / Rashan",
                    nameNormalized = TextNormalizer.normalize("राशन चाय चीनी चावल Rashan"),
                    displayOrder = 3
                ),
                Category(
                    name = "तेल (खाने का, बालों का) / Oil",
                    nameNormalized = TextNormalizer.normalize("तेल खाने का बालों का Oil tel"),
                    displayOrder = 4
                )
            )
            categoryDao.insertAllSync(categories)
        }

        private fun prepopulateMeasurementUnits(unitDao: MeasurementUnitDao) {
            val units = listOf(
                MeasurementUnit(
                    code = "unit",
                    name = "Unit/Piece / Nag",
                    symbol = "pcs",
                    displayOrder = 1
                ),
                MeasurementUnit(
                    code = "kg",
                    name = "Kilogram / Kilogram",
                    symbol = "kg",
                    displayOrder = 2
                ),
                MeasurementUnit(
                    code = "g",
                    name = "Gram / Gram",
                    symbol = "g",
                    displayOrder = 3
                ),
                MeasurementUnit(
                    code = "ltr",
                    name = "Liter / Litar",
                    symbol = "L",
                    displayOrder = 4
                ),
                MeasurementUnit(
                    code = "ml",
                    name = "Milliliter / Mililitar",
                    symbol = "mL",
                    displayOrder = 5
                ),
                MeasurementUnit(
                    code = "packet",
                    name = "Packet / Paiket",
                    symbol = "pkt",
                    displayOrder = 6
                ),
                MeasurementUnit(
                    code = "box",
                    name = "Box / Dibba",
                    symbol = "box",
                    displayOrder = 7
                ),
                MeasurementUnit(
                    code = "dozen",
                    name = "Dozen / Darjan",
                    symbol = "dz",
                    displayOrder = 8
                ),
                MeasurementUnit(
                    code = "bottle",
                    name = "Bottle / Botal",
                    symbol = "btl",
                    displayOrder = 9
                ),
                MeasurementUnit(
                    code = "bag",
                    name = "Bag / Thaila",
                    symbol = "bag",
                    displayOrder = 10
                )
            )
            unitDao.insertAllSync(units)
        }
    }
}
