package dev.dead.projectreactorkt

import kotlin.test.Test


class KiaOnePartTwo {
    @Test
    fun testRange() {
        (0..<6).forEach { print("$it ") }
        println()
    }

    @Test
    fun testTest() {
        val solution88 = Solution88()
        solution88.longestConsecutive(intArrayOf(100, 4, 200, 1, 3, 2))
    }

}

class Solution9 {
    fun containsDuplicate(nums: IntArray): Boolean {
        if (nums.size < 2) return false
        if (nums.distinct().count() == nums.size) return false


        return true
    }
}

class Solution88 {
    fun longestConsecutive(nums: IntArray): Int {
        if (nums.isEmpty()) return 0

        // 1. Sort the array in place
        nums.sort()

        var maxStreak = 1
        var currentStreak = 1

        // 2. Use 'indices' and 'drop' to avoid index-out-of-bounds
        // We look at each element starting from the second one (index 1)
        for (i in 1 until nums.size) {
            val current = nums[i]
            val previous = nums[i - 1]

            // Skip duplicates (don't break the streak, just ignore them)
            if (current == previous) continue

            // Check if they are consecutive
            if (current == previous + 1) {
                currentStreak++
            } else {
                // Sequence broken, reset to 1
                currentStreak = 1
            }

            maxOf(maxStreak, currentStreak).also { maxStreak = it }
        }

        return maxStreak
    }
}

class Solution888 {
    fun longestConsecutive(nums: IntArray): Int {
        // size 0->1
        if (nums.size < 2) return nums.size

        // sort it
        nums.sort()

        // keep a counter [ increment , reset]
        var counter = 0

        (0..<nums.size).forEach {
            if (nums[it] == nums[it + 1] + 1) {
                counter++
            } else counter = 0
        }


        return counter

    }
}

class Solution8888 {
    fun longestConsecutive(nums: IntArray): Int {
        if (nums.isEmpty()) return 0

        val numSet = nums.toHashSet()
        var maxStreak = 0

        for (num in numSet) {
            if (!numSet.contains(num - 1)) {
                var currentNum = num
                var currentStreak = 1

                while (numSet.contains(currentNum + 1)) {
                    currentNum++
                    currentStreak++
                }

                maxStreak = maxOf(maxStreak, currentStreak)
            }
        }

        return maxStreak
    }
}

fun longestConsecutive(nums: IntArray): Int {
    val numSet = nums.toHashSet() // O(n) space and time
    var maxStreak = 0

    for (num in numSet) {
        // Step 1: Check if this is the start of a sequence
        if (num - 1 !in numSet) {
            var currentNum = num
            var currentStreak = 1

            // Step 2: Build the sequence upward
            while (currentNum + 1 in numSet) {
                currentNum++
                currentStreak++
            }

            // Step 3: Update the global record
            maxStreak = maxOf(maxStreak, currentStreak)
        }
    }
    return maxStreak
}

class Solution1 {
    fun groupAnagrams(strs: Array<String>): List<List<String>> {
        val map = mutableMapOf<String, MutableList<String>>()

        for (str in strs) {
            val sorted = str.toCharArray().sortedArray().joinToString("")
            // getOrPut returns the list, then we add the string to it
            map.getOrPut(sorted) { mutableListOf() }.add(str)
        }

        // Convert the map values (collections of lists) into the final result format
        return map.values.toList().also { println(it) }
    }
}

class Solution22 {
    fun productExceptSelf(nums: IntArray): IntArray {
        val n = nums.size
        val result = IntArray(n)

        var zeroCount = 0
        var totalProductWithoutZeros = 1

        for (num in nums) {
            if (num == 0) zeroCount++
            else totalProductWithoutZeros *= num
        }

        for (i in nums.indices) {
            if (zeroCount > 1) {
                result[i] = 0
            } else if (zeroCount == 1) {
                result[i] = if (nums[i] == 0) totalProductWithoutZeros else 0
            } else {
                result[i] = totalProductWithoutZeros / nums[i]
            }
        }

        return result
    }
}

fun main() {
    Solution1().groupAnagrams(arrayOf("eat", "tea", "tan", "ate", "nat", "bat"))

}