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



# Kotlin Functions: Step-by-Step Breakdown

---

## 1. `topTwoFrequent()` - Complete Walkthrough

### Function:
```kotlin
fun topTwoFrequent(s: String): List<Char> {
    val freq = s.groupingBy { it }.eachCount()
    return freq.entries
        .sortedByDescending { it.value }
        .take(2)
        .map { it.key }
}
```

### Example: `topTwoFrequent("aaabbc")`

---

### **Step 1: Input**
```
s = "aaabbc"
Characters: a, a, a, b, b, c
```

---

### **Step 2: `s.groupingBy { it }.eachCount()`**

This counts how many times each character appears.

```
Process:
- 'a' appears at position 0 ✓
- 'a' appears at position 1 ✓
- 'a' appears at position 2 ✓
- 'b' appears at position 3 ✓
- 'b' appears at position 4 ✓
- 'c' appears at position 5 ✓

Result (freq):
{
  'a' -> 3,
  'b' -> 2,
  'c' -> 1
}

Type: Map<Char, Int>
```

---

### **Step 3: `freq.entries`**

Convert the map into a set of key-value pairs.

```
Before:
{
  'a' -> 3,
  'b' -> 2,
  'c' -> 1
}

After .entries:
[
  'a' = 3,    ← This is an "entry" (key-value pair)
  'b' = 2,
  'c' = 1
]

Type: Set<Map.Entry<Char, Int>>
```

---

### **Step 4: `.sortedByDescending { it.value }`**

Sort entries by their VALUE (frequency) from highest to lowest.

```
Before sorting:
[
  'a' = 3,
  'b' = 2,
  'c' = 1
]

Sorting logic:
- 'a' has value 3
- 'b' has value 2
- 'c' has value 1

Compare: 3 > 2 > 1 ✓

After .sortedByDescending:
[
  'a' = 3,    ← highest frequency
  'b' = 2,
  'c' = 1
]

Type: List<Map.Entry<Char, Int>>
```

---

### **Step 5: `.take(2)`**

Take only the first 2 elements from the sorted list.

```
Before:
[
  'a' = 3,
  'b' = 2,
  'c' = 1
]

After .take(2):
[
  'a' = 3,
  'b' = 2
]    ← 'c' is dropped

Type: List<Map.Entry<Char, Int>>
```

---

### **Step 6: `.map { it.key }`**

Extract only the KEY (the character) from each entry, discard the value.

```
Before:
[
  'a' = 3,
  'b' = 2
]

Map process:
- Entry ('a' = 3) → extract key → 'a'
- Entry ('b' = 2) → extract key → 'b'

After .map { it.key }:
['a', 'b']

Type: List<Char>
```

---

### **Final Result:**
```
topTwoFrequent("aaabbc") = ['a', 'b']
```

---

## 2. `groupBy()` - How It Partitions Data

### What `groupBy` does:
Groups elements into a map based on a condition.

### Example 1: Group numbers by even/odd

```kotlin
val numbers = listOf(1, 2, 3, 4, 5, 6)
val grouped = numbers.groupBy { it % 2 }

Process:
- 1 % 2 = 1 (odd)     → group "1"
- 2 % 2 = 0 (even)    → group "0"
- 3 % 2 = 1 (odd)     → group "1"
- 4 % 2 = 0 (even)    → group "0"
- 5 % 2 = 1 (odd)     → group "1"
- 6 % 2 = 0 (even)    → group "0"

Result:
{
  0: [2, 4, 6],        ← all evens
  1: [1, 3, 5]         ← all odds
}

Type: Map<Int, List<Int>>
```

---

### Example 2: Group characters by themselves (frequency)

```kotlin
val chars = "aabbcc"
val grouped = chars.groupBy { it }

Process:
- 'a' → group by itself
- 'a' → group by itself
- 'b' → group by itself
- 'b' → group by itself
- 'c' → group by itself
- 'c' → group by itself

Result:
{
  'a': ['a', 'a'],
  'b': ['b', 'b'],
  'c': ['c', 'c']
}

Type: Map<Char, List<Char>>
```

---

