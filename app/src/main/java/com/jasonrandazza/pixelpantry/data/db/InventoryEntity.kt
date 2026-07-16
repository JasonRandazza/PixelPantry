package com.jasonrandazza.pixelpantry.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val quantityLabel: String,
    val confidence: String,
    val updatedAtEpochMs: Long,
)
