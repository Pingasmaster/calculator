package com.calculator.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TokenizerTest {

    // ========== Basic number tokenization ==========

    @Test
    fun `tokenize single digit`() {
        val tokens = Tokenizer.tokenize("5")
        assertEquals(1, tokens.size)
        assertEquals(Token.Number("5"), tokens[0])
    }

    @Test
    fun `tokenize multi-digit number`() {
        val tokens = Tokenizer.tokenize("123")
        assertEquals(1, tokens.size)
        assertEquals(Token.Number("123"), tokens[0])
    }

    @Test
    fun `tokenize decimal number`() {
        val tokens = Tokenizer.tokenize("3.14")
        assertEquals(1, tokens.size)
        assertEquals(Token.Number("3.14"), tokens[0])
    }

    @Test
    fun `tokenize number starting with decimal point`() {
        val tokens = Tokenizer.tokenize(".5")
        // .5 needs a digit after the dot to be parsed as number
        assertEquals(1, tokens.size)
        assertEquals(Token.Number(".5"), tokens[0])
    }

    @Test
    fun `tokenize number with leading zero`() {
        val tokens = Tokenizer.tokenize("007")
        assertEquals(1, tokens.size)
        assertEquals(Token.Number("007"), tokens[0])
    }

    // ========== Operator tokenization ==========

    @Test
    fun `tokenize addition`() {
        val tokens = Tokenizer.tokenize("1+2")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("+", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `tokenize subtraction`() {
        val tokens = Tokenizer.tokenize("5-3")
        assertEquals(3, tokens.size)
        assertEquals("-", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `tokenize multiplication with unicode symbol`() {
        val tokens = Tokenizer.tokenize("2\u00D73")
        assertEquals(3, tokens.size)
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `tokenize division with unicode symbol`() {
        val tokens = Tokenizer.tokenize("6\u00F72")
        assertEquals(3, tokens.size)
        assertEquals("/", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `tokenize minus with unicode symbol`() {
        val tokens = Tokenizer.tokenize("5\u22122")
        assertEquals(3, tokens.size)
        assertEquals("-", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `operator precedence - addition is 1`() {
        val tokens = Tokenizer.tokenize("1+2")
        assertEquals(1, (tokens[1] as Token.Operator).precedence)
    }

    @Test
    fun `operator precedence - subtraction is 1`() {
        val tokens = Tokenizer.tokenize("1-2")
        // first '-' is binary operator because preceded by number
        // Actually, "1-2" -> Number(1), Operator("-", 1), Number(2)
        assertEquals(1, (tokens[1] as Token.Operator).precedence)
    }

    @Test
    fun `operator precedence - multiplication is 2`() {
        val tokens = Tokenizer.tokenize("1*2")
        assertEquals(2, (tokens[1] as Token.Operator).precedence)
    }

    @Test
    fun `operator precedence - division is 2`() {
        val tokens = Tokenizer.tokenize("1/2")
        assertEquals(2, (tokens[1] as Token.Operator).precedence)
    }

    // ========== Unary minus ==========

    @Test
    fun `unary minus at start of expression`() {
        val tokens = Tokenizer.tokenize("-5")
        assertEquals(2, tokens.size)
        assertTrue(tokens[0] is Token.UnaryMinus)
        assertEquals(Token.Number("5"), tokens[1])
    }

    @Test
    fun `unary minus after left paren`() {
        val tokens = Tokenizer.tokenize("(-5)")
        assertTrue(tokens[1] is Token.UnaryMinus)
    }

    @Test
    fun `unary minus after operator`() {
        val tokens = Tokenizer.tokenize("3+-5")
        assertEquals(4, tokens.size)
        assertTrue(tokens[0] is Token.Number)
        assertTrue(tokens[1] is Token.Operator)
        assertTrue(tokens[2] is Token.UnaryMinus)
        assertTrue(tokens[3] is Token.Number)
    }

    @Test
    fun `binary minus after number`() {
        val tokens = Tokenizer.tokenize("5-3")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("-", (tokens[1] as Token.Operator).op)
    }

    // ========== Constants ==========

    @Test
    fun `tokenize pi constant`() {
        val tokens = Tokenizer.tokenize("\u03C0")
        assertEquals(1, tokens.size)
        assertTrue(tokens[0] is Token.Constant)
        assertEquals("\u03C0", (tokens[0] as Token.Constant).name)
    }

    @Test
    fun `tokenize e constant`() {
        val tokens = Tokenizer.tokenize("e")
        assertEquals(1, tokens.size)
        assertTrue(tokens[0] is Token.Constant)
        assertEquals("e", (tokens[0] as Token.Constant).name)
    }

    @Test
    fun `pi value is approximately correct`() {
        val tokens = Tokenizer.tokenize("\u03C0")
        val value = (tokens[0] as Token.Constant).value
        assertTrue(value > BigDecimal("3.14"))
        assertTrue(value < BigDecimal("3.15"))
    }

    @Test
    fun `e value is approximately correct`() {
        val tokens = Tokenizer.tokenize("e")
        val value = (tokens[0] as Token.Constant).value
        assertTrue(value > BigDecimal("2.71"))
        assertTrue(value < BigDecimal("2.72"))
    }

    // ========== Parentheses ==========

    @Test
    fun `tokenize left paren`() {
        val tokens = Tokenizer.tokenize("(1)")
        assertTrue(tokens[0] is Token.LeftParen)
    }

    @Test
    fun `tokenize right paren`() {
        val tokens = Tokenizer.tokenize("(1)")
        assertTrue(tokens[2] is Token.RightParen)
    }

    @Test
    fun `nested parentheses`() {
        val tokens = Tokenizer.tokenize("((1+2))")
        val leftParens = tokens.count { it is Token.LeftParen }
        val rightParens = tokens.count { it is Token.RightParen }
        assertEquals(2, leftParens)
        assertEquals(2, rightParens)
    }

    // ========== Functions ==========

    @Test
    fun `tokenize sqrt function`() {
        val tokens = Tokenizer.tokenize("\u221A(4)")
        assertTrue(tokens[0] is Token.Function)
        assertEquals("sqrt", (tokens[0] as Token.Function).name)
    }

    @Test
    fun `tokenize factorial`() {
        val tokens = Tokenizer.tokenize("5!")
        assertEquals(2, tokens.size)
        assertTrue(tokens[1] is Token.Function)
        assertEquals("!", (tokens[1] as Token.Function).name)
    }

    // ========== Percent ==========

    @Test
    fun `tokenize percent`() {
        val tokens = Tokenizer.tokenize("50%")
        assertEquals(2, tokens.size)
        assertTrue(tokens[1] is Token.Percent)
    }

    // ========== Implicit multiplication ==========

    @Test
    fun `implicit multiply between number and left paren`() {
        val tokens = Tokenizer.tokenize("2(3)")
        // Should be: Number(2), Operator(*), LeftParen, Number(3), RightParen
        assertEquals(5, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `implicit multiply between right paren and number`() {
        val tokens = Tokenizer.tokenize("(3)2")
        // Should be: LeftParen, Number(3), RightParen, Operator(*), Number(2)
        assertEquals(5, tokens.size)
        assertTrue(tokens[3] is Token.Operator)
        assertEquals("*", (tokens[3] as Token.Operator).op)
    }

    @Test
    fun `implicit multiply between right paren and left paren`() {
        val tokens = Tokenizer.tokenize("(2)(3)")
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `implicit multiply between number and pi`() {
        val tokens = Tokenizer.tokenize("2\u03C0")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `implicit multiply between number and e`() {
        val tokens = Tokenizer.tokenize("2e")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `implicit multiply between pi and number`() {
        val tokens = Tokenizer.tokenize("\u03C02")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `implicit multiply between constant and left paren`() {
        val tokens = Tokenizer.tokenize("\u03C0(2)")
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `implicit multiply between right paren and constant`() {
        val tokens = Tokenizer.tokenize("(2)\u03C0")
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `implicit multiply between percent and number`() {
        val tokens = Tokenizer.tokenize("50%2")
        // 50, %, *, 2
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `implicit multiply between factorial and number`() {
        val tokens = Tokenizer.tokenize("5!2")
        // 5, !, *, 2
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `implicit multiply between number and sqrt`() {
        val tokens = Tokenizer.tokenize("2\u221A(4)")
        // 2, *, sqrt, (, 4, )
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `no implicit multiply between operator and number`() {
        val tokens = Tokenizer.tokenize("2+3")
        assertEquals(3, tokens.size)
        // Only one operator, the +
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("+", operators[0].op)
    }

    // ========== Whitespace handling ==========

    @Test
    fun `whitespace is ignored`() {
        val tokens = Tokenizer.tokenize("1 + 2")
        assertEquals(3, tokens.size)
    }

    // ========== Empty and edge cases ==========

    @Test
    fun `empty string produces empty token list`() {
        val tokens = Tokenizer.tokenize("")
        assertTrue(tokens.isEmpty())
    }

    @Test
    fun `whitespace only produces empty token list`() {
        val tokens = Tokenizer.tokenize("   ")
        assertTrue(tokens.isEmpty())
    }

    @Test
    fun `unknown characters throw`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("1@2")
        }
    }

    // ========== Complex expressions ==========

    @Test
    fun `complex expression tokenization`() {
        val tokens = Tokenizer.tokenize("2+3\u00D74")
        assertEquals(5, tokens.size)
        assertEquals(Token.Number("2"), tokens[0])
        assertEquals("+", (tokens[1] as Token.Operator).op)
        assertEquals(Token.Number("3"), tokens[2])
        assertEquals("*", (tokens[3] as Token.Operator).op)
        assertEquals(Token.Number("4"), tokens[4])
    }

    @Test
    fun `expression with all operator types`() {
        val tokens = Tokenizer.tokenize("1+2-3*4/5")
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(4, operators.size)
        assertEquals("+", operators[0].op)
        assertEquals("-", operators[1].op)
        assertEquals("*", operators[2].op)
        assertEquals("/", operators[3].op)
    }

    @Test
    fun `expression with decimal at end of number`() {
        // A lone "3." should tokenize as Number("3") since no digit follows the dot
        val tokens = Tokenizer.tokenize("3.")
        // Trailing dot is stripped during tokenization.
        assertEquals(1, tokens.size)
        assertEquals(Token.Number("3"), tokens[0])
    }

    @Test
    fun `consecutive unary and binary operators`() {
        // "1*-2" should produce: Number(1), Operator(*), UnaryMinus, Number(2)
        val tokens = Tokenizer.tokenize("1*-2")
        assertEquals(4, tokens.size)
        assertTrue(tokens[2] is Token.UnaryMinus)
    }

    @Test
    fun `e followed by letter throws`() {
        // "ex" - 'e' followed by a letter is not tokenized as the constant 'e',
        // and the lookahead leaves both chars as unknown → Tokenizer rejects it.
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("ex")
        }
    }

    @Test
    fun `e at end of expression is a constant`() {
        val tokens = Tokenizer.tokenize("e")
        assertEquals(1, tokens.size)
        assertTrue(tokens[0] is Token.Constant)
    }

    @Test
    fun `e followed by operator is a constant`() {
        val tokens = Tokenizer.tokenize("e+1")
        assertTrue(tokens[0] is Token.Constant)
    }

    // ========== Unicode normalization (explicit) ==========

    @Test
    fun `unicode multiplication sign normalizes to star`() {
        val tokens = Tokenizer.tokenize("2\u00D73")
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `unicode division sign normalizes to slash`() {
        val tokens = Tokenizer.tokenize("6\u00F72")
        assertEquals("/", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `unicode minus as binary operator`() {
        val tokens = Tokenizer.tokenize("5\u22122")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("-", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `unicode minus as unary at start`() {
        val tokens = Tokenizer.tokenize("\u22125")
        assertEquals(2, tokens.size)
        assertTrue(tokens[0] is Token.UnaryMinus)
        assertEquals(Token.Number("5"), tokens[1])
    }

    @Test
    fun `mixed unicode operators in one expression`() {
        val tokens = Tokenizer.tokenize("2\u00D73\u00F74\u22121")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(3, ops.size)
        assertEquals("*", ops[0].op)
        assertEquals("/", ops[1].op)
        assertEquals("-", ops[2].op)
    }

    // ========== Number edge cases ==========

    @Test
    fun `second dot in same number throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("1.2.3")
        }
    }

    @Test
    fun `standalone dot throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize(".")
        }
    }

    @Test
    fun `two leading-dot numbers separated by operator`() {
        val tokens = Tokenizer.tokenize(".5+.25")
        assertEquals(3, tokens.size)
        assertEquals(Token.Number(".5"), tokens[0])
        assertEquals(Token.Number(".25"), tokens[2])
    }

    @Test
    fun `trailing dot followed by operator drops the dot`() {
        val tokens = Tokenizer.tokenize("3.+1")
        assertEquals(3, tokens.size)
        assertEquals(Token.Number("3"), tokens[0])
        assertEquals("+", (tokens[1] as Token.Operator).op)
        assertEquals(Token.Number("1"), tokens[2])
    }

    @Test
    fun `very long integer tokenizes as single number`() {
        val longInt = "1".repeat(50)
        val tokens = Tokenizer.tokenize(longInt)
        assertEquals(1, tokens.size)
        assertEquals(Token.Number(longInt), tokens[0])
    }

    @Test
    fun `very long decimal tokenizes as single number`() {
        val longDec = "0." + "1".repeat(40)
        val tokens = Tokenizer.tokenize(longDec)
        assertEquals(1, tokens.size)
        assertEquals(Token.Number(longDec), tokens[0])
    }

    @Test
    fun `tiny decimal with leading zeros preserved`() {
        val tokens = Tokenizer.tokenize("0.0001")
        assertEquals(1, tokens.size)
        assertEquals(Token.Number("0.0001"), tokens[0])
    }

    @Test
    fun `whitespace between digits produces two numbers with implicit multiply`() {
        val tokens = Tokenizer.tokenize("1 2")
        assertEquals(3, tokens.size)
        assertEquals(Token.Number("1"), tokens[0])
        assertEquals("*", (tokens[1] as Token.Operator).op)
        assertEquals(Token.Number("2"), tokens[2])
    }

    // ========== Operator edge cases ==========

    @Test
    fun `leading plus produces operator and number`() {
        val tokens = Tokenizer.tokenize("+5")
        assertEquals(2, tokens.size)
        assertTrue(tokens[0] is Token.Operator)
        assertEquals("+", (tokens[0] as Token.Operator).op)
        assertEquals(Token.Number("5"), tokens[1])
    }

    @Test
    fun `consecutive binary operators tokenize`() {
        // "5+*3" is tokenized (parser/eval rejects later)
        val tokens = Tokenizer.tokenize("5+*3")
        assertEquals(4, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertTrue(tokens[2] is Token.Operator)
    }

    @Test
    fun `unicode multiply then unicode minus is unary`() {
        val tokens = Tokenizer.tokenize("5\u00D7\u22123")
        assertEquals(4, tokens.size)
        assertEquals("*", (tokens[1] as Token.Operator).op)
        assertTrue(tokens[2] is Token.UnaryMinus)
        assertEquals(Token.Number("3"), tokens[3])
    }

    @Test
    fun `unary minus after multiplication`() {
        val tokens = Tokenizer.tokenize("2*-3")
        assertEquals(4, tokens.size)
        assertTrue(tokens[2] is Token.UnaryMinus)
    }

    @Test
    fun `unary minus after division`() {
        val tokens = Tokenizer.tokenize("2/-3")
        assertEquals(4, tokens.size)
        assertTrue(tokens[2] is Token.UnaryMinus)
    }

    @Test
    fun `unary minus after plus`() {
        val tokens = Tokenizer.tokenize("2+-3")
        assertTrue(tokens[2] is Token.UnaryMinus)
    }

    @Test
    fun `unary minus after another unary minus`() {
        val tokens = Tokenizer.tokenize("--5")
        assertEquals(3, tokens.size)
        assertTrue(tokens[0] is Token.UnaryMinus)
        assertTrue(tokens[1] is Token.UnaryMinus)
        assertEquals(Token.Number("5"), tokens[2])
    }

    @Test
    fun `whitespace between unary minus and operand does not change classification`() {
        val tokens = Tokenizer.tokenize("- 5")
        assertEquals(2, tokens.size)
        assertTrue(tokens[0] is Token.UnaryMinus)
    }

    @Test
    fun `whitespace around binary minus does not change classification`() {
        val tokens = Tokenizer.tokenize("1 - 2")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("-", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `space between number and signed operand is still binary minus`() {
        // "1 -2" tokenizes as 1, -, 2 (binary minus, not implicit mult of 1 and -2)
        val tokens = Tokenizer.tokenize("1 -2")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("-", (tokens[1] as Token.Operator).op)
    }

    // ========== Implicit-multiplication matrix ==========

    @Test
    fun `implicit multiply pi before number`() {
        val tokens = Tokenizer.tokenize("\u03C05")
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `implicit multiply pi before left paren`() {
        val tokens = Tokenizer.tokenize("\u03C0(1)")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
        assertEquals("*", ops[0].op)
    }

    @Test
    fun `implicit multiply pi before sqrt`() {
        val tokens = Tokenizer.tokenize("\u03C0\u221A(4)")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
        assertEquals("*", ops[0].op)
    }

    @Test
    fun `pi followed by e produces pi times e`() {
        val tokens = Tokenizer.tokenize("\u03C0e")
        assertEquals(3, tokens.size)
        assertTrue(tokens[0] is Token.Constant)
        assertEquals("*", (tokens[1] as Token.Operator).op)
        assertTrue(tokens[2] is Token.Constant)
    }

    @Test
    fun `implicit multiply right paren before number`() {
        val tokens = Tokenizer.tokenize("(1)2")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
        assertEquals("*", ops[0].op)
    }

    @Test
    fun `implicit multiply right paren before pi`() {
        val tokens = Tokenizer.tokenize("(1)\u03C0")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
    }

    @Test
    fun `implicit multiply right paren before sqrt`() {
        val tokens = Tokenizer.tokenize("(1)\u221A(4)")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
        assertEquals("*", ops[0].op)
    }

    @Test
    fun `implicit multiply percent before pi`() {
        val tokens = Tokenizer.tokenize("50%\u03C0")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
    }

    @Test
    fun `implicit multiply percent before left paren`() {
        val tokens = Tokenizer.tokenize("50%(2)")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
    }

    @Test
    fun `implicit multiply percent before sqrt`() {
        val tokens = Tokenizer.tokenize("50%\u221A(4)")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
    }

    @Test
    fun `implicit multiply factorial before pi`() {
        val tokens = Tokenizer.tokenize("3!\u03C0")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
    }

    @Test
    fun `implicit multiply factorial before left paren`() {
        val tokens = Tokenizer.tokenize("3!(2)")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
    }

    @Test
    fun `implicit multiply factorial before sqrt`() {
        val tokens = Tokenizer.tokenize("3!\u221A(4)")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
    }

    @Test
    fun `no implicit multiply after left paren`() {
        val tokens = Tokenizer.tokenize("(5)")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(0, ops.size)
    }

    @Test
    fun `no implicit multiply after operator`() {
        val tokens = Tokenizer.tokenize("1+2")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
    }

    @Test
    fun `no implicit multiply after unary minus`() {
        val tokens = Tokenizer.tokenize("-5")
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(0, ops.size)
    }

    // ========== Constant boundaries ==========

    @Test
    fun `e followed by pi throws because pi is a letter`() {
        // 'π' (U+03C0) is a Unicode letter so the 'e' lookahead rejects it.
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("e\u03C0")
        }
    }

    @Test
    fun `e followed by e throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("ee")
        }
    }

    @Test
    fun `e followed by digit is constant times number`() {
        val tokens = Tokenizer.tokenize("e2")
        assertEquals(3, tokens.size)
        assertTrue(tokens[0] is Token.Constant)
        assertEquals("*", (tokens[1] as Token.Operator).op)
        assertEquals(Token.Number("2"), tokens[2])
    }

    @Test
    fun `e before left paren is constant`() {
        val tokens = Tokenizer.tokenize("e(2)")
        assertTrue(tokens[0] is Token.Constant)
        val ops = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, ops.size)
    }

    @Test
    fun `pi followed by letters throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("pi")
        }
    }

    // ========== Function tokenization ==========

    @Test
    fun `sqrt without parens tokenizes function and number`() {
        // √4 tokenizes as Function("sqrt"), Number("4") even though semantically odd.
        val tokens = Tokenizer.tokenize("\u221A4")
        assertEquals(2, tokens.size)
        assertTrue(tokens[0] is Token.Function)
        assertEquals("sqrt", (tokens[0] as Token.Function).name)
        assertEquals(Token.Number("4"), tokens[1])
    }

    @Test
    fun `sqrt standalone tokenizes`() {
        val tokens = Tokenizer.tokenize("\u221A(")
        assertEquals(2, tokens.size)
        assertTrue(tokens[0] is Token.Function)
        assertTrue(tokens[1] is Token.LeftParen)
    }

    @Test
    fun `double factorial tokenizes as two function tokens`() {
        val tokens = Tokenizer.tokenize("3!!")
        assertEquals(3, tokens.size)
        assertEquals(Token.Number("3"), tokens[0])
        assertTrue(tokens[1] is Token.Function)
        assertTrue(tokens[2] is Token.Function)
        assertEquals("!", (tokens[1] as Token.Function).name)
        assertEquals("!", (tokens[2] as Token.Function).name)
    }

    // ========== Parens ==========

    @Test
    fun `empty parens tokenize as left and right`() {
        val tokens = Tokenizer.tokenize("()")
        assertEquals(2, tokens.size)
        assertTrue(tokens[0] is Token.LeftParen)
        assertTrue(tokens[1] is Token.RightParen)
    }

    @Test
    fun `reversed parens still tokenize with implicit multiply`() {
        // ")(" tokenizes as RightParen, *, LeftParen — the tokenizer inserts
        // an implicit multiply between a closing paren and an opening paren.
        val tokens = Tokenizer.tokenize(")(")
        assertEquals(3, tokens.size)
        assertTrue(tokens[0] is Token.RightParen)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("*", (tokens[1] as Token.Operator).op)
        assertTrue(tokens[2] is Token.LeftParen)
    }

    @Test
    fun `unbalanced extra left paren tokenizes`() {
        val tokens = Tokenizer.tokenize("((1)")
        assertEquals(4, tokens.size)
        assertEquals(2, tokens.count { it is Token.LeftParen })
        assertEquals(1, tokens.count { it is Token.RightParen })
    }

    // ========== Error paths ==========

    @Test
    fun `at-sign throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("@")
        }
    }

    @Test
    fun `hash throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("#")
        }
    }

    @Test
    fun `dollar throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("$")
        }
    }

    @Test
    fun `tilde throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("~")
        }
    }

    @Test
    fun `tab is whitespace and ignored`() {
        val tokens = Tokenizer.tokenize("1\t+\t2")
        assertEquals(3, tokens.size)
    }

    @Test
    fun `newline is whitespace and ignored`() {
        val tokens = Tokenizer.tokenize("1\n+\n2")
        assertEquals(3, tokens.size)
    }

    // ========== Operator precedence values ==========

    @Test
    fun `multiplication has precedence 2`() {
        val tokens = Tokenizer.tokenize("2\u00D73")
        assertEquals(2, (tokens[1] as Token.Operator).precedence)
    }

    @Test
    fun `division has precedence 2`() {
        val tokens = Tokenizer.tokenize("6\u00F72")
        assertEquals(2, (tokens[1] as Token.Operator).precedence)
    }

    @Test
    fun `implicit multiply precedence is 2`() {
        val tokens = Tokenizer.tokenize("2\u03C0")
        assertEquals(2, (tokens[1] as Token.Operator).precedence)
    }

    @Test
    fun `unary minus has precedence 3`() {
        val tokens = Tokenizer.tokenize("-5")
        assertEquals(3, (tokens[0] as Token.UnaryMinus).precedence)
    }
}
