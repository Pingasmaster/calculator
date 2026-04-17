package com.calculator.app.data.local.db

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class CalculatorDatabaseMigrationTest {

    @Test
    fun `migration 1 to 2 creates timestamp index`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        CalculatorDatabase.MIGRATION_1_2.migrate(db)
        verify {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp " +
                        "ON calculation_history(timestamp)"
            )
        }
    }

    @Test
    fun `migration 1 to 2 targets correct versions`() {
        assert(CalculatorDatabase.MIGRATION_1_2.startVersion == 1)
        assert(CalculatorDatabase.MIGRATION_1_2.endVersion == 2)
    }

    @Test
    fun `migration is idempotent (uses IF NOT EXISTS)`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        CalculatorDatabase.MIGRATION_1_2.migrate(db)
        CalculatorDatabase.MIGRATION_1_2.migrate(db)
        // Both runs should just issue the same idempotent CREATE INDEX IF NOT EXISTS statement.
        verify(exactly = 2) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp " +
                        "ON calculation_history(timestamp)"
            )
        }
    }
}
