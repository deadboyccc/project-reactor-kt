package dev.dead.projectreactorkt

class LeetStrings {
}

// the size of s1 is the sliding window = fixed
/*
 * -------------------------------------------------------------------------
 * DETAILED BREAKDOWN: WINDOWED FREQUENCY MAPPING
 * -------------------------------------------------------------------------
 * 1. TARGET FINGERPRINT:
 * - We use `groupingBy { it }.eachCount()` on s1.
 * - This creates a Map<Char, Int> (e.g., "aabb" -> {a=2, b=2}).
 * - This map serves as a unique "signature" for any permutation of s1.
 *
 * 2. LAZY SLIDING WINDOW:
 * - `s2.windowedSequence(s1.length)` creates a window of s1's size.
 * - It slides across s2 one character at a time.
 * - Using a `Sequence` (lazy) instead of a `List` (eager) ensures
 * we don't store every possible substring in memory at once.
 *
 * 3. LOCAL COMPARISON:
 * - For each window generated, we calculate its own frequency map.
 * - We compare this window's map to our target `s1Counts`.
 * - Kotlin's `==` operator for Maps performs a "deep equality" check,
 * verifying that both maps have the exact same keys and values.
 *
 * 4. SHORT-CIRCUIT EVALUATION:
 * - `.any { ... }` returns `true` the instant a match is found.
 * - This prevents unnecessary processing of the remaining string.
 * -------------------------------------------------------------------------
 */
class SolutionFF {

    fun checkInclusion(s1: String, s2: String): Boolean {
        // Map< Char -> count > for target frequencies
        val s1Counts = s1.groupingBy { it }.eachCount()

        // s2 windowed at s1.length -> Map< Char -> count > for each window -> any match
        return s2.windowedSequence(s1.length)
            .any { it.groupingBy { char -> char }.eachCount() == s1Counts }
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
