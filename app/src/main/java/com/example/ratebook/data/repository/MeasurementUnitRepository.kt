package com.example.ratebook.data.repository

import com.example.ratebook.data.database.dao.MeasurementUnitDao
import com.example.ratebook.data.database.entity.MeasurementUnit
import kotlinx.coroutines.flow.Flow

class MeasurementUnitRepository(private val unitDao: MeasurementUnitDao) {

    fun getAllUnits(): Flow<List<MeasurementUnit>> = unitDao.getAllUnits()

    suspend fun getAllUnitsSync(): List<MeasurementUnit> = unitDao.getAllUnitsSync()

    suspend fun getById(id: Long): MeasurementUnit? = unitDao.getById(id)

    suspend fun getByCode(code: String): MeasurementUnit? = unitDao.getByCode(code)

    suspend fun insert(code: String, name: String, symbol: String): Long {
        val maxOrder = unitDao.getMaxDisplayOrder() ?: 0
        val unit = MeasurementUnit(
            code = code,
            name = name,
            symbol = symbol,
            displayOrder = maxOrder + 1
        )
        return unitDao.insert(unit)
    }

    suspend fun update(unit: MeasurementUnit) {
        unitDao.update(unit)
    }

    suspend fun delete(unit: MeasurementUnit) {
        unitDao.delete(unit)
    }

    suspend fun deleteAll() {
        unitDao.deleteAll()
    }

    suspend fun insertAll(units: List<MeasurementUnit>) {
        unitDao.insertAll(units)
    }

    suspend fun getCount(): Int = unitDao.getCount()
}
