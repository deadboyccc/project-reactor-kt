package dev.dead.projectreactorkt

import kotlin.test.Test


class LeetCodeGraphs {
    @Test
    fun test() {
        println("hi")
    }
}

// given an array of length n -> return array of length 2n such that answer[i] = original[i]
// 2. answer[i+n] == original [i]
class SolutionBB {
    fun getConcatenation(nums: IntArray): IntArray {
        return intArrayOf(*nums, *nums)
    }
}

// island count
/*
    Col 0  Col 1  Col 2  Col 3  Col 4
    R0:  ███    ███    ███    ███     ~
    R1:  ███    ███     ~     ███     ~
    R2:  ███    ███     ~      ~      ~
    R3:   ~      ~      ~      ~      ~

    TOTAL ISLANDS: 1
 */
// just traverse it bfs or dfs and mark visited= per traverse = 1 island
class SolutionZz {
    fun numIslands(grid: Array<CharArray>): Int {

        val visited = hashSetOf<Pair<Int, Int>>()
        var count = 0
        for (row in grid.indices) {
            for (col in grid[row].indices) {
                if (grid[row][col] == '1' && !visited.contains(Pair(row, col))) {
                    dfs(grid, Pair(row, col), visited)
                    count++
                }

            }
        }
        return count


    }

    fun dfs(grid: Array<CharArray>, pair: Pair<Int, Int>, visited: HashSet<Pair<Int, Int>>) {
        val row = pair.first
        val col = pair.second

        visited.add(pair)

        val directions = arrayOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
        )

        for (dir in directions) {
            val newRow = row + dir.first
            val newCol = col + dir.second
            val nextPair = Pair(newRow, newCol)

            if (newRow in grid.indices &&
                newCol in grid[0].indices &&
                grid[newRow][newCol] == '1' &&
                !visited.contains(nextPair)
            ) {

                dfs(grid, nextPair, visited)
            }
        }
    }
}