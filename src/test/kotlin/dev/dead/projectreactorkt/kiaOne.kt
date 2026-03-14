package dev.dead.projectreactorkt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.exists
import kotlin.io.path.useLines
import kotlin.test.Test
import kotlin.test.assertTrue

class kiaOne {
    @Test
    fun testTrueDemo() {
        assertTrue(true)
    }

    @Test
    fun testWhen() {
        whenOnInt(0, 1).forEach { print("$it ") }

    }

    fun whenOnInt(a: Int, b: Int): String {
        return when (setOf(a, b)) {
            setOf(1, 2) -> "one and two"
            setOf(0, 1) -> "binary"
            else -> "default"
        }
    }

    sealed interface Expr

    class Num(val value: Int) : Expr
    class Sum(val left: Expr, val right: Expr) : Expr

    fun eval(e: Expr): Int = when (e) {
        is Num -> e.value
        is Sum -> eval(e.left) + eval(e.right)
    }

    @Test
    fun testEval() {
        // Result: (1 + 2) + 4 = 7
        println(eval(Sum(Sum(Num(1), Num(2)), Num(4))))
        println(evalWithLogging(Sum(Sum(Num(1), Num(2)), Num(4))))
    }

    @Test
    fun nestedWithLabels() {
        outer@ for (i in 1..10) {
            // 1 -> 10
            inner@ while (i < 5) {
                println("inner loop $i")
                if (i == 4) {
                    println("breaking loop $i")
                    break@inner
                } else {
                    continue@outer
                }

            }
        }
    }

    fun evalWithLogging(e: Expr): Int =
        when (e) {
            is Num -> {
                println("num: ${e.value}")
                e.value
            }

            is Sum -> {
                val left = evalWithLogging(e.left)
                val right = evalWithLogging(e.right)
                println("sum: $left + $right")
                left + right
            }

        }

    @Test
    fun FizzBuzzWhenNoArguments() {
        (1..100).forEach {
            when {
                it % 15 == 0 -> println("FizzBuzz : $it")
                it % 3 == 0 -> println("Fizz  : $it")
                it % 5 == 0 -> println("Buzz : $it")
            }
        }
    }

    @Test
    fun rangesDemo() {
        for (i in 100 downTo 1 step 5) {
            print("$i ")
        }
    }

    @Test
    fun binaryMaps() {
        val binaryReps = mutableMapOf<Char, String>()
        for (char in 'A'..'Z') {

            val binary = char.code.toString(radix = 2)
            println(char.code.toString(radix = 16))
            binaryReps[char] = binary
        }
        for ((letter, binary) in binaryReps) {

            println("$letter = $binary")
        }
        // A = 1000001 D = 1000100
        // B = 1000010 E = 1000101
        // C = 1000011 F = 1000110
        // (output split into columns for conciseness)
    }

    @Test
    fun testConverter() {
        println(radixDecToHex(Short.MAX_VALUE.toInt()))
    }

    fun radixDecToHex(dec: Int): String =
        Integer.toHexString(dec)


    @Test
    fun oneToNDummyAlgo() = runBlocking {
        val path = Path.of("./test.txt")

        // 1. Efficient Writing (using Use for auto-closing)
        withContext(Dispatchers.IO) {
            path.bufferedWriter().use { writer ->
                (1..100).forEach { n ->
                    writer.write("Line $n\n")
                }
            }
        }

        // 2. Modern Idiomatic Reading (using Extension Functions)
        val content = withContext(Dispatchers.IO) {
            if (path.exists()) {
                // readLines() is great for small/medium files
                // useLines() is better for massive files (streaming)
                path.useLines { lines ->
                    lines.joinToString(separator = ", ")
                }
            } else {
                "File not found"
            }
        }

        println(content)
    }
}