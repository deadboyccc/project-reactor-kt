package dev.dead.projectreactorkt

import java.util.*
import kotlin.math.max
import kotlin.test.Test


class LeetCode {
    @Test
    fun testMax() {
        maxOf(10, 20).also { println(it) }
    }
    // reduce, fold, scan, windowed  - check


    // map methods getOrPut getOrDefault etc <String, List<String>> - check

    // list and arrays and arrayDeq - check
    @Test
    fun runTreeTest() {
        // Creating a medium-sized BST
        //         50
        //       /    \
        //     30      70
        //    /  \    /  \
        //   20  40  60  80

        val root = TreeNode(50).apply {
            left = TreeNode(30).apply {
                left = TreeNode(20)
                right = TreeNode(40)
            }
            right = TreeNode(70).apply {
                left = TreeNode(60)
                right = TreeNode(80)
            }
        }
        depthFirstSearchStackBased(root)

        println("Pre-order: ")
        printPreOrder(root) // 50 30 20 40 70 60 80

        println("\nIn-order (Sorted):")
        printInOrder(root)  // 20 30 40 50 60 70 80

        println("\nPost-order:")
        printPostOrder(root) // 20 40 30 60 80 70 50
        println()
    }

}


fun getLevelOrderListCleaned(root: TreeNode?): List<Int> {
    val startNode = root ?: return emptyList()
    val queue = ArrayDeque<TreeNode>()
    queue.addLast(startNode) // Join the back of the line

    return buildList {
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst() // Leave from the front of the line
            add(node.value)

            // Add children to the back of the line
            node.left?.let { queue.addLast(it) }
            node.right?.let { queue.addLast(it) }
        }
    }
}

fun getPreOrderListCleaned(root: TreeNode?): List<Int> {
    val startNode = root ?: return emptyList()
    val stack = ArrayDeque<TreeNode>()
    stack.addLast(startNode) // Push to the top

    return buildList {
        while (stack.isNotEmpty()) {
            val node = stack.removeLast() // Pop from the top
            add(node.value)

            // Right then Left, so Left is on "top" (at the end)
            node.right?.let { stack.addLast(it) }
            node.left?.let { stack.addLast(it) }
        }
    }
}
fun depthFirstSearchStackBased(root: TreeNode?) {
    val arr = mutableListOf<Int>()
    val stack = Stack<TreeNode>()
    stack.push(root)
    while (!stack.empty()) {
        val node = stack.pop()


        arr.add(node.value)

        node.right?.let { stack.push(it) }
        node.left?.let { stack.push(it) }


    }
    arr.joinToString(",", prefix = "[", postfix = "]") { it.toString() }.also { println(it) }

}

fun getPreOrderList(root: TreeNode?): List<Int> {
    val startNode = root ?: return emptyList()

    // Using ArrayDeque as a Stack (LIFO)
    val stack = ArrayDeque<TreeNode>().apply { push(startNode) }

    return buildList {
        while (stack.isNotEmpty()) {
            val node = stack.pop()
            add(node.value)

            // Push Right then Left so Left is processed first
            node.right?.let(stack::push)
            node.left?.let(stack::push)
        }
    }
}

fun getLevelOrderList(root: TreeNode?): List<Int> {
    val startNode = root ?: return emptyList()

    // Using ArrayDeque as a Queue (FIFO)
    val queue = ArrayDeque<TreeNode>().apply { add(startNode) }


    return buildList {
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst() // Remove from the front
            add(node.value)

            // Add children in natural order (Left then Right)
            node.left?.let(queue::add)
            node.right?.let(queue::add)
        }
    }
}

class TreeNode(var value: Int) {
    var left: TreeNode? = null
    var right: TreeNode? = null
}

// Root -> Left -> Right
fun printPreOrder(root: TreeNode?) {
    root ?: return
    print("${root.value} ")
    printPreOrder(root.left)
    printPreOrder(root.right)
}

// Left -> Root -> Right
fun printInOrder(root: TreeNode?) {
    root ?: return
    printInOrder(root.left)
    print("${root.value} ")
    printInOrder(root.right)
}

// Left -> Right -> Root
fun printPostOrder(root: TreeNode?) {
    root ?: return
    printPostOrder(root.left)
    printPostOrder(root.right)
    print("${root.value} ")
}

class SolutionF {
    fun maxDepth(root: TreeNode?): Int {
        if (root == null) return 0

        var depth = 0
        val queue = ArrayDeque<TreeNode>()
        queue.addLast(root)

        while (queue.isNotEmpty()) {
            val levelSize = queue.size

            depth++

            for (i in 0 until levelSize) {
                val node = queue.removeFirst()

                node.left?.let { queue.addLast(it) }
                node.right?.let { queue.addLast(it) }
            }
        }

        return depth
    }
}

class SolutionLLOL {
    fun maxDepth(root: TreeNode?): Int {
        if (root == null) return 0

        return 1 + max(maxDepth(root.left), maxDepth(root.right))
    }
}