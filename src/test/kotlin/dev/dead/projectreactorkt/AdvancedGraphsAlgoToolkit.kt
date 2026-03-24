package dev.dead.projectreactorkt

import java.util.*

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

// Represents a directed weighted edge from [src] to [dst]
data class Edge(val src: Char, val dst: Char, val weight: Int)

// Represents a graph node identified by [label], with an optional initial distance
// (used by Dijkstra/Prim to track the best-known cost to reach this node)
data class GraphNode(val label: Char, val distance: Int = Int.MAX_VALUE)

class AdvancedGraphsAlgoToolkit {

    // Adjacency list: each node maps to the list of edges going out from it - used for Dijkstra and Prim
    private val adjGraph = mutableMapOf<Char, MutableList<Edge>>()

    // Flat list of all edges (undirected, one entry per pair) — used by Kruskal
    private val allEdges = mutableListOf<Edge>()

    // Populates the graph with the fixed edges shown in the diagram above
    fun setupPracticeGraph() {
        val edges = listOf(
            Triple('A', 'B', 4), Triple('A', 'C', 2), Triple('A', 'D', 1),
            Triple('B', 'D', 5),
            Triple('C', 'D', 8), Triple('C', 'E', 10), Triple('C', 'F', 6),
            Triple('D', 'F', 2),
            Triple('E', 'F', 3)
        )
        edges.forEach { (src, dst, weight) -> addEdge(src, dst, weight) }
    }

    // Adds an undirected edge: registers both directions in adjGraph,
    // and stores the canonical u→v direction in allEdges for Kruskal
    private fun addEdge(src: Char, dst: Char, weight: Int) {
        val edge = Edge(src, dst, weight)

        adjGraph.getOrPut(src) { mutableListOf() }.add(edge)
        adjGraph.getOrPut(dst) { mutableListOf() }.add(Edge(dst, src, weight))

        allEdges.add(edge)
    }

    /** DIJKSTRA: Shortest path from [start] to all reachable nodes.
     *
     *  Strategy: greedy relaxation via a min-heap.
     *  - Each entry in the PriorityQueue is Pair(node, cumulativeDistance).
     *  - Always process the unvisited node with the smallest known distance first.
     *  - When a shorter path to a neighbor is found, push a new entry for it. */
    fun dijkstra(start: Char): Map<Char, Int> {
        // each graph node = node + distance ( cumulative )
        val minHeap = PriorityQueue<GraphNode>(compareBy { it.distance })
        val visited = hashSetOf(start)
        val resultMap = mutableMapOf<Char, Int>().apply { put(start, 0) }
        while (minHeap.isNotEmpty()) {
            val currentNode = minHeap.poll()
            if (visited.contains(currentNode.label)) {
                continue
            }
            visited.add(currentNode.label)
            adjGraph[currentNode.label]?.forEach { edge ->
                val newDistance = edge.weight + currentNode.distance
                if (newDistance < resultMap.getOrDefault(edge.dst, Int.MAX_VALUE)) {
                    resultMap[edge.dst] = newDistance
                    minHeap.add(GraphNode(edge.dst, newDistance))
                }
            }
        }
        return resultMap
    }

    /** PRIM: Minimum Spanning Tree (MST) grown outward from [start].
     *
     *  Strategy: greedily pick the cheapest edge that crosses the cut.
     *  - Maintain a set of visited nodes and a min-heap of candidate edges.
     *  - At each step, pop the cheapest edge; if its destination is unvisited,
     *    add it to the MST and enqueue all edges leaving that new node. */
    fun prim(start: Char) {
        // TODO
    }

    /** KRUSKAL: Minimum Spanning Tree built from globally sorted edges.
     *
     *  Strategy: sort allEdges by weight, then greedily accept edges
     *  that connect two previously disconnected components.
     *  - Use UnionFind to detect and prevent cycles.
     *  - Stop once (nodes.size - 1) edges have been accepted. */
    fun kruskal(nodes: List<Char>) {
        // TODO
    }

    // Union-Find (Disjoint Set Union) with path compression and union by rank.
    // Used by Kruskal to efficiently check whether two nodes share a component.
    class UnionFind(nodes: List<Char>) {

        // parent[i] holds the representative (root) of node i;
        // initialised so each node is its own root
        private val parent = IntArray(nodes.size) { it }

        // rank[i] is an upper-bound on the height of the tree rooted at i;
        // used to keep trees shallow during union
        private val rank = IntArray(nodes.size) { 0 }

        // Stable mapping from node character to its array index
        private val nodeToIndex = nodes.withIndex().associate { it.value to it.index }

        // Returns the root of node i, compressing the path on the way up
        // so future lookups on any node in this chain are O(1)
        fun find(i: Int): Int {
            if (parent[i] == i) return i
            parent[i] = find(parent[i]) // path compression
            return parent[i]
        }

        // Merges the components containing u and v.
        // Returns true if they were in different components (edge is useful),
        // or false if they were already connected (edge would form a cycle).
        fun union(u: Char, v: Char): Boolean {
            val rootU = find(nodeToIndex[u] ?: return false)
            val rootV = find(nodeToIndex[v] ?: return false)
            if (rootU == rootV) return false // already in the same component

            // Attach the shorter tree under the taller one to preserve balance
            when {
                rank[rootU] < rank[rootV] -> parent[rootU] = rootV
                rank[rootU] > rank[rootV] -> parent[rootV] = rootU
                else -> {
                    parent[rootU] = rootV; rank[rootV]++
                } // equal height: bump rank
            }
            return true
        }
    }
}

// Runs Dijkstra from 'A' and prints shortest distances to all reachable nodes
fun testDijkstra() {
    val toolkit = AdvancedGraphsAlgoToolkit()
    toolkit.setupPracticeGraph()

    val result = toolkit.dijkstra('A')

    println("Shortest distances from A:")
    result.entries.sortedBy { it.key }.forEach { (node, dist) ->
        println("  A -> $node : $dist")
    }
}

/*  Expected output:
    Shortest distances from A:
      A -> A : 0
      A -> B : 4
      A -> C : 2
      A -> D : 1
      A -> E : 12
      A -> F : 3
*/

fun main() {
    testDijkstra()
}
