package com.example.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import net.objecthunter.exp4j.ExpressionBuilder
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    private lateinit var resultTextView: TextView
    private lateinit var workingsTextView: TextView
    private var currentExpression = StringBuilder()
    private var hasResult = false
    private var lastNumberPercent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.resultTextView)
        workingsTextView = findViewById(R.id.workingsTextView)

        // Number buttons
        for (i in 0..9) {
            val buttonId = resources.getIdentifier("button$i", "id", packageName)
            findViewById<Button>(buttonId).setOnClickListener {
                if (hasResult) {
                    clearCalculator()
                    hasResult = false
                }
                if (lastNumberPercent) {
                    clearCalculator()
                    lastNumberPercent = false
                }
                appendToExpression(i.toString())
            }
        }

        // Operator buttons
        findViewById<Button>(R.id.buttonPlus).setOnClickListener { appendToExpression("+") }
        findViewById<Button>(R.id.buttonMinus).setOnClickListener { appendToExpression("-") }
        findViewById<Button>(R.id.buttonMultiply).setOnClickListener { appendToExpression("×") }
        findViewById<Button>(R.id.buttonDivide).setOnClickListener { appendToExpression("÷") }
        findViewById<Button>(R.id.buttonDot).setOnClickListener {
            if (lastNumberPercent) {
                clearCalculator()
                lastNumberPercent = false
            }
            appendToExpression(".")
        }
        findViewById<Button>(R.id.buttonPercent).setOnClickListener { handlePercent() }
        findViewById<Button>(R.id.buttonDelete).setOnClickListener { deleteLastChar() }

        // Brackets
        findViewById<Button>(R.id.buttonBrackets).setOnClickListener { handleBrackets() }

        // Function buttons
        findViewById<Button>(R.id.buttonClear).setOnClickListener { clearCalculator() }
        findViewById<Button>(R.id.buttonEquals).setOnClickListener { calculateResult(true) }
    }

    private fun handleBrackets() {
        val expression = currentExpression.toString()
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }

        if (openCount == closeCount || expression.isEmpty() ||
            expression.last() in listOf('+', '-', '×', '÷')) {
            appendToExpression("(")
        } else {
            appendToExpression(")")
        }
        calculateResult(false)
    }

    private fun appendToExpression(value: String) {
        // Don't append operators to empty expression except minus
        if (currentExpression.isEmpty() && value in listOf("+", "×", "÷", ")")) {
            return
        }

        // Handle consecutive operators
        if (currentExpression.isNotEmpty()) {
            val last = currentExpression.last().toString()
            if (last in listOf("+", "-", "×", "÷") && value in listOf("+", "×", "÷")) {
                currentExpression.deleteCharAt(currentExpression.length - 1)
            }
        }

        currentExpression.append(value)
        workingsTextView.text = currentExpression.toString()
        calculateResult(false)
    }

    private fun clearCalculator() {
        currentExpression.clear()
        workingsTextView.text = ""
        resultTextView.text = ""
        hasResult = false
    }

    private fun deleteLastChar() {
        if (currentExpression.isNotEmpty()) {
            if (lastNumberPercent) {
                // Remove percentage sign from display
                currentExpression.deleteCharAt(currentExpression.length - 1)
                lastNumberPercent = false
            }
            currentExpression.deleteCharAt(currentExpression.length - 1)
            workingsTextView.text = currentExpression.toString()
            calculateResult(false)
        }
    }

    private fun handlePercent() {
        if (currentExpression.isEmpty()) return

        try {
            // Get the last number from the expression
            val expr = currentExpression.toString()
            val operators = listOf("+", "-", "×", "÷")
            var lastNumber = expr
            var operator = ""
            var firstPart = ""

            for (op in operators) {
                val lastIndex = expr.lastIndexOf(op)
                if (lastIndex != -1) {
                    lastNumber = expr.substring(lastIndex + 1)
                    operator = op
                    firstPart = expr.substring(0, lastIndex)
                    break
                }
            }

            // Add percentage sign to display
            if (!lastNumberPercent) {
                lastNumberPercent = true
                currentExpression.append("%")
                workingsTextView.text = currentExpression.toString()
            }

            // Calculate the actual value
            val percentValue = lastNumber.toDouble() / 100.0
            var calculationExpression = ""

            when {
                operator.isEmpty() -> {
                    calculationExpression = percentValue.toString()
                }
                operator == "+" || operator == "-" -> {
                    val baseNumber = evaluateExpression(firstPart)
                    val percentage = baseNumber * percentValue
                    calculationExpression = "$firstPart$operator$percentage"
                }
                else -> {
                    calculationExpression = "$firstPart$operator$percentValue"
                }
            }

            // Calculate and show result
            val result = ExpressionBuilder(calculationExpression.replace("×", "*").replace("÷", "/"))
                .build()
                .evaluate()

            resultTextView.text = formatResult(result)

        } catch (e: Exception) {
            if (currentExpression.isNotEmpty() && !lastNumberPercent) {
                lastNumberPercent = true
                currentExpression.append("%")
                workingsTextView.text = currentExpression.toString()
            }
        }
    }
    private fun calculateResult(isEquals: Boolean) {
        try {
            if (currentExpression.isEmpty()) {
                resultTextView.text = ""
                return
            }

            // Don't calculate if expression ends with an operator
            if (!isEquals && currentExpression.last() in listOf('+', '-', '×', '÷')) {
                return
            }

            var expression = currentExpression.toString()
                .replace("×", "*")
                .replace("÷", "/")

            // Balance brackets if needed
            val openBrackets = expression.count { it == '(' }
            val closeBrackets = expression.count { it == ')' }
            if (openBrackets > closeBrackets) {
                expression += ")".repeat(openBrackets - closeBrackets)
            }

            val result = ExpressionBuilder(expression)
                .build()
                .evaluate()

            val formattedResult = formatResult(result)

            if (isEquals) {
                hasResult = true
                currentExpression.clear()
                currentExpression.append(formattedResult)
                workingsTextView.text = ""
            }

            resultTextView.text = formattedResult

        } catch (e: Exception) {
            if (isEquals) {
                resultTextView.text = "Error"
            }
        }
    }

    private fun evaluateExpression(expr: String): Double {
        return try {
            ExpressionBuilder(expr.replace("×", "*").replace("÷", "/"))
                .build()
                .evaluate()
        } catch (e: Exception) {
            0.0
        }
    }

    private fun formatResult(number: Double): String {
        val roundedResult = round(number * 1000000) / 1000000
        return if (roundedResult % 1 == 0.0) {
            roundedResult.toLong().toString()
        } else {
            roundedResult.toString()
        }
    }
}