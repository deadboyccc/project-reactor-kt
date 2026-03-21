# Kotlin LeetCode Features Guide

## 1. Collection Aggregation Functions

### `reduce` - Combine elements left to right (no initial value)
```kotlin
listOf(1, 2, 3, 4).reduce { acc, num -> acc + num }  // 10
// First element becomes accumulator: 1 + 2 + 3 + 4

// Max/Min
listOf(5, 2, 8, 1).reduce { acc, num -> maxOf(acc, num) }  // 8
```

### `fold` - Like reduce but with initial value
```kotlin
listOf(1, 2, 3, 4).fold(0) { acc, num -> acc + num }  // 10
listOf(1, 2, 3, 4).fold(1) { acc, num -> acc * num }  // 24

// Useful: Count occurrences
listOf('a', 'b', 'a').fold(mutableMapOf<Char, Int>()) { map, char ->
    map[char] = map.getOrDefault(char, 0) + 1
    map
}  // {a=2, b=1}
```

### `scan` - Return intermediate results (like fold but returns list)
```kotlin
listOf(1, 2, 3, 4).scan(0) { acc, num -> acc + num }
// [0, 1, 3, 6, 10]  ← includes initial value and all intermediates

// Running sum: useful for prefix sums problems
val nums = intArrayOf(1, 2, 3, 4)
nums.scan(0) { acc, n -> acc + n }.drop(1)  // [1, 3, 6, 10]
```

### `windowed` - Slide a window over elements
```kotlin
listOf(1, 2, 3, 4, 5).windowed(3)
// [[1, 2, 3], [2, 3, 4], [3, 4, 5]]

listOf(1, 2, 3, 4, 5).windowed(3, step = 2)
// [[1, 2, 3], [3, 4, 5]]

// Max in each window (subarray problems)
listOf(1, 3, 1, 2, 0, 5).windowed(3) { it.maxOrNull() ?: 0 }
// [3, 3, 2, 5]
```

---

## 2. Map Functions (Critical for LeetCode)

### `getOrDefault` - Safe access with fallback
```kotlin
val map = mutableMapOf("a" to 1, "b" to 2)
map.getOrDefault("c", 0)  // 0 (safe, no exception)
map.getOrDefault("a", 0)  // 1

// Common: count frequencies
val freq = mutableMapOf<Char, Int>()
for (char in "aab") {
    freq[char] = freq.getOrDefault(char, 0) + 1
}  // {a=2, b=1}
```

### `getOrPut` - Get or compute and store
```kotlin
val map = mutableMapOf<String, MutableList<String>>()

// Instead of: if (!map.containsKey(key)) map[key] = mutableListOf()
// Use getOrPut:
map.getOrPut("vowels") { mutableListOf() }.add("a")
map.getOrPut("vowels") { mutableListOf() }.add("e")
// {vowels=[a, e]}  ← computed lambda only runs if key missing

// Group elements
val grouped = mutableMapOf<Int, MutableList<Int>>()
for (num in listOf(1, 2, 1, 3)) {
    grouped.getOrPut(num) { mutableListOf() }.add(num)
}  // {1=[1, 1], 2=[2], 3=[3]}
```

### `putIfAbsent` - Only set if key doesn't exist
```kotlin
val map = mutableMapOf("a" to 1)
map.putIfAbsent("a", 2)  // returns 1, map unchanged
map.putIfAbsent("b", 2)  // returns null, map becomes {a=1, b=2}
```

### `compute` / `computeIfPresent` / `computeIfAbsent`
```kotlin
val map = mutableMapOf("a" to 1)

// compute: always recompute
map.compute("a") { key, value -> value!! + 1 }  // {a=2}

// computeIfAbsent: only if missing
map.computeIfAbsent("b") { 0 }  // {a=2, b=0}

// computeIfPresent: only if exists
map.computeIfPresent("a") { key, value -> value + 10 }  // {a=12, b=0}
```

---

## 3. Collections: Lists, Arrays, ArrayDeque

### `MutableList` (flexible size)
```kotlin
val list = mutableListOf(1, 2, 3)
list.add(4)          // [1, 2, 3, 4]
list.remove(2)       // [1, 3, 4]
list[0] = 10         // [10, 3, 4]
list.removeAt(1)     // [10, 4]

// Common in graph problems (adjacency list)
val graph = mutableMapOf<Int, MutableList<Int>>()
graph.getOrPut(1) { mutableListOf() }.add(2)  // node 1 → node 2
```

### `IntArray` / `CharArray` (fixed size, primitive)
```kotlin
val arr = IntArray(5)           // [0, 0, 0, 0, 0]
val arr = IntArray(5) { it }    // [0, 1, 2, 3, 4]  ← lambda init
val arr = intArrayOf(1, 2, 3)   // [1, 2, 3]

// Faster than List for tight loops
arr[0] = 10
arr.forEach { println(it) }

// Sum, max, min
arr.sum()      // quick math
arr.maxOrNull()
arr.minOrNull()
```

