package dev.dead.projectreactorkt

/*
    GRAPH VISUALIZATION (Precision Diagonals):

    (A) ----------- 4 ----------- (B)
     | \                           |
     |  \ 1                        |
     2   \                         5
     |    \                        |
     |     \                       |
    (C) ----------- 8 ----------- (D)
     | \                           |
     |  \ 6                        |
    10   \                         2
     |    \                        |
     |     \                       |
    (E) ----------- 3 ----------- (F)

    CONNECTIONS:
    Horizontal: A-B(4), C-D(8), E-F(3)
    Vertical:   A-C(2), B-D(5), C-E(10), D-F(2)
    Diagonal:   A-D(1), C-F(6)
*/

data class Edge(val src: Char, val dst: Char, val weight: Int)

class AdvancedGraphsAlgoToolkit {

    private val adjGraph = mutableMapOf<Char, MutableList<Edge>>()
    private val allEdges = mutableListOf<Edge>()

    fun setupPracticeGraph() {
        val edges = listOf(
            Triple('A', 'B', 4), Triple('A', 'C', 2), Triple('A', 'D', 1),
            Triple('B', 'D', 5),
            Triple('C', 'D', 8), Triple('C', 'E', 10), Triple('C', 'F', 6),
            Triple('D', 'F', 2),
            Triple('E', 'F', 3)
        )
        edges.forEach { (u, v, w) -> addEdge(u, v, w) }
    }

    private fun addEdge(u: Char, v: Char, weight: Int) {
        val edge = Edge(u, v, weight)
        adjGraph.getOrPut(u) { mutableListOf() }.add(edge)
        adjGraph.getOrPut(v) { mutableListOf() }.add(Edge(v, u, weight))
        allEdges.add(edge)
    }

    /** DIJKSTRA: Shortest path from [start].
     *  Tip: PriorityQueue stores Pair(Node, CumulativeDistance). */
    fun dijkstra(start: Char) {
        // TODO
    }

    /** PRIM: Minimum Spanning Tree from [start].
     *  Tip: PriorityQueue stores Edge comparing individual weights. */
    fun prim(start: Char) {
        // TODO
    }

    /** KRUSKAL: Minimum Spanning Tree using sorted global edges.
     *  Tip: Use the UnionFind class below. */
    fun kruskal(nodes: List<Char>) {
        // TODO
    }

    class UnionFind(nodes: List<Char>) {

        private val parent = IntArray(nodes.size) { it }
        private val rank = IntArray(nodes.size) { 0 }
        private val nodeToIndex = nodes.withIndex().associate { it.value to it.index }

        fun find(i: Int): Int {
            if (parent[i] == i) return i
            parent[i] = find(parent[i])
            return parent[i]
        }

        fun union(u: Char, v: Char): Boolean {
            val rootU = find(nodeToIndex[u] ?: return false)
            val rootV = find(nodeToIndex[v] ?: return false)
            if (rootU == rootV) return false
            when {
                rank[rootU] < rank[rootV] -> parent[rootU] = rootV
                rank[rootU] > rank[rootV] -> parent[rootV] = rootU
                else -> {
                    parent[rootU] = rootV; rank[rootV]++
                }
            }
            return true
        }
    }
}
