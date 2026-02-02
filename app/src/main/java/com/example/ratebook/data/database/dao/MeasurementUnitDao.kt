package com.example.ratebook.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ratebook.data.database.entity.MeasurementUnit
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementUnitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: MeasurementUnit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<MeasurementUnit>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(units: List<MeasurementUnit>)

    @Update
    suspend fun update(unit: MeasurementUnit)

    @Delete
    suspend fun delete(unit: MeasurementUnit)

    @Query("DELETE FROM measurement_units")
    suspend fun deleteAll()

    @Query("SELECT * FROM measurement_units ORDER BY display_order ASC, name ASC")
    fun getAllUnits(): Flow<List<MeasurementUnit>>

    @Query("SELECT * FROM measurement_units ORDER BY display_order ASC, name ASC")
    suspend fun getAllUnitsSync(): List<MeasurementUnit>

    @Query("SELECT * FROM measurement_units WHERE id = :id")
    suspend fun getById(id: Long): MeasurementUnit?

    @Query("SELECT * FROM measurement_units WHERE code = :code")
    suspend fun getByCode(code: String): MeasurementUnit?

    @Query("SELECT COUNT(*) FROM measurement_units")
    suspend fun getCount(): Int

    @Query("SELECT MAX(display_order) FROM measurement_units")
    suspend fun getMaxDisplayOrder(): Int?
}
