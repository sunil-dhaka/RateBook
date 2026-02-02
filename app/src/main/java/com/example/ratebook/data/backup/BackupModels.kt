package com.example.ratebook.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupManifest(
    val version: Int = CURRENT_VERSION,
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("device_info")
    val deviceInfo: DeviceInfo,
    val counts: BackupCounts
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    @SerialName("android_version")
    val androidVersion: String,
    @SerialName("app_version")
    val appVersion: String
)

@Serializable
data class BackupCounts(
    val products: Int,
    val categories: Int,
    @SerialName("measurement_units")
    val measurementUnits: Int,
    val photos: Int
)
