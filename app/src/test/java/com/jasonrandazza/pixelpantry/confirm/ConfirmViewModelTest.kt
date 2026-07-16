package com.jasonrandazza.pixelpantry.confirm

import com.jasonrandazza.pixelpantry.data.InventoryRepository
import com.jasonrandazza.pixelpantry.data.db.InventoryDao
import com.jasonrandazza.pixelpantry.data.db.InventoryEntity
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** One-off confirm never touches inventory, so this dao fails the test if it's called. */
private class UnusedInventoryDao : InventoryDao {
    override fun observeAll(): Flow<List<InventoryEntity>> = error("not used in one-off flow")
    override suspend fun findByNames(names: List<String>): List<InventoryEntity> = error("not used in one-off flow")
    override suspend fun upsertAll(entities: List<InventoryEntity>) = error("not used in one-off flow")
    override suspend fun upsert(entity: InventoryEntity) = error("not used in one-off flow")
    override suspend fun deleteById(id: String) = error("not used in one-off flow")
}

class ConfirmViewModelTest {
    private fun oneOffViewModel(initialText: String) =
        ConfirmViewModel(initialText, ConfirmMode.OneOffSession, InventoryRepository(UnusedInventoryDao()))

    @Test
    fun emptyInitialTextStartsWithOneBlankItem() {
        val viewModel = oneOffViewModel("")
        assertEquals(1, viewModel.ui.value.items.size)
        assertEquals("", viewModel.ui.value.items.single().name)
    }

    @Test
    fun oneOffConfirmDoesNotSetSaveCompletedAndExposesItems() {
        val viewModel = oneOffViewModel("eggs | 6 | high")

        viewModel.confirm()

        val state = viewModel.ui.value
        assertTrue(state.confirmedItems != null)
        assertEquals("eggs", state.confirmedItems!!.single().name)
        assertEquals(false, state.saveCompleted)
    }

    @Test
    fun oneOffConfirmWithNoNamedItemsReportsError() {
        val viewModel = oneOffViewModel("")

        viewModel.confirm()

        val state = viewModel.ui.value
        assertNull(state.confirmedItems)
        assertEquals("Add at least one named item", state.error)
    }
}
