package dev.dead.projectreactorkt

class LeetStrings {
}

class Solution {
    fun checkInclusion(s1: String, s2: String): Boolean {

    }
}

class SolutionPPX {
    fun isPalindrome(s: String): Boolean {
        val filtered = s.filter { it.isLetterOrDigit() }.lowercase()
        return filtered == filtered.reversed()
    }
}

// string = abc
// reverse = cba
class SolutionJJ {
    fun validPalindrome(s: String): Boolean {
        var left = 0
        for (right in s.length - 1 downTo 0) {
            if (left >= right) break

            if (s[left] != s[right]) {
                val s1 = s.removeRange(left, left + 1)
                val s2 = s.removeRange(right, right + 1)

                return s1 == s1.reversed() || s2 == s2.reversed()
            }
            left++
        }
        return true
    }
}
