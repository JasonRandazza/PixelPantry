package com.jasonrandazza.pixelpantry.data

import com.jasonrandazza.pixelpantry.data.db.InventoryDao
import com.jasonrandazza.pixelpantry.data.db.InventoryEntity
import com.jasonrandazza.pixelpantry.domain.Confidence
import com.jasonrandazza.pixelpantry.domain.DetectedIngredient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InventoryRepository(private val dao: InventoryDao) {
    fun observeAll(): Flow<List<DetectedIngredient>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun upsertAll(items: List<DetectedIngredient>) {
        val now = System.currentTimeMillis()
        dao.upsertAll(items.map { it.toEntity(now) })
    }
}

private fun DetectedIngredient.toEntity(updatedAtEpochMs: Long) = InventoryEntity(
    id = id,
    name = name,
    quantityLabel = quantityLabel,
    confidence = confidence.name,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun InventoryEntity.toDomain() = DetectedIngredient(
    id = id,
    name = name,
    quantityLabel = quantityLabel,
    confidence = Confidence.entries.firstOrNull { it.name == confidence } ?: Confidence.UNKNOWN,
)
