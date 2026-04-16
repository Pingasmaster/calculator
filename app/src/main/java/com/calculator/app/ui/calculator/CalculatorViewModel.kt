package com.calculator.app.ui.calculator

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calculator.app.CalculatorApplication
import com.calculator.app.data.repository.HistoryRepository
import com.calculator.app.domain.engine.CalculatorEngine
import com.calculator.app.domain.model.CalculatorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CalculatorViewModel(
    private val historyRepo: HistoryRepository,
    private val savedStateHandle: SavedStateHandle,
    private val engine: CalculatorEngine = CalculatorEngine(),
) : ViewModel() {

    companion object {
        private const val KEY_EXPRESSION = "expression"
        private const val KEY_DISPLAY = "displayText"
        private const val KEY_RESULT_DISPLAYED = "isResultDisplayed"
        private const val KEY_IS_ERROR = "isError"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalculatorApplication
                val historyRepo = HistoryRepository(app.database.historyDao())
                val savedStateHandle = createSavedStateHandle()
                CalculatorViewModel(historyRepo, savedStateHandle)
            }
        }
    }

    val expressionField = TextFieldState(
        initialText = savedStateHandle[KEY_EXPRESSION] ?: "",
    )

    private val _state = MutableStateFlow(
        run {
            val expr = savedStateHandle[KEY_EXPRESSION] ?: ""
            CalculatorState(
                expression = expr,
                displayText = savedStateHandle[KEY_DISPLAY] ?: "0",
                openParenCount = parenCount(expr),
                isResultDisplayed = savedStateHandle[KEY_RESULT_DISPLAYED] ?: false,
                isError = savedStateHandle[KEY_IS_ERROR] ?: false,
            )
        }
    )
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    val history = historyRepo.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val operators = setOf("+", "−", "×", "÷")

    private val expression: String get() = expressionField.text.toString()

    private val cursor: Int get() = expressionField.selection.start

    private fun parenCount(expr: String = expression): Int =
        (expr.count { it == '(' } - expr.count { it == ')' }).coerceAtLeast(0)

    private fun syncState() {
        val expr = expression
        _state.update {
            it.copy(
                expression = expr,
                displayText = expr.ifEmpty { "0" },
                openParenCount = parenCount(expr),
                isError = false,
            )
        }
    }

    private fun saveState() {
        val s = _state.value
        savedStateHandle[KEY_EXPRESSION] = if (s.isResultDisplayed) s.expression else expression
        savedStateHandle[KEY_DISPLAY] = s.displayText
        savedStateHandle[KEY_RESULT_DISPLAYED] = s.isResultDisplayed
        savedStateHandle[KEY_IS_ERROR] = s.isError
    }

    fun onButtonClick(symbol: String) {
        when (symbol) {
            "AC" -> onClear()
            "⌫" -> onBackspace()
            "=" -> onEquals()
            "()" -> toggleParentheses()
            "%" -> appendToExpression("%")
            "!" -> appendToExpression("!")
            "√" -> appendSqrt()
            "π" -> appendConstant("π")
            "e" -> appendConstant("e")
            in operators -> appendOperator(symbol)
            else -> appendDigit(symbol)
        }
    }

    private fun appendDigit(digit: String) {
        val current = _state.value
        if (current.isResultDisplayed || current.isError) {
            expressionField.setTextAndPlaceCursorAtEnd(digit)
            _state.update { CalculatorState(expression = digit, displayText = digit) }
        } else {
            if (digit == ".") {
                val pos = cursor
                val text = expression
                val charBefore = if (pos > 0) text[pos - 1] else null
                if (charBefore == 'π' || charBefore == 'e' || charBefore == '%' || charBefore == '!') return
                val leftNum = text.substring(0, pos).takeLastWhile { it.isDigit() || it == '.' }
                val rightNum = text.substring(pos).takeWhile { it.isDigit() || it == '.' }
                if ('.' in (leftNum + rightNum)) return
            }
            expressionField.edit {
                val pos = selection.start
                insert(pos, digit)
                selection = TextRange(pos + digit.length)
            }
            syncState()
        }
        saveState()
        updatePreview()
    }

    private fun appendOperator(op: String) {
        val current = _state.value
        if (current.isError) {
            expressionField.clearText()
            _state.update { CalculatorState() }
            saveState()
            return
        }

        if (current.isResultDisplayed) {
            val result = current.displayText
            expressionField.setTextAndPlaceCursorAtEnd(result + op)
            _state.update {
                val newExpr = result + op
                CalculatorState(expression = newExpr, displayText = newExpr)
            }
        } else {
            val text = expression
            if (text.isEmpty()) return

            val pos = cursor
            if (pos == text.length) {
                val lastChar = text.last().toString()
                if (lastChar in operators) {
                    expressionField.edit {
                        delete(length - 1, length)
                        append(op)
                        placeCursorAtEnd()
                    }
                } else {
                    expressionField.edit {
                        append(op)
                        placeCursorAtEnd()
                    }
                }
            } else {
                expressionField.edit {
                    insert(pos, op)
                    selection = TextRange(pos + op.length)
                }
            }
            syncState()
        }
        saveState()
        updatePreview()
    }

    private fun appendConstant(constant: String) {
        val current = _state.value
        if (current.isResultDisplayed || current.isError) {
            expressionField.setTextAndPlaceCursorAtEnd(constant)
            _state.update { CalculatorState(expression = constant, displayText = constant) }
        } else {
            expressionField.edit {
                val pos = selection.start
                insert(pos, constant)
                selection = TextRange(pos + constant.length)
            }
            syncState()
        }
        saveState()
        updatePreview()
    }

    private fun appendSqrt() {
        val current = _state.value
        if (current.isResultDisplayed || current.isError) {
            expressionField.setTextAndPlaceCursorAtEnd("√(")
            _state.update { CalculatorState(expression = "√(", displayText = "√(", openParenCount = 1) }
        } else {
            expressionField.edit {
                val pos = selection.start
                insert(pos, "√(")
                selection = TextRange(pos + 2)
            }
            syncState()
        }
        saveState()
        updatePreview()
    }

    private fun appendToExpression(symbol: String) {
        val current = _state.value
        if (current.isError) {
            expressionField.clearText()
            _state.update { CalculatorState() }
            saveState()
            return
        }
        if (!current.isResultDisplayed && expression.isEmpty()) return

        if (current.isResultDisplayed) {
            val newExpr = current.displayText + symbol
            expressionField.setTextAndPlaceCursorAtEnd(newExpr)
            _state.update { CalculatorState(expression = newExpr, displayText = newExpr) }
        } else {
            expressionField.edit {
                val pos = selection.start
                insert(pos, symbol)
                selection = TextRange(pos + symbol.length)
            }
            syncState()
        }
        saveState()
        updatePreview()
    }

    private fun toggleParentheses() {
        val current = _state.value

        if (current.isResultDisplayed || current.isError) {
            expressionField.setTextAndPlaceCursorAtEnd("(")
            _state.update { CalculatorState(expression = "(", displayText = "(", openParenCount = 1) }
            saveState()
            updatePreview()
            return
        }

        val text = expression
        val pos = if (text.isEmpty()) 0 else cursor
        val charBefore = if (pos > 0 && text.isNotEmpty()) text[pos - 1] else null

        val shouldClose = parenCount() > 0 &&
                text.isNotEmpty() &&
                charBefore != null &&
                charBefore.toString() !in operators &&
                charBefore != '('

        if (shouldClose) {
            expressionField.edit {
                insert(pos, ")")
                selection = TextRange(pos + 1)
            }
        } else {
            val prefix = if (charBefore != null && (charBefore.isDigit() || charBefore == ')' || charBefore == 'π' || charBefore == 'e' || charBefore == '%' || charBefore == '!')) {
                "×("
            } else {
                "("
            }
            expressionField.edit {
                insert(pos, prefix)
                selection = TextRange(pos + prefix.length)
            }
        }
        syncState()
        saveState()
        updatePreview()
    }

    private fun onEquals() {
        var expr = expression

        if (expr.isEmpty()) return

        // Strip trailing operators
        while (expr.isNotEmpty() && expr.last().toString() in operators) {
            expr = expr.dropLast(1)
        }
        if (expr.isEmpty()) return

        val openParens = parenCount(expr)
        val closedExpr = expr + ")".repeat(openParens)

        engine.evaluate(closedExpr).fold(
            onSuccess = { result ->
                expressionField.setTextAndPlaceCursorAtEnd(closedExpr)
                _state.update {
                    it.copy(
                        expression = closedExpr,
                        displayText = result,
                        previewResult = "",
                        isResultDisplayed = true,
                        isError = false,
                        openParenCount = 0,
                    )
                }
                saveState()
                viewModelScope.launch { historyRepo.addEntry(closedExpr, result) }
            },
            onFailure = {
                _state.update {
                    it.copy(
                        displayText = "Error",
                        previewResult = "",
                        isError = true,
                        isResultDisplayed = true,
                        openParenCount = 0,
                    )
                }
                saveState()
            },
        )
    }

    private fun onClear() {
        expressionField.clearText()
        _state.update { CalculatorState() }
        saveState()
    }

    private fun onBackspace() {
        val current = _state.value
        if (current.isResultDisplayed || current.isError) {
            expressionField.clearText()
            _state.update { CalculatorState() }
            saveState()
            return
        }

        val text = expression
        val pos = cursor
        if (text.isEmpty() || pos == 0) return

        val removed = text[pos - 1]
        expressionField.edit {
            delete(pos - 1, pos)
            if (removed == '(' && pos >= 2 && text[pos - 2] == '√') {
                delete(pos - 2, pos - 1)
                selection = TextRange(pos - 2)
            } else {
                selection = TextRange(pos - 1)
            }
        }
        syncState()
        saveState()
        updatePreview()
    }

    private fun updatePreview() {
        val expr = expression
        if (expr.isEmpty() || _state.value.isResultDisplayed) {
            _state.update { it.copy(previewResult = "") }
            return
        }

        val closedExpr = expr + ")".repeat(parenCount())

        engine.evaluate(closedExpr).fold(
            onSuccess = { preview ->
                _state.update { it.copy(previewResult = preview) }
            },
            onFailure = {
                _state.update { it.copy(previewResult = "") }
            },
        )
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepo.clearHistory() }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch { historyRepo.deleteEntry(id) }
    }

    fun loadFromHistory(value: String) {
        expressionField.setTextAndPlaceCursorAtEnd(value)
        _state.update {
            CalculatorState(
                expression = value,
                displayText = value,
                openParenCount = parenCount(value),
            )
        }
        saveState()
        updatePreview()
    }
}
