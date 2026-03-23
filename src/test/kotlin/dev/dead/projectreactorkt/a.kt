package dev.dead.projectreactorkt

import java.util.*


class MatrixExplorer {
    private val dirs = arrayOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)

    /**
     * DFS: Simple recursive "sinking"
     */
    fun dfs(grid: Array<IntArray>, r: Int, c: Int) {
        // 1. Out of bounds or already water? Stop.
        if (r !in grid.indices || c !in grid[0].indices || grid[r][c] == 0) return

        // 2. "Sink" the land to mark as visited
        grid[r][c] = 0

        // 3. Visit all neighbors
        for ((dr, dc) in dirs) dfs(grid, r + dr, c + dc)
    }

    /**
     * BFS: Simple iterative "expansion"
     */
    fun bfs(grid: Array<IntArray>, startR: Int, startC: Int) {
        val q: Queue<Pair<Int, Int>> = LinkedList(listOf(startR to startC))
        grid[startR][startC] = 0

        while (q.isNotEmpty()) {
            val (r, c) = q.poll()

            for ((dr, dc) in dirs) {
                val nr = r + dr
                val nc = c + dc

                if (nr in grid.indices && nc in grid[0].indices && grid[nr][nc] == 1) {
                    grid[nr][nc] = 0 // Sink before adding to queue
                    q.add(nr to nc)
                }
            }
        }
    }
}
