package dev.dead.projectreactorkt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.io.path.*
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
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

    fun evalWithLogging(e: Expr): Int = when (e) {
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

    fun radixDecToHex(dec: Int): String = Integer.toHexString(dec)


    @Test
    fun oneToNDummyAlgo() = runBlocking {
        val path = Path.of("./test1.txt")

        // 1. Efficient Writing (using Use for auto-closing)
        withContext(Dispatchers.IO) {
            path.bufferedWriter().use { writer ->
                (1..151).forEach { n ->
                    writer.write("$n")
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

    @Test
    fun oneToNDummyAlgoReadOnly() = runBlocking {
        val path = Path.of("./test.txt")

        // 1. Efficient Writing (using Use for auto-closing)
//        withContext(Dispatchers.IO) {
//            path.bufferedWriter().use { writer ->
//                (1..100).forEach { n ->
//                    writer.write("$n")
//                }
//            }
//        }

        // 2. Modern Idiomatic Reading (using Extension Functions)
        val content = withContext(Dispatchers.IO) {
            if (path.exists()) {
                // on path use >
                // readLines() is great for small/medium files
                // useLines() is better for massive files (streaming)
                val result = path.readText().trim() // .trim() handles trailing newlines from writes

                val expected = (1..151).joinToString(separator = "")

                val isSame = result == expected

                // Or use compareTo if you specifically need the integer delta
                val comparisonInt = result.compareTo(expected)

                println("File Content: $result")
                println("Expected:     $expected")
                println("Are they equal? $isSame")
                println("Comparison Int: $comparisonInt")


            } else {
                "File not found"
            }
        }

        println(content)
    }

    @Test
    fun oneToNDummyAlgoReadAssert() = runBlocking {

        val path = Path.of("./test.txt")
        val result = path.readText().trim()
        val expected = (1..151).joinToString("")


        // This will give you a "Click to show difference" link in IntelliJ
//        assertEquals(expected, result, "The file content should match the sequence 1..151")
        val isCorrect = (1..151).asSequence().map { it.toString() }.joinToString("") == result
        println("isCorrect: $isCorrect")
    }

    @Test
    fun oneToNDummyAlgoReadAssertCleanedAndOptimizedJustInTimeComparison() = runBlocking(Dispatchers.IO) {
        val path = Path.of("./test1.txt")

        // 1. Generate the expected sequence as a sequence/stream of characters
        val expectedSequence = (1..151).asSequence().flatMap { it.toString().asSequence() }

        // 2. Stream the file content and compare lazily
        val isCorrect = path.bufferedReader().use { reader ->
            expectedSequence.all { expectedChar ->
                reader.read().toChar() == expectedChar
            } && reader.read() == -1 // Ensure no extra trailing data
        }

        println("isCorrect: $isCorrect")
    }

    @Test
    fun oneToNDummyAlgoReadAssertFP(): Unit = runBlocking {
        val path = Path.of("./test.txt")

        val expected = (1..151).joinToString(separator = "")
        val actual = path.readText().trim()

        (actual == expected).also { success ->
            println("Match Status: ${if (success) "✅ Correct" else "❌ Mismatch"}")
        }
    }


    @Test
    fun graphTraversingCommonPastCoWorkers() {
        // 1. Initialize the Graph (Adjacency List)
        // A Map where the Key is a Worker, and the Value is a Set of their past co-workers.
        val graph = mutableMapOf<String, MutableSet<String>>()

        // Helper function to add a bi-directional edge (since co-working goes both ways)
        fun addCoWorker(w1: String, w2: String) {
            graph.computeIfAbsent(w1) { mutableSetOf() }.add(w2)
            graph.computeIfAbsent(w2) { mutableSetOf() }.add(w1)
        }

        // --- 2. Build the Data ---
        // We'll create a moderately sized dataset with a mix of successful groups and noise.

        // Group A: Alice, Bob, Charlie (All worked together)
        addCoWorker("Alice", "Bob")
        addCoWorker("Bob", "Charlie")
        addCoWorker("Charlie", "Alice")

        // Group B: Charlie, Dave, Eve (All worked together)
        addCoWorker("Charlie", "Dave")
        addCoWorker("Dave", "Eve")
        addCoWorker("Eve", "Charlie")

        // Overlapping Group C: Bob, Charlie, Dave
        // Bob and Charlie already worked together, Charlie and Dave already worked together.
        // We just add a link between Bob and Dave to complete a new 3-person group.
        addCoWorker("Bob", "Dave")

        // Noise (These workers don't form a complete 3-person group with anyone)
        addCoWorker("Alice", "Frank")
        addCoWorker("Frank", "Grace")
        addCoWorker("Grace", "Eve")

        println("=== Initial Graph: Past Co-worker Relationships ===")
        graph.forEach { (worker, coWorkers) ->
            println("$worker has worked with: $coWorkers")
        }
        println("\n=== Starting Algorithm: Search for Optimal Groups of 3 ===")

        // --- 3. The Algorithm ---
        val idealGroups = mutableListOf<Set<String>>()

        // Convert to list so we can iterate by index. This prevents finding duplicates.
        val workersList = graph.keys.toList()

        // Loop through pairs of workers.
        // Using indices (i < j) ensures we check [Alice, Bob] but ignore [Bob, Alice]
        for (i in 0 until workersList.size) {
            val worker1 = workersList[i]

            for (j in i + 1 until workersList.size) {
                val worker2 = workersList[j]

                // Step A: Check if worker1 and worker2 have actually worked together
                if (graph[worker1]?.contains(worker2) == true) {
                    println("Evaluating connected pair: [$worker1, $worker2]")

                    // Step B: Find common past co-workers between them.
                    // This is the efficiency trick: Intersecting their sets is very fast.
                    val commonCoWorkers = graph[worker1]!!.intersect(graph[worker2]!!)

                    // Step C: Any common co-worker forms a perfect group of 3!
                    for (worker3 in commonCoWorkers) {

                        // To prevent duplicate groups (e.g., logging {Alice, Bob, Charlie}
                        // and later {Bob, Charlie, Alice}), we ensure worker3's index
                        // comes AFTER worker2 in our master list.
                        if (workersList.indexOf(worker3) > j) {
                            val group = setOf(worker1, worker2, worker3)
                            idealGroups.add(group)
                            println("  -> Success! Found 3rd member '$worker3'. Formed group: $group")
                        }
                    }
                }
            }
        }

        // --- 4. Results Output and Validation ---
        println("\n=== Final Results ===")
        println("Total optimal groups found: ${idealGroups.size}")
        idealGroups.forEachIndexed { index, group ->
            println("Group ${index + 1}: $group")
        }

        // We explicitly built the data to have exactly 3 complete triangles.
        assertEquals(3, idealGroups.size, "The algorithm should have found exactly 3 groups.")
    }

    @Test
    fun `verify file content matches sequence with diff`() = runBlocking(Dispatchers.IO) {
        val file = Path.of("./test1.txt")
        val expectedChars = (1..151).joinToString("").asSequence()

        val result = file.bufferedReader().use { reader ->
            var mismatch: String? = null

            expectedChars.forEachIndexed { i, expected ->
                val actualInt = reader.read()
                val actual = if (actualInt == -1) "EOF" else actualInt.toChar().toString()

                if (actual != expected.toString()) {
                    mismatch = "Mismatch at index $i: expected '$expected' but got '$actual'"
                    return@use mismatch
                }
            }

            if (reader.read() != -1) "File has trailing data" else null
        }

        if (result == null) println("Success") else println("Failure: $result")
    }

    //    @file:JvmName("Test")
    @JvmOverloads
    fun <T> joinToString(
        collection: Collection<T>,
        separator: String = ",",
        prefix: String = "",
        postfix: String = "",
    ): String {
        val result = StringBuilder(prefix)
        for ((index, element) in collection.withIndex()) {
            if (index > 0) result.append(separator)

            result.append(element)
        }
        result.append(postfix)
        return result.toString()
    }

    //p 122
    @Test
    fun testStringBuilder() {
        with(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)) {
            println(joinToString(" | ", "(", ")"))
        }

    }


    @Test
    fun testStreamsExtensions() {
        // 1. Generate, parallelize, and transform
        val list = IntStream.range(1, 1_000_000).parallel()
            .mapToObj { it to it.toString(16) } // mapToObj creates the Pair<Int, String>
            .toList() // Kotlin extension to collect the stream efficiently

        // 2. Verify a couple of values
        assertEquals(999_999, list.size)
        assertEquals(255 to "ff", list[254])

        println("Processed ${list.size} elements. Example: ${list.last()}")
    }


    @Test
    fun testStreamToMap() {
        val maxRange = 1_000_000

        // 1. Generate and map to a Map structure
        val hexMap: Map<Int, String> = IntStream.range(1, maxRange).parallel().mapToObj { it to it.toString(16) }
            // We use the Java Collector; 'it.first' is the key, 'it.second' is the value
            .collect(Collectors.toMap({ it.first }, { it.second }))

        // 2. Assertions
        assertEquals(maxRange - 1, hexMap.size)
        assertEquals("ff", hexMap[255])
        assertEquals("1e240", hexMap[123456])

        assertTrue(hexMap.containsKey(999_999))
    }

    @Test
    fun testStreamsExtensionsCleaned() {
        val list = IntStream.range(1, 1_000_000).parallel().mapToObj { it to it.toString(16) }.toList()

        assertEquals(999_999, list.size)
        assertEquals(255 to "ff", list[254])
    }

    @Test
    fun testStreamToMapCleaned() {
        val hexMap = IntStream.range(1, 1_000_000).parallel().mapToObj { it to it.toString(16) }.toList().toMap()

        assertEquals(999_999, hexMap.size)
        assertEquals("ff", hexMap[255])
    }

    @Test
    fun compareParallelPerformance() {
        val range = 20_000_000

        // Measure Sequential
        val seqTime = measureTimeMillis {
            IntStream.range(1, range).sum()
        }

        // Measure Parallel
        val parTime = measureTimeMillis {
            IntStream.range(1, 1_000_000).parallel().sum()
        }

        println("Sequential: ${seqTime}ms | Parallel: ${parTime}ms")
        assertTrue(parTime < seqTime, "Parallel should be faster for large CPU-bound tasks")
    }

    open class View {
        open fun click() = println("View clicked")
    }

    class Button : View() {
        override fun click() = println("Button clicked")
    }

    @Test
    fun testOOPStuff() {
        val b = Button()
        b.click()
        val a: View = Button()
        a.click()

    }

    @Test
    fun testDestructuringVarArgs() {
        // 1. Define as an IntArray or convert the list to an array
        val intArray = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        // 2. Now the spread operator (*) will work
        val secondArray = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, *intArray.toTypedArray())

        // 3.
        printInts(1, 2, 3, *intArray)

    }

    fun printInts(vararg ints: Int) {
        for (int in ints) {
            println(int)
        }
    }

    @Test
    fun revString() {

    }

    @Test
    fun halvesEqual() {
        val solutionTwo = SolutionTwo()
        solutionTwo.halvesAreAlike("textbook").also { println(it) }
    }
}

val String.lastChar: Char
    get() = this.get(length - 1)

class Solution {
    fun reverseString(s: CharArray): Unit {
        var left = 0
        var right = s.size - 1

        while (left < right) {
            val temp = s[left]
            s[left] = s[right]
            s[right] = temp

            left++
            right--
        }
    }
}

class SolutionTwo {
    fun halvesAreAlike(s: String): Boolean {
        val vowels = setOf('a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U')

        val mid = s.length / 2
        val a1 = s.substring(0, mid)
        val a2 = s.substring(mid)

        val count1 = a1.count { it in vowels }
        val count2 = a2.count { it in vowels }

        return count1 == count2
    }
}

class Solution3 {
    /**
     * "../" : Move to the parent folder. (If in main, remain in main).
     * "./"  : Remain in the same folder.
     * "x/"  : Move to the child folder named x.
     */
    fun minOperations(logs: Array<String>): Int {
        var depth = 0

        logs.forEach { log ->
            when (log) {
                "../" -> if (depth > 0) depth--
                "./" -> { /* Do nothing */
                }

                else -> depth++
            }
        }

        return depth
    }
}

class Solution4 {
    fun reversePrefix(word: String, ch: Char): String {
        val targetIndex = word.indexOf(ch)

        if (targetIndex == -1) return word

        return buildString {
            append(word.substring(0, targetIndex + 1).reversed())
            append(word.substring(targetIndex + 1))
        }
    }
}


interface INestedInteger {
    fun isInteger(): Boolean
    fun getInteger(): Int?
    fun getList(): List<INestedInteger>?
}
//
/**
 * // This is the interface that allows for creating nested lists.
 * // You should not implement it, or speculate about its implementation
 * class NestedInteger {
 *     // Constructor initializes an empty nested list.
 *     constructor()
 *
 *     // Constructor initializes a single integer.
 *     constructor(value: Int)
 *
 *     // @return true if this NestedInteger holds a single integer, rather than a nested list.
 *     fun isInteger(): Boolean
 *
 *     // @return the single integer that this NestedInteger holds, if it holds a single integer
 *     // Return null if this NestedInteger holds a nested list
 *     fun getInteger(): Int?
 *
 *     // Set this NestedInteger to hold a single integer.
 *     fun setInteger(value: Int): Unit
 *
 *     // Set this NestedInteger to hold a nested list and adds a nested integer to it.
 *     fun add(ni: NestedInteger): Unit
 *
 *     // @return the nested list that this NestedInteger holds, if it holds a nested list
 *     // Return null if this NestedInteger holds a single integer
 *     fun getList(): List<NestedInteger>?
 * }
 */

class NestedIterator(nestedList: List<NestedInteger>) {
    private val stack: Deque<NestedInteger> = java.util.ArrayDeque()
    fun next(): Int {
        return 1;


    }

    fun hasNext(): Boolean {
        return true;


    }
}

/**
 * Your NestedIterator object will be instantiated and called as such:
 * var obj = NestedIterator(nestedList)
 * var param_1 = obj.next()
 * var param_2 = obj.hasNext()
 */
/**
 * // This is the interface that allows for creating nested lists.
 * // You should not implement it, or speculate about its implementation
 */
/**
 * Interface for a data structure that can represent either a single integer
 * or a nested list of integers.
 */
interface NestedInteger {

    /**
     * @return true if this NestedInteger holds a single integer, rather than a nested list.
     */
    fun isInteger(): Boolean

    /**
     * @return the single integer that this NestedInteger holds, if it holds a single integer.
     * Return null if this NestedInteger holds a nested list.
     */
    fun getInteger(): Int?

    /**
     * Set this NestedInteger to hold a single integer.
     */
    fun setInteger(value: Int)

    /**
     * Adds a nested integer to this NestedInteger (converts it to a list if it wasn't one).
     */
    fun add(ni: NestedInteger)

    /**
     * @return the nested list that this NestedInteger holds, if it holds a nested list.
     * Return null if this NestedInteger holds a single integer.
     */
    fun getList(): List<NestedInteger>?
}
