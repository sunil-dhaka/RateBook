package com.example.ratebook.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "measurement_units",
    indices = [
        Index(value = ["code"], unique = true)
    ]
)
data class MeasurementUnit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val code: String,

    val name: String,

    val symbol: String,

    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
