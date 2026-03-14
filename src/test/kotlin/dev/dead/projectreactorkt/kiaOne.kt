package dev.dead.projectreactorkt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.*
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
                (1..150).forEach { n ->
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
        val isCorrect = (1..151).asSequence()
            .map { it.toString() }
            .joinToString("") == result
        println("isCorrect: $isCorrect")
    }

    @Test
    fun oneToNDummyAlgoReadAssertCleanedAndOptimizedJustInTimeComparison() = runBlocking(Dispatchers.IO) {
        val path = Path.of("./test1.txt")

        // 1. Generate the expected sequence as a sequence/stream of characters
        val expectedSequence = (1..151).asSequence()
            .flatMap { it.toString().asSequence() }

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
}


