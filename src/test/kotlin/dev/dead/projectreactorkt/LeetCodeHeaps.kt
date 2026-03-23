package dev.dead.projectreactorkt

import java.util.*

class LeetCodeHeaps {
}

class SolutionCleaned {
    fun lastStoneWeight(stones: IntArray): Int {
        // Use a Max-Heap by reversing the natural order
        val maxHeap = PriorityQueue<Int>(compareByDescending { it }).apply {
            addAll(stones.toList())
        }

        while (maxHeap.size > 1) {
            // Directly pull the two heaviest stones
            val first = maxHeap.poll()
            val second = maxHeap.poll()

            // Only add back if there is a remainder (weight > 0)
            val remainder = first - second
            if (remainder > 0) {
                maxHeap.add(remainder)
            }
        }

        // Return the last stone, or 0 if none remain
        return maxHeap.poll() ?: 0
    }
}

class SolutionQ {
    fun lastStoneWeight(stones: IntArray): Int {
        val maxHeap = PriorityQueue<Int>(compareByDescending { it })
        stones.forEach { stone -> maxHeap.add(stone) }
        while (maxHeap.size > 1) {
            val (first, second) = List(2) { maxHeap.poll() }
            val reminder = first - second
            if (reminder.equals(0)) {
                continue
            }
            maxHeap.add(reminder)

        }
        return maxHeap.poll() ?: 0

    }
}