### Key Difference: `groupBy` vs `groupingBy().eachCount()`

```kotlin
// groupBy - returns lists of values
"aabbcc".groupBy { it }
// {'a': ['a', 'a'], 'b': ['b', 'b'], 'c': ['c', 'c']}

// groupingBy().eachCount() - returns counts
"aabbcc".groupingBy { it }.eachCount()
// {'a': 2, 'b': 2, 'c': 2}
```

---

## 3. `windowed()` - Sliding Window Explained

### What `windowed` does:
Creates overlapping "windows" of elements, sliding one at a time.

---

### Example 1: Basic windowed with size 3

```kotlin
val list = listOf(1, 2, 3, 4, 5)
val result = list.windowed(3)

Visual:
Original: [1, 2, 3, 4, 5]

Window 1: [1, 2, 3]  ← positions 0-2
Window 2:    [2, 3, 4]  ← positions 1-3
Window 3:       [3, 4, 5]  ← positions 2-4

Result:
[
  [1, 2, 3],
  [2, 3, 4],
  [3, 4, 5]
]
```

---

### Example 2: Windowed with step = 2

```kotlin
val list = listOf(1, 2, 3, 4, 5)
val result = list.windowed(3, step = 2)

Visual:
Original: [1, 2, 3, 4, 5]

Window 1: [1, 2, 3]     ← start at position 0
Jump by 2 steps
Window 2:       [3, 4, 5]  ← start at position 2
Jump by 2 steps
Window 3:             ??? → can't fit 3 elements, stop

Result:
[
  [1, 2, 3],
  [3, 4, 5]
]
```

---

### Example 3: Find max in each window (subarray problem)

```kotlin
val list = listOf(1, 3, 1, 2, 0, 5)
val result = list.windowed(3) { it.maxOrNull() ?: 0 }

Process each window:

Window 1: [1, 3, 1]
  maxOrNull() → 3

Window 2: [3, 1, 2]
  maxOrNull() → 3

Window 3: [1, 2, 0]
  maxOrNull() → 2

Window 4: [2, 0, 5]
  maxOrNull() → 5

Result:
[3, 3, 2, 5]
```

---

## 4. `associate()` - Create a Map from a List

### What `associate` does:
Transform each element into a key-value pair and collect into a map.

### Example 1: Map numbers to their doubles

```kotlin
val numbers = listOf(1, 2, 3)
val result = numbers.associate { it to it * 2 }

Process each element:

Element 1:
  it = 1
  it to it * 2 = 1 to 2
  Add to map: {1: 2}

Element 2:
  it = 2
  it to it * 2 = 2 to 4
  Add to map: {1: 2, 2: 4}

Element 3:
  it = 3
  it to it * 2 = 3 to 6
  Add to map: {1: 2, 2: 4, 3: 6}

Final Result:
{1: 2, 2: 4, 3: 6}

Type: Map<Int, Int>
```

---

### Example 2: Create index map

```kotlin
val words = listOf("apple", "banana", "cherry")
val result = words.associate { it to it.length }

Process:

Element "apple":
  "apple" to "apple".length = "apple" to 5

Element "banana":
  "banana" to "banana".length = "banana" to 6

Element "cherry":
  "cherry" to "cherry".length = "cherry" to 6

Result:
{
  "apple": 5,
  "banana": 6,
  "cherry": 6
}

Type: Map<String, Int>
```

---

## 5. `toMap()` - Convert Pairs to Map

### What `toMap` does:
Takes a list of key-value pairs and converts to a map.

### Example:

```kotlin
val pairs = listOf("a" to 1, "b" to 2, "c" to 3)
val result = pairs.toMap()

What is "a" to 1?
  It's shorthand for Pair("a", 1)
  Creates a tuple: ("a", 1)

Process pairs:

Pair 1: "a" to 1 → key="a", value=1
Pair 2: "b" to 2 → key="b", value=2
Pair 3: "c" to 3 → key="c", value=3

Combine into map:
{
  "a": 1,
  "b": 2,
  "c": 3
}

Type: Map<String, Int>
```

---

## 6. `flatten()` - Flatten Nested Lists

