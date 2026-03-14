package dev.dead.projectreactorkt

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
}