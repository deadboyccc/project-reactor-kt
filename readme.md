# Kotlin LeetCode Features Guide

## Aggregation Functions

### `reduce` – Combine elements (first element is accumulator)
```kotlin
listOf(1, 2, 3, 4).reduce { acc, num -> acc + num }  // 10
listOf(5, 2, 8, 1).reduce { acc, num -> maxOf(acc, num) }  // 8
```

### `fold` – Combine elements with initial value
```kotlin
listOf(1, 2, 3, 4).fold(0) { acc, num -> acc + num }  // 10
listOf(1, 2, 3, 4).fold(1) { acc, num -> acc * num }  // 24

// Count frequencies
listOf('a', 'b', 'a').fold(mutableMapOf<Char, Int>()) { map, char ->
    map[char] = map.getOrDefault(char, 0) + 1
    map
}  // {a=2, b=1}
```

### `scan` – Return all intermediate results (fold but returns list)
```kotlin
listOf(1, 2, 3, 4).scan(0) { acc, num -> acc + num }  // [0, 1, 3, 6, 10]
// Each element is the accumulator at that step
```

### `windowed` – Slide a window across elements
```kotlin
listOf(1, 2, 3, 4, 5).windowed(3)
// [[1,2,3], [2,3,4], [3,4,5]]  ← slides by 1

listOf(1, 2, 3, 4, 5).windowed(3, step = 2)
// [[1,2,3], [3,4,5]]  ← slides by 2

// Find max in each window
listOf(1, 3, 1, 2, 0, 5).windowed(3) { it.maxOrNull() ?: 0 }
// [3, 3, 2, 5]
```

---

## Map Functions

### `getOrDefault` – Safe access with fallback
```kotlin
val map = mutableMapOf("a" to 1)
map.getOrDefault("b", 0)  // 0 (key missing → default)
map.getOrDefault("a", 0)  // 1 (key exists)

// Frequency counting pattern
val freq = mutableMapOf<Char, Int>()
for (char in "aab") {
    freq[char] = freq.getOrDefault(char, 0) + 1
}  // {a=2, b=1}
```

### `getOrPut` – Get existing value or compute and store
```kotlin
val map = mutableMapOf<String, MutableList<Int>>()

// Lambda only runs if key is missing
map.getOrPut("evens") { mutableListOf() }.add(2)
map.getOrPut("evens") { mutableListOf() }.add(4)
// {evens=[2, 4]}  ← second call reuses existing list
```

### `putIfAbsent` – Only insert if key doesn't exist
```kotlin
val map = mutableMapOf("a" to 1)
map.putIfAbsent("a", 2)  // returns 1, map unchanged
map.putIfAbsent("b", 2)  // returns null, adds entry
```

### `compute` / `computeIfAbsent` / `computeIfPresent`
```kotlin
val map = mutableMapOf("a" to 1)

map.compute("a") { key, value -> value!! + 1 }  // {a=2}
map.computeIfAbsent("b") { 0 }  // {a=2, b=0}
map.computeIfPresent("a") { key, value -> value + 10 }  // {a=12}
```

---

## Collections

### `MutableList` – Dynamic size
```kotlin
val list = mutableListOf(1, 2, 3)
list.add(4)          // [1, 2, 3, 4]
list.remove(2)       // [1, 3, 4]
list[0] = 10         // [10, 3, 4]

// Graph adjacency list (common pattern)
val graph = mutableMapOf<Int, MutableList<Int>>()
graph.getOrPut(1) { mutableListOf() }.add(2)  // edge 1→2
```

### `IntArray` / `CharArray` – Fixed size, primitive (faster)
```kotlin
val arr = IntArray(5)          // [0, 0, 0, 0, 0]
val arr = IntArray(5) { it }   // [0, 1, 2, 3, 4]
val arr = intArrayOf(1, 2, 3)  // [1, 2, 3]

arr.sum()      // quick aggregation
arr.maxOrNull()
arr.minOrNull()
```

