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
        if (items.isEmpty()) return
        val now = System.currentTimeMillis()
        val itemsByName = items.associateBy { it.name.lowercase() }
        val existingIdsByName = dao.findByNames(itemsByName.keys.toList())
            .associate { it.name.lowercase() to it.id }
        dao.upsertAll(
            itemsByName.map { (name, item) ->
                item.toEntity(existingIdsByName[name] ?: item.id, now)
            },
        )
    }

    suspend fun upsert(item: DetectedIngredient) {
        dao.upsert(item.toEntity(item.id, System.currentTimeMillis()))
    }

    suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }
}

private fun DetectedIngredient.toEntity(id: String, updatedAtEpochMs: Long) = InventoryEntity(
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
