package com.jasonrandazza.pixelpantry.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Upsert
    suspend fun upsertAll(entities: List<InventoryEntity>)
}
