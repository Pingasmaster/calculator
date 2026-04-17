package com.calculator.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelTest {

    // ========== CalculatorState ==========

    @Test
    fun `CalculatorState defaults`() {
        val s = CalculatorState()
        assertEquals("", s.expression)
        assertEquals("0", s.displayText)
        assertEquals("", s.previewResult)
        assertEquals(false, s.isResultDisplayed)
        assertEquals(false, s.isError)
        assertEquals(0, s.openParenCount)
    }

    @Test
    fun `CalculatorState copy preserves unchanged fields`() {
        val s = CalculatorState(expression = "1+2", displayText = "1+2")
        val t = s.copy(previewResult = "3")
        assertEquals("1+2", t.expression)
        assertEquals("1+2", t.displayText)
        assertEquals("3", t.previewResult)
    }

    @Test
    fun `CalculatorState equality uses all fields`() {
        val a = CalculatorState(expression = "1", isError = true)
        val b = CalculatorState(expression = "1", isError = true)
        val c = CalculatorState(expression = "1", isError = false)
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    // ========== HistoryItem ==========

    @Test
    fun `HistoryItem equality`() {
        val a = HistoryItem(id = 1, expression = "1+1", result = "2", timestamp = 100L)
        val b = HistoryItem(id = 1, expression = "1+1", result = "2", timestamp = 100L)
        assertEquals(a, b)
    }

    @Test
    fun `HistoryItem inequality on id`() {
        val a = HistoryItem(id = 1, expression = "x", result = "y", timestamp = 0L)
        val b = HistoryItem(id = 2, expression = "x", result = "y", timestamp = 0L)
        assertNotEquals(a, b)
    }

    // ========== CalculatorButton ==========

    @Test
    fun `CalculatorButton default contentDescription equals symbol`() {
        val b = CalculatorButton("7", ButtonCategory.NUMBER)
        assertEquals("7", b.contentDescription)
    }

    @Test
    fun `CalculatorButton default widthWeight is one`() {
        val b = CalculatorButton("7", ButtonCategory.NUMBER)
        assertEquals(1f, b.widthWeight)
    }

    // ========== buttonRows structure ==========

    @Test
    fun `buttonRows has five rows`() {
        assertEquals(5, buttonRows.size)
    }

    @Test
    fun `all digits zero through nine are present`() {
        val symbols = buttonRows.flatten().map { it.symbol }
        for (d in 0..9) assertTrue("digit $d missing", symbols.contains(d.toString()))
    }

    @Test
    fun `all four operators present with OPERATOR category`() {
        val ops = buttonRows.flatten().filter { it.category == ButtonCategory.OPERATOR }
        val symbols = ops.map { it.symbol }.toSet()
        assertEquals(setOf("+", "\u2212", "\u00D7", "\u00F7"), symbols)
    }

    @Test
    fun `equals button has double widthWeight`() {
        val eq = buttonRows.flatten().first { it.symbol == "=" }
        assertEquals(2f, eq.widthWeight)
    }

    @Test
    fun `operator buttons have narrower width`() {
        val ops = buttonRows.flatten().filter { it.category == ButtonCategory.OPERATOR }
        assertTrue(ops.all { it.widthWeight == 0.75f })
    }

    @Test
    fun `AC button has AC category`() {
        val ac = buttonRows.flatten().first { it.symbol == "AC" }
        assertEquals(ButtonCategory.AC, ac.category)
    }

    // ========== scientificRow ==========

    @Test
    fun `scientificRow has five entries in expected order`() {
        assertEquals(5, scientificRow.size)
        assertEquals(listOf("\u221A", "\u03C0", "e", "!", "\u232B"), scientificRow.map { it.symbol })
    }

    @Test
    fun `scientificRow sqrt pi e factorial are SCIENTIFIC category`() {
        val scientific = scientificRow.take(4)
        assertTrue(scientific.all { it.category == ButtonCategory.SCIENTIFIC })
    }

    @Test
    fun `scientificRow backspace is BACKSPACE category`() {
        val back = scientificRow.first { it.symbol == "\u232B" }
        assertEquals(ButtonCategory.BACKSPACE, back.category)
    }

    // ========== ButtonCategory enum ==========

    @Test
    fun `ButtonCategory enum has seven values`() {
        assertEquals(7, ButtonCategory.values().size)
    }

    @Test
    fun `ButtonCategory contains expected names`() {
        val names = ButtonCategory.values().map { it.name }.toSet()
        assertEquals(
            setOf("NUMBER", "OPERATOR", "FUNCTION", "AC", "SCIENTIFIC", "EQUALS", "BACKSPACE"),
            names,
        )
    }
}
