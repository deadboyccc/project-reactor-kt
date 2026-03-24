package dev.dead.projectreactorkt

class DSAToolkit {

}

fun main() {
    val list = listOf(1, 2, 3, 4, 5)

    list
        .binarySearch(4).also { println(it) }

    list
        .bns(4).also { println(it) }


}

fun List<Int>.bns(target: Int): Int? {
    val sortedList = this.sorted()

    var L = 0
    var R = sortedList.size - 1

    while (L <= R) {
        val mid = (L + R) / 2

        when {
            sortedList[mid] == target -> return mid
            target < sortedList[mid] -> R = mid - 1
            target > sortedList[mid] -> L = mid + 1
        }
    }
    return null
}

class Node(var value: Int, var left: Node? = null, var right: Node? = null)

// --- BST OPERATIONS ---

/** Search: Returns the node if found, else null. Time: O(h) */
fun Node?.search(target: Int): Node? = when {
    this == null || value == target -> this
    target < value -> left.search(target)
    else -> right.search(target)
}

/** Insert: Adds value and returns the updated tree. Time: O(h) */
fun Node?.insert(target: Int): Node {
    if (this == null) return Node(target)
    if (target < value) left = left.insert(target) else right = right.insert(target)
    return this
}

// --- TRAVERSALS ---

/** DFS (In-Order): Left -> Root -> Right. Visits nodes in sorted order. */
fun Node?.dfs(process: (Int) -> Unit) {
    this ?: return
    left.dfs(process)
    process(value)
    right.dfs(process)
}

/** BFS (Level-Order): Uses a Queue to visit layer by layer. */
fun Node?.bfs() {
    val queue = ArrayDeque<Node>().apply { this@bfs?.let { add(it) } }

    while (queue.isNotEmpty()) {
        repeat(queue.size) {
            val current = queue.removeFirst()
            print("${current.value} ") // Process

            current.left?.let { queue.add(it) }
            current.right?.let { queue.add(it) }
        }
    }
}