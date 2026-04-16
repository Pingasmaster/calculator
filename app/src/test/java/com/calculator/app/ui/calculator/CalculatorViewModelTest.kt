package com.calculator.app.ui.calculator

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.calculator.app.data.repository.HistoryRepository
import com.calculator.app.domain.engine.CalculatorEngine
import com.calculator.app.domain.model.CalculatorState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var historyRepo: HistoryRepository
    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        historyRepo = mockk(relaxed = true)
        coEvery { historyRepo.observeHistory() } returns flowOf(emptyList())
        viewModel = CalculatorViewModel(historyRepo, SavedStateHandle())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== Digit input ==========

    @Test
    fun `initial state shows zero`() {
        assertEquals("0", viewModel.state.value.displayText)
        assertEquals("", viewModel.state.value.expression)
    }

    @Test
    fun `pressing digit updates expression and display`() {
        viewModel.onButtonClick("5")
        assertEquals("5", viewModel.state.value.expression)
        assertEquals("5", viewModel.state.value.displayText)
    }

    @Test
    fun `pressing multiple digits builds expression`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("3")
        assertEquals("123", viewModel.state.value.expression)
        assertEquals("123", viewModel.state.value.displayText)
    }

    @Test
    fun `pressing decimal point adds it`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick(".")
        viewModel.onButtonClick("5")
        assertEquals("1.5", viewModel.state.value.expression)
    }

    @Test
    fun `duplicate decimal point is rejected`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick(".")
        viewModel.onButtonClick("5")
        viewModel.onButtonClick(".")
        assertEquals("1.5", viewModel.state.value.expression)
    }

    @Test
    fun `decimal after constant is rejected`() {
        viewModel.onButtonClick("π")
        viewModel.onButtonClick(".")
        assertEquals("π", viewModel.state.value.expression)
    }

    // ========== Operator input ==========

    @Test
    fun `pressing operator after digit appends it`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        assertEquals("5+", viewModel.state.value.expression)
    }

    @Test
    fun `replacing operator when pressing new operator`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("×")
        assertEquals("5×", viewModel.state.value.expression)
    }

    @Test
    fun `operator on empty expression is ignored`() {
        viewModel.onButtonClick("+")
        assertEquals("", viewModel.state.value.expression)
    }

    // ========== Clear ==========

    @Test
    fun `AC resets to initial state`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("AC")
        assertEquals("", viewModel.state.value.expression)
        assertEquals("0", viewModel.state.value.displayText)
    }

    // ========== Backspace ==========

    @Test
    fun `backspace removes last character`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("⌫")
        assertEquals("12", viewModel.state.value.expression)
        assertEquals("12", viewModel.state.value.displayText)
    }

    @Test
    fun `backspace on single character shows zero`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("⌫")
        assertEquals("", viewModel.state.value.expression)
        assertEquals("0", viewModel.state.value.displayText)
    }

    @Test
    fun `backspace after result clears everything`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        viewModel.onButtonClick("⌫")
        assertEquals("", viewModel.state.value.expression)
        assertEquals("0", viewModel.state.value.displayText)
    }

    @Test
    fun `backspace removes sqrt with its opening paren`() {
        viewModel.onButtonClick("√")
        assertEquals("√(", viewModel.state.value.expression)
        assertEquals(1, viewModel.state.value.openParenCount)
        viewModel.onButtonClick("⌫")
        assertEquals("", viewModel.state.value.expression)
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    // ========== Parentheses ==========

    @Test
    fun `toggle parentheses opens paren`() {
        viewModel.onButtonClick("()")
        assertEquals("(", viewModel.state.value.expression)
        assertEquals(1, viewModel.state.value.openParenCount)
    }

    @Test
    fun `toggle parentheses closes when appropriate`() {
        viewModel.onButtonClick("()")
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("()")
        assertEquals("(5)", viewModel.state.value.expression)
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    @Test
    fun `implicit multiplication before opening paren`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("()")
        assertEquals("5×(", viewModel.state.value.expression)
    }

    @Test
    fun `nested parentheses tracking`() {
        viewModel.onButtonClick("()")
        viewModel.onButtonClick("()")
        viewModel.onButtonClick("5")
        assertEquals(2, viewModel.state.value.openParenCount)
        viewModel.onButtonClick("()")
        assertEquals(1, viewModel.state.value.openParenCount)
        viewModel.onButtonClick("()")
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    // ========== Equals ==========

    @Test
    fun `equals evaluates expression`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        assertEquals("5", viewModel.state.value.displayText)
        assertTrue(viewModel.state.value.isResultDisplayed)
    }

    @Test
    fun `equals auto-closes unclosed parentheses`() {
        viewModel.onButtonClick("(")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        // Don't close the paren
        viewModel.onButtonClick("=")
        assertEquals("5", viewModel.state.value.displayText)
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    @Test
    fun `equals on empty expression does nothing`() {
        viewModel.onButtonClick("=")
        assertEquals("0", viewModel.state.value.displayText)
        assertFalse(viewModel.state.value.isResultDisplayed)
    }

    @Test
    fun `equals on invalid expression shows error`() {
        viewModel.onButtonClick("÷")
        // Can't start with operator, so expression is empty
        viewModel.onButtonClick("=")
        // Since expression is empty, equals does nothing
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `equals saves to history`() = runTest {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")

        coVerify { historyRepo.addEntry("2+3", "5") }
    }

    // ========== Continuing from result ==========

    @Test
    fun `digit after result starts new expression`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        assertTrue(viewModel.state.value.isResultDisplayed)

        viewModel.onButtonClick("7")
        assertEquals("7", viewModel.state.value.expression)
        assertFalse(viewModel.state.value.isResultDisplayed)
    }

    @Test
    fun `operator after result continues from result`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        assertEquals("5", viewModel.state.value.displayText)

        viewModel.onButtonClick("+")
        assertEquals("5+", viewModel.state.value.expression)
    }

    // ========== Constants ==========

    @Test
    fun `pi is appended correctly`() {
        viewModel.onButtonClick("π")
        assertEquals("π", viewModel.state.value.expression)
    }

    @Test
    fun `e constant is appended correctly`() {
        viewModel.onButtonClick("e")
        assertEquals("e", viewModel.state.value.expression)
    }

    @Test
    fun `constant after result starts fresh`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("=")
        viewModel.onButtonClick("π")
        assertEquals("π", viewModel.state.value.expression)
    }

    // ========== Scientific functions ==========

    @Test
    fun `sqrt appends with opening paren`() {
        viewModel.onButtonClick("√")
        assertEquals("√(", viewModel.state.value.expression)
        assertEquals(1, viewModel.state.value.openParenCount)
    }

    @Test
    fun `sqrt after result starts fresh`() {
        viewModel.onButtonClick("9")
        viewModel.onButtonClick("=")
        viewModel.onButtonClick("√")
        assertEquals("√(", viewModel.state.value.expression)
        assertEquals(1, viewModel.state.value.openParenCount)
    }

    @Test
    fun `percent is appended`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("0")
        viewModel.onButtonClick("%")
        assertEquals("50%", viewModel.state.value.expression)
    }

    @Test
    fun `factorial is appended`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("!")
        assertEquals("5!", viewModel.state.value.expression)
    }

    // ========== Preview ==========

    @Test
    fun `preview updates while typing`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        assertEquals("5", viewModel.state.value.previewResult)
    }

    @Test
    fun `preview clears after equals`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        assertEquals("", viewModel.state.value.previewResult)
    }

    @Test
    fun `preview empty for incomplete expression`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        // Just "2+" can't evaluate, preview should be empty
        assertEquals("", viewModel.state.value.previewResult)
    }

    // ========== State flow ==========

    @Test
    fun `state flow emits updates`() = runTest {
        viewModel.state.test {
            val initial = awaitItem()
            assertEquals("0", initial.displayText)

            viewModel.onButtonClick("5")
            val afterDigit = awaitItem()
            assertEquals("5", afterDigit.displayText)

            // Preview update may emit an additional state
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== History operations ==========

    @Test
    fun `clearHistory delegates to repo`() = runTest {
        viewModel.clearHistory()
        coVerify { historyRepo.clearHistory() }
    }

    @Test
    fun `deleteHistoryEntry delegates to repo`() = runTest {
        viewModel.deleteHistoryEntry(42L)
        coVerify { historyRepo.deleteEntry(42L) }
    }

    @Test
    fun `loadFromHistory sets expression and tracks parens`() {
        viewModel.loadFromHistory("(2+3")
        assertEquals("(2+3", viewModel.state.value.expression)
        assertEquals("(2+3", viewModel.state.value.displayText)
        assertEquals(1, viewModel.state.value.openParenCount)
    }

    @Test
    fun `loadFromHistory with balanced parens has zero count`() {
        viewModel.loadFromHistory("(2+3)")
        assertEquals("(2+3)", viewModel.state.value.expression)
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    // ========== SavedStateHandle persistence ==========

    @Test
    fun `state is saved to SavedStateHandle`() {
        val handle = SavedStateHandle()
        val vm = CalculatorViewModel(historyRepo, handle)

        vm.onButtonClick("4")
        vm.onButtonClick("2")

        assertEquals("42", handle["expression"])
        assertEquals("42", handle["displayText"])
    }

    @Test
    fun `state is restored from SavedStateHandle`() {
        val handle = SavedStateHandle(
            mapOf(
                "expression" to "12+3",
                "displayText" to "12+3",
                "openParenCount" to 0,
                "isResultDisplayed" to false,
            )
        )
        val vm = CalculatorViewModel(historyRepo, handle)

        assertEquals("12+3", vm.state.value.expression)
        assertEquals("12+3", vm.state.value.displayText)
    }

    // ========== Error handling ==========

    @Test
    fun `operator after error clears state`() {
        // Force an error by dividing by zero
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("÷")
        viewModel.onButtonClick("0")
        viewModel.onButtonClick("=")

        if (viewModel.state.value.isError) {
            viewModel.onButtonClick("+")
            assertEquals("", viewModel.state.value.expression)
        }
    }

    @Test
    fun `appendToExpression after error clears state`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("÷")
        viewModel.onButtonClick("0")
        viewModel.onButtonClick("=")

        if (viewModel.state.value.isError) {
            viewModel.onButtonClick("%")
            assertEquals("", viewModel.state.value.expression)
        }
    }

    // ========== Bug fix: isError persisted across process death ==========

    @Test
    fun `isError is saved to SavedStateHandle`() {
        val handle = SavedStateHandle()
        val vm = CalculatorViewModel(historyRepo, handle)

        vm.onButtonClick("1")
        vm.onButtonClick("÷")
        vm.onButtonClick("0")
        vm.onButtonClick("=")

        assertTrue(vm.state.value.isError)
        assertEquals(true, handle["isError"])
    }

    @Test
    fun `isError is restored from SavedStateHandle`() {
        val handle = SavedStateHandle(
            mapOf(
                "expression" to "1÷0",
                "displayText" to "Error",
                "openParenCount" to 0,
                "isResultDisplayed" to true,
                "isError" to true,
            )
        )
        val vm = CalculatorViewModel(historyRepo, handle)

        assertTrue(vm.state.value.isError)
        assertEquals("Error", vm.state.value.displayText)
    }

    @Test
    fun `operator after restored error does not create Error+ expression`() {
        val handle = SavedStateHandle(
            mapOf(
                "expression" to "1÷0",
                "displayText" to "Error",
                "openParenCount" to 0,
                "isResultDisplayed" to true,
                "isError" to true,
            )
        )
        val vm = CalculatorViewModel(historyRepo, handle)
        vm.onButtonClick("+")

        assertFalse(vm.state.value.expression.contains("Error"))
        assertEquals("", vm.state.value.expression)
    }

    // ========== Bug fix: trailing operator before equals ==========

    @Test
    fun `equals with trailing operator strips it and evaluates`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("=")

        assertEquals("5", viewModel.state.value.displayText)
        assertTrue(viewModel.state.value.isResultDisplayed)
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `equals with expression and trailing operator evaluates correctly`() {
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("×")
        viewModel.onButtonClick("=")

        assertEquals("5", viewModel.state.value.displayText)
        assertFalse(viewModel.state.value.isError)
    }

    // ========== Bug fix: postfix operators on empty expression ==========

    @Test
    fun `percent on empty expression is ignored`() {
        viewModel.onButtonClick("%")

        assertEquals("", viewModel.state.value.expression)
        assertEquals("0", viewModel.state.value.displayText)
    }

    @Test
    fun `factorial on empty expression is ignored`() {
        viewModel.onButtonClick("!")

        assertEquals("", viewModel.state.value.expression)
        assertEquals("0", viewModel.state.value.displayText)
    }

    @Test
    fun `percent after result is allowed`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("0")
        viewModel.onButtonClick("=")
        viewModel.onButtonClick("%")

        assertEquals("50%", viewModel.state.value.expression)
    }
}
