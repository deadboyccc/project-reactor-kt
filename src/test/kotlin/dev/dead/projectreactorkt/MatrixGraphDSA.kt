package dev.dead.projectreactorkt

class MatrixGraphDSA {

    val adjList = mutableMapOf<Int, MutableList<Int>>()

    /** Depth-First Search: Recursive traversal */
    fun matrixDFS(grid: Array<IntArray>, r: Int, c: Int, visited: MutableSet<Pair<Int, Int>>) {
        if (
            r !in grid.indices ||       // Out of vertical bounds
            c !in grid[0].indices ||    // Out of horizontal bounds
            grid[r][c] == 1 ||          // Hits a wall
            (r to c) in visited         // Already processed
        ) return

        visited.add(r to c)

        matrixDFS(grid, r + 1, c, visited)
        matrixDFS(grid, r - 1, c, visited)
        matrixDFS(grid, r, c + 1, visited)
        matrixDFS(grid, r, c - 1, visited)
    }

    /** Breadth-First Search: Iterative traversal */
    fun matrixBFS(grid: Array<IntArray>, startR: Int, startC: Int) {
        if (grid.isEmpty()) return

        val queue = ArrayDeque<Pair<Int, Int>>().apply { add(startR to startC) }
        val visited = mutableSetOf<Pair<Int, Int>>().apply { add(startR to startC) }
        val directions = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))

        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()

            for (d in directions) {
                val nr = r + d[0]
                val nc = c + d[1]

                if (
                    nr in grid.indices &&       // Check row bounds
                    nc in grid[0].indices &&    // Check col bounds
                    grid[nr][nc] == 0 &&        // Must be a path
                    (nr to nc) !in visited      // Must not be visited
                ) {
                    visited.add(nr to nc)
                    queue.add(nr to nc)
                }
            }
        }
    }

    /** Adjacency List: Map-based graph construction */
    fun buildGraph(edges: List<Pair<Int, Int>>, undirected: Boolean = true) {
        for ((src, dst) in edges) {
            // Add forward edge
            adjList.getOrPut(src) { mutableListOf() }.add(dst)

            // Add backward edge if undirected
            if (undirected) {
                adjList.getOrPut(dst) { mutableListOf() }.add(src)
            }
        }
    }
}