### `ArrayDeque` – Fast at both ends (queue/stack)
```kotlin
val deque = ArrayDeque<Int>()
deque.addFirst(1)      // [1]
deque.addLast(2)       // [1, 2]
deque.removeFirst()    // returns 1
deque.removeLast()     // returns 2

// Use as queue
val queue = ArrayDeque<Int>()
queue.add(1)           // enqueue
queue.removeFirst()    // dequeue
```

---

## Transformation Functions

### `groupBy` – Partition by condition
```kotlin
listOf(1, 2, 3, 4, 5).groupBy { it % 2 }
// {0=[2, 4], 1=[1, 3, 5]}  ← evens/odds

"aabbcc".groupBy { it }
// {a=[a, a], b=[b, b], c=[c, c]}
```

### `groupingBy().eachCount()` – Count frequencies (preferred over groupBy)
```kotlin
"aabbcc".groupingBy { it }.eachCount()
// {a=2, b=2, c=2}  ← only counts, not lists
```

### `associate` – Transform each element to key-value pair
```kotlin
listOf(1, 2, 3).associate { it to it * 2 }
// {1=2, 2=4, 3=6}

listOf("apple", "banana").associate { it to it.length }
// {apple=5, banana=6}
```

### `toMap()` – Convert pairs to map
```kotlin
listOf("a" to 1, "b" to 2).toMap()
// {a=1, b=2}
```

### `flatten` – Merge nested lists
```kotlin
listOf(listOf(1, 2), listOf(3, 4)).flatten()
// [1, 2, 3, 4]
```

### `flatMap` – Transform then flatten
```kotlin
listOf(1, 2, 3).flatMap { listOf(it, it * 2) }
// [1, 2, 2, 4, 3, 6]
// Each element becomes a list, then all lists merge

// vs map (keeps structure)
listOf(1, 2, 3).map { listOf(it, it * 2) }
// [[1, 2], [2, 4], [3, 6]]  ← still nested
```

---

## Utility Functions

### `also` / `apply` – Execute block, return self
```kotlin
5.also { println("Value is $it") }  // prints, returns 5

mutableListOf(1, 2).apply { add(3) }  // returns [1, 2, 3]
```

### `let` / `run` – Execute block, return result
```kotlin
5.let { it * 2 }  // returns 10

val x: Int? = 5
x?.let { it * 2 }  // runs only if x is not null
```

### `any` / `all` / `none` – Check conditions
```kotlin
listOf(1, 2, 3, 4).any { it > 3 }   // true (at least one)
listOf(1, 2, 3, 4).all { it > 0 }   // true (all match)
listOf(1, 2, 3, 4).none { it > 5 }  // true (none match)
```

### `repeat` / `until` / `step` – Loops
```kotlin
repeat(3) { println("hi") }  // print 3 times

for (i in 0 until 5) println(i)        // 0, 1, 2, 3, 4
for (i in 5 downTo 0 step 2) println(i)  // 5, 3, 1
```

---

## String Utilities

### Conversions
```kotlin
"hello".toCharArray()        // ['h','e','l','l','o']
"hello".toList()             // ['h','e','l','l','o']
charArrayOf('a', 'b').joinToString("")  // "ab"

"hello".reversed()           // "olleh"
"hello".split("")            // ["h","e","l","l","o"]
```

### Frequency counting
```kotlin
"aabbcc".groupingBy { it }.eachCount()  // {a=2, b=2, c=2}
```

---

## Quick Reference

| Task | Solution |
|------|----------|
| Sum/aggregate | `reduce`, `fold`, `sum()` |
| Sliding window | `windowed(n)` |
| Frequency map | `groupingBy().eachCount()` |
| Partition by condition | `groupBy { }` |
| Transform to map | `associate { }` |
| Safe map lookup | `getOrDefault`, `getOrPut` |
| Queue/Stack | `ArrayDeque<T>()` |
| Flatten lists | `flatten()`, `flatMap()` |
| Check conditions | `any`, `all`, `none` |

---

## Step-by-Step Examples

### Example 1: Top 2 Most Frequent Characters

