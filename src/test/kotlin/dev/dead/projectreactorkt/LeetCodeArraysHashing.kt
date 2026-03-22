package dev.dead.projectreactorkt

import java.util.*
import kotlin.test.Test

class LeetCodeArraysHashing {
    @Test
    fun testGroupingBy() {
        val s = "aabbccdef"
        val groupingBy = s.groupingBy { it }.eachCount()
        val groupBy = s.groupBy { it }
        groupBy.entries.associate({ it.key to it.value.size }).also { println(it) }
        println("_".repeat(15))
        SolutionJ().twoSum(intArrayOf(1, 2, 3, 4, 5, 6), 7)
    }
}

/**
 * Example:
 * var ti = TreeNode(5)
 * var v = ti.`val`
 * Definition for a binary tree node.
 * class TreeNode(var `val`: Int) {
 *     var left: TreeNode? = null
 *     var right: TreeNode? = null
 * }
 */
class SolutionSt {
    fun isValid(s: String): Boolean {
        val stack = ArrayDeque<Char>()
        val pairs = mapOf(')' to '(', ']' to '[', '}' to '{')

        for (char in s) {
            val matchingOpening = pairs[char]
            if (matchingOpening == null) {
                stack.addLast(char)
            } else {
                if (stack.isEmpty() || stack.removeLast() != matchingOpening) {
                    return false
                }
            }
        }

        return stack.isEmpty()
    }
}

class SolutionStudyIt {
    fun isSameTree(p: TreeNode?, q: TreeNode?): Boolean {
        return getPreOrderIntList(p) == getPreOrderIntList(q)
    }

    private fun getPreOrderIntList(root: TreeNode?): List<Int?> {
        val stack = ArrayDeque<TreeNode?>()
        stack.addLast(root)

        return buildList {
            while (stack.isNotEmpty()) {
                val node = stack.removeLast()

                if (node == null) {
                    add(null)
                    continue
                }

                add(node.value)
                stack.addLast(node.right)
                stack.addLast(node.left)
            }
        }
    }
}

class SolutionJ {
    fun twoSum(nums: IntArray, target: Int): IntArray {
        val indexMap = mutableMapOf<Int, Int>()

        nums.forEachIndexed { index, num ->
            val complement = target - num

            if (indexMap.containsKey(complement)) {
                return intArrayOf(indexMap[complement]!!, index)
            }

            indexMap[num] = index
        }

        return intArrayOf()
    }
}

class SolutionXO {
    fun isAnagram(s: String, t: String): Boolean {
        return s.groupingBy { it }.eachCount() == t.groupingBy { it }.eachCount()


    }
}