### `ArrayDeque` (double-ended queue, fast at both ends)
```kotlin
val deque = ArrayDeque<Int>()
deque.addFirst(1)    // [1]
deque.addLast(2)     // [1, 2]
deque.removeFirst()  // [2], returns 1
deque.removeLast()   // [], returns 2
deque.firstOrNull()  // safe access

// Sliding window, BFS
val queue = ArrayDeque<Int>()
queue.add(1)         // enqueue
queue.removeFirst()  // dequeue
```

---

## 4. Useful Helper Functions

### `also` / `apply` (return self, side effects)
```kotlin
5.also { println("Value is $it") }  // prints, returns 5
listOf(1, 2, 3).also { println(it) }  // prints list, returns list

// apply: modify receiver
mutableListOf(1, 2).apply { add(3) }  // returns [1, 2, 3]
```

### `let` / `run` (return result)
```kotlin
5.let { it * 2 }  // returns 10
listOf(1, 2, 3).let { it.sum() }  // returns 6

// Safe call: process only if not null
val x: Int? = 5
x?.let { it * 2 }  // runs block only if x not null
```

### `repeat` / `until` / `step`
```kotlin
repeat(3) { println("hi") }  // print "hi" 3 times

for (i in 0 until 5) println(i)      // 0, 1, 2, 3, 4
for (i in 5 downTo 0 step 2) println(i)  // 5, 3, 1
```

### `any` / `all` / `none`
```kotlin
listOf(1, 2, 3, 4).any { it > 3 }      // true
listOf(1, 2, 3, 4).all { it > 0 }      // true
listOf(1, 2, 3, 4).none { it > 5 }     // true
```

### `groupBy` (partition into map)
```kotlin
listOf(1, 2, 3, 4, 5).groupBy { it % 2 }
// {0=[2, 4], 1=[1, 3, 5]}  ← evens/odds

val chars = "aabbcc"
chars.groupBy { it }
// {a=[a, a], b=[b, b], c=[c, c]}
```

### `associate` / `toMap` (create map from list)
```kotlin
listOf(1, 2, 3).associate { it to it * 2 }
// {1=2, 2=4, 3=6}

listOf("a" to 1, "b" to 2).toMap()
// {a=1, b=2}
```

### `flatten` / `flatMap`
```kotlin
listOf(listOf(1, 2), listOf(3, 4)).flatten()
// [1, 2, 3, 4]

listOf(1, 2, 3).flatMap { listOf(it, it * 2) }
// [1, 2, 2, 4, 3, 6]
```

---

## 5. String Utilities

### Common conversions
```kotlin
"hello".toCharArray()           // ['h', 'e', 'l', 'l', 'o']
"hello".toList()                // ['h', 'e', 'l', 'l', 'o']
charArrayOf('a', 'b').joinToString()  // "a, b"
charArrayOf('a', 'b').joinToString("")  // "ab"

"hello".reversed()              // "olleh"
"hello".split("")               // ["h", "e", "l", "l", "o"]
```

### Frequency counting (ultra-common)
```kotlin
val s = "aabbcc"
s.groupingBy { it }.eachCount()  // {a=2, b=2, c=2}

// Or manual
val freq = mutableMapOf<Char, Int>()
for (c in s) freq[c] = freq.getOrDefault(c, 0) + 1
```

---

## 6. Quick Reference: When to Use What

| Problem Type | Use This |
|---|---|
| Sum/aggregate array | `reduce`, `fold`, `sum()` |
| Sliding window | `windowed`, two pointers |
| Frequency count | `groupingBy().eachCount()`, `fold` with map |
| Graph adjacency list | `Map<Int, MutableList<Int>>` |
| BFS/queue | `ArrayDeque<T>()` |
| Stack | `ArrayDeque<T>()` (use as stack) |
| Grouping by condition | `groupBy` |
| Safe map access | `getOrDefault`, `getOrPut` |
| Transform lists | `map`, `flatMap`, `filter` |
| Check conditions | `any`, `all`, `none` |

---

## 7. Complete Example: LeetCode Problem

**Problem**: Count character frequencies and return top 2

```kotlin
fun topTwoFrequent(s: String): List<Char> {
    // Frequency map
    val freq = s.groupingBy { it }.eachCount()
    
    // Sort by frequency descending, take top 2
    return freq.entries
        .sortedByDescending { it.value }
        .take(2)
        .map { it.key }
}

// Test
topTwoFrequent("aabbcc")  // [a, b] or [b, a] (both frequency 2)
topTwoFrequent("aaabbc")  // [a, b]
```

---

## Quick Kotlin Syntax Tips

```kotlin
// Elvis operator (null safety)
val x: Int? = 5
val y = x ?: 0  // use x if not null, else 0

// Range
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