```kotlin
fun topTwoFrequent(s: String): List<Char> {
    val freq = s.groupingBy { it }.eachCount()
    return freq.entries
        .sortedByDescending { it.value }
        .take(2)
        .map { it.key }
}

// Walkthrough: topTwoFrequent("aaabbc")

// Step 1: Count frequencies
// "aaabbc".groupingBy { it }.eachCount()
// → {a=3, b=2, c=1}

// Step 2: Get entries (key-value pairs)
// → [a=3, b=2, c=1]

// Step 3: Sort by value descending
// → [a=3, b=2, c=1]  (already sorted)

// Step 4: Take first 2
// → [a=3, b=2]

// Step 5: Extract keys
// → ['a', 'b']
```

### Example 2: Max in Each Sliding Window

```kotlin
fun maxSlidingWindow(nums: IntArray, k: Int): IntArray {
    return nums.windowed(k) { it.maxOrNull() ?: 0 }.toIntArray()
}

// Walkthrough: [1, 3, 1, 2, 0, 5], k=3

// Step 1: Create windows of size 3
// → [[1,3,1], [3,1,2], [1,2,0], [2,0,5]]

// Step 2: Find max in each
// [1,3,1] → 3
// [3,1,2] → 3
// [1,2,0] → 2
// [2,0,5] → 5

// Result: [3, 3, 2, 5]
```

### Example 3: Group and Aggregate

```kotlin
val numbers = listOf(1, 2, 3, 4, 5, 6)
val grouped = numbers.groupBy { it % 2 }
// → {0=[2, 4, 6], 1=[1, 3, 5]}

val evens = grouped[0]   // [2, 4, 6]
val odds = grouped[1]    // [1, 3, 5]
```

### Example 4: Transform to Map

```kotlin
// Before: listOf(1, 2, 3)
// Goal: {1→2, 2→4, 3→6}

val result = listOf(1, 2, 3).associate { it to it * 2 }
// Step-by-step:
// Element 1: 1 to 2 → add to map
// Element 2: 2 to 4 → add to map
// Element 3: 3 to 6 → add to map
// → {1=2, 2=4, 3=6}
```

### Example 5: FlatMap vs Map

```kotlin
val numbers = listOf(1, 2, 3)

// map: Transform, keep structure
numbers.map { listOf(it, it * 2) }
// → [[1,2], [2,4], [3,6]]  ← nested

// flatMap: Transform, then flatten
numbers.flatMap { listOf(it, it * 2) }
// → [1, 2, 2, 4, 3, 6]  ← flat

// What happens internally:
// 1. Transform: 1 → [1, 2], 2 → [2, 4], 3 → [3, 6]
// 2. Flatten: [[1,2], [2,4], [3,6]] → [1, 2, 2, 4, 3, 6]
```

---

## Kotlin Syntax Tips

```kotlin
// Elvis operator (null coalescing)
val x: Int? = 5
val y = x ?: 0  // use x if not null, else 0

// Ranges
for (i in 1..5) println(i)      // 1, 2, 3, 4, 5
for (i in 1 until 5) println(i) // 1, 2, 3, 4

// Destructuring
val (key, value) = mapEntry
for ((k, v) in map) println("$k: $v")

// When (pattern matching)
when {
    x > 0 -> println("positive")
    x < 0 -> println("negative")
    else -> println("zero")
}
```

---

## Common LeetCode Patterns

### Frequency Counter
```kotlin
fun isAnagram(s: String, t: String): Boolean {
    return s.groupingBy { it }.eachCount() == 
           t.groupingBy { it }.eachCount()
}
```

### Two-Sum Pattern
```kotlin
fun twoSum(nums: IntArray, target: Int): IntArray {
    val map = mutableMapOf<Int, Int>()
    for ((i, num) in nums.withIndex()) {
        val complement = target - num
        if (map.containsKey(complement)) {
            return intArrayOf(map[complement]!!, i)
        }
        map[num] = i
    }
    return intArrayOf()
}
```

### Graph Adjacency List
```kotlin
val graph = mutableMapOf<Int, MutableList<Int>>()
graph.getOrPut(1) { mutableListOf() }.add(2)
graph.getOrPut(2) { mutableListOf() }.add(3)
```

### BFS with Queue
```kotlin
val queue = ArrayDeque<Int>()
queue.add(startNode)
while (queue.isNotEmpty()) {
    val node = queue.removeFirst()
    // process node
}
```