### What `flatten` does:
Takes a list of lists and merges them into a single flat list.

### Example:

```kotlin
val nested = listOf(listOf(1, 2), listOf(3, 4), listOf(5))
val result = nested.flatten()

Visual:
Before (nested structure):
[
  [1, 2],
  [3, 4],
  [5]
]

Process:
- Take [1, 2] → add 1, add 2
- Take [3, 4] → add 3, add 4
- Take [5] → add 5

After (flat):
[1, 2, 3, 4, 5]

Type: List<Int>
```

---

## 7. `flatMap()` - Transform AND Flatten

### What `flatMap` does:
Transform each element into a list, then flatten all lists together.

### Example 1: Duplicate each number

```kotlin
val numbers = listOf(1, 2, 3)
val result = numbers.flatMap { listOf(it, it * 2) }

Process each element:

Element 1:
  it = 1
  listOf(1, 1 * 2) = listOf(1, 2)
  Intermediate: [[1, 2], ...]

Element 2:
  it = 2
  listOf(2, 2 * 2) = listOf(2, 4)
  Intermediate: [[1, 2], [2, 4], ...]

Element 3:
  it = 3
  listOf(3, 3 * 2) = listOf(3, 6)
  Intermediate: [[1, 2], [2, 4], [3, 6]]

Now flatten (merge all lists):
[1, 2, 2, 4, 3, 6]

Type: List<Int>
```

---

### Example 2: Expand each word into characters

```kotlin
val words = listOf("hi", "bye")
val result = words.flatMap { it.toList() }

Process:

Element "hi":
  "hi".toList() = ['h', 'i']
  Intermediate: [['h', 'i'], ...]

Element "bye":
  "bye".toList() = ['b', 'y', 'e']
  Intermediate: [['h', 'i'], ['b', 'y', 'e']]

Flatten:
['h', 'i', 'b', 'y', 'e']

Type: List<Char>
```

---

## Comparison: `map` vs `flatMap`

### `map` - keeps structure
```kotlin
val numbers = listOf(1, 2, 3)
val result = numbers.map { listOf(it, it * 2) }

Result: [[1, 2], [2, 4], [3, 6]]  ← still nested!
```

### `flatMap` - flattens structure
```kotlin
val numbers = listOf(1, 2, 3)
val result = numbers.flatMap { listOf(it, it * 2) }

Result: [1, 2, 2, 4, 3, 6]  ← flat!
```

---

## Quick Reference: When to Use Each

| Function | Does What | Returns |
|----------|-----------|---------|
| `groupBy { }` | Group by condition | Map<Key, List<Value>> |
| `groupingBy().eachCount()` | Count frequencies | Map<Key, Int> |
| `windowed(n)` | Sliding windows | List<List<T>> |
| `windowed(n) { }` | Windows + transform | List<TransformType> |
| `associate { }` | Transform to pairs | Map<Key, Value> |
| `toMap()` | Pairs → map | Map<Key, Value> |
| `flatten()` | Nested list → flat | List<T> |
| `flatMap { }` | Transform + flatten | List<T> |

---

## Real LeetCode Usage Examples

### Frequency Counter (Interview Classic)
```kotlin
fun isAnagram(s: String, t: String): Boolean {
    return s.groupingBy { it }.eachCount() == 
           t.groupingBy { it }.eachCount()
}

// "listen" and "silent" return true
```

### Sliding Window Maximum (Hard Problem)
```kotlin
fun maxSlidingWindow(nums: IntArray, k: Int): IntArray {
    return nums.windowed(k) { it.maxOrNull() ?: 0 }.toIntArray()
}

// [1, 3, 1, 2, 0, 5], k=3 → [3, 3, 2, 5]
```

### Group by Modulo
```kotlin
fun groupByOddEven(nums: List<Int>) {
    val grouped = nums.groupBy { if (it % 2 == 0) "even" else "odd" }
    println(grouped)
    // {even: [2, 4], odd: [1, 3, 5]}
}
```

### Flatten Matrix
```kotlin
val matrix = listOf(listOf(1, 2), listOf(3, 4))
val flat = matrix.flatten()  // [1, 2, 3, 4]
```
