package com.example.ratebook.domain.util

import com.example.ratebook.data.database.dao.ProductDao
import com.example.ratebook.data.database.entity.Category
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SkuGenerator(private val productDao: ProductDao) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMM")

    suspend fun generateSku(category: Category?): String {
        val categoryCode = generateCategoryCode(category)
        val dateCode = LocalDate.now().format(dateFormatter)
        val prefix = "$categoryCode-$dateCode"

        val maxSequence = productDao.getMaxSequenceForPrefix(prefix) ?: 0
        val nextSequence = maxSequence + 1
        val sequenceCode = nextSequence.toString().padStart(4, '0')

        return "$prefix-$sequenceCode"
    }

    private fun generateCategoryCode(category: Category?): String {
        if (category == null) {
            return "GEN"
        }

        val name = category.name
            .uppercase()
            .replace(Regex("[^A-Z]"), "")

        return when {
            name.length >= 3 -> name.substring(0, 3)
            name.isNotEmpty() -> name.padEnd(3, 'X')
            else -> "GEN"
        }
    }

    companion object {
        fun extractCategoryCode(sku: String): String? {
            val parts = sku.split("-")
            return if (parts.size >= 3) parts[0] else null
        }

        fun extractDateCode(sku: String): String? {
            val parts = sku.split("-")
            return if (parts.size >= 3) parts[1] else null
        }

        fun extractSequence(sku: String): Int? {
            val parts = sku.split("-")
            return if (parts.size >= 3) parts[2].toIntOrNull() else null
        }
    }
}
