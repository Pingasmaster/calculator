package com.calculator.app.data.local.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.calculator.app.data.local.db.CalculatorDatabase
import com.calculator.app.data.local.db.entity.HistoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HistoryDaoTest {

    private lateinit var db: CalculatorDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CalculatorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.historyDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert then observe returns entry`() = runTest {
        dao.insert(HistoryEntity(expression = "1+1", result = "2", timestamp = 100L))
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("1+1", items[0].expression)
            assertEquals("2", items[0].result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAll orders by timestamp descending`() = runTest {
        dao.insert(HistoryEntity(expression = "a", result = "1", timestamp = 100L))
        dao.insert(HistoryEntity(expression = "b", result = "2", timestamp = 300L))
        dao.insert(HistoryEntity(expression = "c", result = "3", timestamp = 200L))

        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(listOf("b", "c", "a"), items.map { it.expression })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAll empties the table`() = runTest {
        dao.insert(HistoryEntity(expression = "x", result = "1", timestamp = 1L))
        dao.insert(HistoryEntity(expression = "y", result = "2", timestamp = 2L))
        dao.clearAll()

        dao.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteById removes only the matching row`() = runTest {
        dao.insert(HistoryEntity(id = 0, expression = "a", result = "1", timestamp = 1L))
        dao.insert(HistoryEntity(id = 0, expression = "b", result = "2", timestamp = 2L))
        dao.insert(HistoryEntity(id = 0, expression = "c", result = "3", timestamp = 3L))

        dao.observeAll().test {
            val before = awaitItem()
            assertEquals(3, before.size)
            val middleId = before.first { it.expression == "b" }.id

            dao.deleteById(middleId)
            val after = awaitItem()
            assertEquals(2, after.size)
            assertTrue(after.none { it.expression == "b" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `trimToSize keeps newest N by timestamp`() = runTest {
        for (ts in 1..10) {
            dao.insert(HistoryEntity(expression = "e$ts", result = "r$ts", timestamp = ts.toLong()))
        }
        dao.trimToSize(5)

        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(5, items.size)
            // Should be timestamps 6..10 in descending order.
            assertEquals(listOf(10L, 9L, 8L, 7L, 6L), items.map { it.timestamp })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `trimToSize with larger keepCount than rows is no-op`() = runTest {
        dao.insert(HistoryEntity(expression = "a", result = "1", timestamp = 1L))
        dao.insert(HistoryEntity(expression = "b", result = "2", timestamp = 2L))
        dao.trimToSize(100)

        dao.observeAll().test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `trimToSize on empty table is no-op`() = runTest {
        dao.trimToSize(5)
        dao.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insert with id zero auto-generates unique ids`() = runTest {
        dao.insert(HistoryEntity(expression = "a", result = "1", timestamp = 1L))
        dao.insert(HistoryEntity(expression = "b", result = "2", timestamp = 2L))

        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            val ids = items.map { it.id }.toSet()
            assertEquals(2, ids.size)
            assertTrue(ids.all { it > 0L })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observe emits fresh list on insert`() = runTest {
        dao.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            dao.insert(HistoryEntity(expression = "a", result = "1", timestamp = 1L))
            val next = awaitItem()
            assertEquals(1, next.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observe emits fresh list on deleteById`() = runTest {
        dao.insert(HistoryEntity(expression = "a", result = "1", timestamp = 1L))
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            dao.deleteById(items[0].id)
            val after = awaitItem()
            assertTrue(after.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
