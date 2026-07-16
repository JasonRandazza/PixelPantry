package com.jasonrandazza.pixelpantry.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jasonrandazza.pixelpantry.data.db.AppDatabase
import com.jasonrandazza.pixelpantry.domain.Confidence
import com.jasonrandazza.pixelpantry.domain.DetectedIngredient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryRepositoryInstrumentedTest {
    @Test
    fun upsertThenObserve() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).build()
        try {
            val repository = InventoryRepository(db.inventoryDao())

            repository.upsertAll(
                listOf(DetectedIngredient("eggs", "6", Confidence.HIGH, id = "1")),
            )

            val items = repository.observeAll().first()
            assertEquals("eggs", items.single().name)
            assertEquals("6", items.single().quantityLabel)
            assertEquals(Confidence.HIGH, items.single().confidence)
            assertEquals("1", items.single().id)
        } finally {
            db.close()
        }
    }
}
