# Common LeetCode Patterns - Detailed Breakdown

## Pattern 1: Frequency Counter

### Problem Example: Check if two strings are anagrams

An anagram means both strings have the same characters with the same frequencies.

Example: "listen" and "silent" are anagrams

---

### Solution:

```kotlin
fun isAnagram(s: String, t: String): Boolean {
    return s.groupingBy { it }.eachCount() == 
           t.groupingBy { it }.eachCount()
}
```

---

### Walkthrough: `isAnagram("listen", "silent")`

**Step 1: Process string `s = "listen"`**

```
Characters: l, i, s, t, e, n

groupingBy { it }.eachCount():
- 'l' appears 1 time
- 'i' appears 1 time
- 's' appears 1 time
- 't' appears 1 time
- 'e' appears 1 time
- 'n' appears 1 time

Result: {l=1, i=1, s=1, t=1, e=1, n=1}
```

**Step 2: Process string `t = "silent"`**

```
Characters: s, i, l, e, n, t

groupingBy { it }.eachCount():
- 's' appears 1 time
- 'i' appears 1 time
- 'l' appears 1 time
- 'e' appears 1 time
- 'n' appears 1 time
- 't' appears 1 time

Result: {s=1, i=1, l=1, e=1, n=1, t=1}
```

**Step 3: Compare frequency maps**

```
s frequency: {l=1, i=1, s=1, t=1, e=1, n=1}
t frequency: {s=1, i=1, l=1, e=1, n=1, t=1}

Same keys? YES ✓
Same values? YES ✓

Return: true
```

---

### Why This Works:

Two strings are anagrams **if and only if** they have identical character frequencies. 

This solution:
1. Counts every character in both strings
2. Compares the frequency maps
3. If maps are equal → anagrams ✓

---

### Real Test Cases:

```kotlin
isAnagram("anagram", "nagaram")     // true
isAnagram("rat", "car")            // false
isAnagram("ab", "ba")              // true
isAnagram("a", "b")                // false
isAnagram("aab", "baa")            // true
```

---

## Pattern 2: Two-Sum (Hash Map Lookup)

### Problem Example: Find two numbers that add to target

Given array and target, return indices of the two numbers that sum to target.

Example: `nums = [2, 7, 11, 15]`, `target = 9` → `[0, 1]` (because 2 + 7 = 9)

---

### Solution:

```kotlin
fun twoSum(nums: IntArray, target: Int): IntArray {
    val map = mutableMapOf<Int, Int>()  // value → index
    
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

---

### Walkthrough: `twoSum([2, 7, 11, 15], 9)`

**Initial State:**
```
nums = [2, 7, 11, 15]
target = 9
map = {} (empty)
```

---

**Iteration 1: i = 0, num = 2**

```
Step 1: Calculate complement
  complement = target - num = 9 - 2 = 7

Step 2: Check if complement exists in map
  Does map contain 7? NO
  map = {}

Step 3: Add current number to map
  map[2] = 0
  map = {2: 0}
  
State: map = {2: 0}
```

---

**Iteration 2: i = 1, num = 7**

```
Step 1: Calculate complement
  complement = target - num = 9 - 7 = 2

Step 2: Check if complement exists in map
  Does map contain 2? YES! ✓
  
Step 3: Return indices
  map[2] = 0  (index of 2)
  i = 1       (current index)
  
Return: [0, 1]
```

---

### Why This Works:

**Key Insight:** If we've already seen a number, and the current number's complement matches that previous number, we found our pair!

**Algorithm:**
1. For each number, calculate what we need: `complement = target - current`
2. Check if we've already seen that complement
3. If yes → return both indices
4. If no → remember this number for later

**Time Complexity:** O(n) - single pass through array  
**Space Complexity:** O(n) - hash map stores up to n elements

---

### Real Test Cases:

```kotlin
twoSum([2, 7, 11, 15], 9)      // [0, 1] → 2 + 7 = 9
twoSum([3, 2, 4], 6)           // [1, 2] → 2 + 4 = 6
twoSum([3, 3], 6)              // [0, 1] → 3 + 3 = 6
twoSum([2, 5, 5, 11], 10)      // [1, 2] → 5 + 5 = 10
```

---

## Pattern 3: Graph Adjacency List

### Problem Example: Build a graph structure

Store connections between nodes. Node 1 connects to nodes 2 and 3.

---

### Solution:

```kotlin
val graph = mutableMapOf<Int, MutableList<Int>>()
graph.getOrPut(1) { mutableListOf() }.add(2)
graph.getOrPut(1) { mutableListOf() }.add(3)
graph.getOrPut(2) { mutableListOf() }.add(3)
```

---

### Step-by-Step: Building the graph

**Step 1: Add edge 1 → 2**

```
graph.getOrPut(1) { mutableListOf() }.add(2)

Process:
- Does graph contain key 1? NO
- Create new list: [2]
- Add to graph: {1: [2]}

Result: graph = {1: [2]}
```

---

**Step 2: Add edge 1 → 3**

```
graph.getOrPut(1) { mutableListOf() }.add(3)

Process:
- Does graph contain key 1? YES
- Get existing list: [2]
- Add 3 to it: [2, 3]

Result: graph = {1: [2, 3]}
```

---

**Step 3: Add edge 2 → 3**

```
graph.getOrPut(2) { mutableListOf() }.add(3)

Process:
- Does graph contain key 2? NO
- Create new list: [3]
- Add to graph: {2: [3]}

Result: graph = {1: [2, 3], 2: [3]}
```

---

### Visual Representation:

```
Adjacency List: {1: [2, 3], 2: [3]}

Graph Structure:
    1
   / \
  2   3
  |
  3

Connections:
- Node 1 → Node 2
- Node 1 → Node 3
- Node 2 → Node 3
```

---

### Complete Example: Build from edges

```kotlin
val edges = listOf(
    Pair(1, 2),
    Pair(1, 3),
    Pair(2, 3),
    Pair(2, 4)
)

val graph = mutableMapOf<Int, MutableList<Int>>()

for ((from, to) in edges) {
    graph.getOrPut(from) { mutableListOf() }.add(to)
}

// Result:
// graph = {
//   1: [2, 3],
//   2: [3, 4]
// }
```

---

### Real Test Cases:

```kotlin
// Access neighbors of node 1
graph[1]  // [2, 3]

// Check if edge exists
graph[1]?.contains(2)  // true

// Iterate all neighbors
for (neighbor in graph[1] ?: emptyList()) {
    println(neighbor)  // prints 2, then 3
}

// Get all nodes
graph.keys  // [1, 2]
```

---

## Pattern 4: BFS with Queue

### Problem Example: Level-order traversal of tree

Visit nodes level by level: first the root, then all children, then all grandchildren, etc.

---

### Solution:

```kotlin
fun levelOrder(root: TreeNode?): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    if (root == null) return result
    
    val queue = ArrayDeque<TreeNode>()
    queue.add(root)
    
    while (queue.isNotEmpty()) {
        val levelSize = queue.size
        val currentLevel = mutableListOf<Int>()
        
        // Process all nodes at current level
        for (i in 0 until levelSize) {
            val node = queue.removeFirst()
            currentLevel.add(node.val)
            
            if (node.left != null) queue.add(node.left)
            if (node.right != null) queue.add(node.right)
        }
        
        result.add(currentLevel)
    }
    
    return result
}
```

---

### Walkthrough: Tree Level Order Traversal

**Tree Structure:**
```
        1
       / \
      2   3
     / \
    4   5
```

---

**Initial State:**

```
queue = [1]
result = []
```

---

**Level 1: Process node 1**

```
queue = [1]
levelSize = 1 (only node 1)

Iteration 1:
  - removeFirst() → node = 1
  - Add 1 to currentLevel → [1]
  - Add left child (2) to queue → queue = [2]
  - Add right child (3) to queue → queue = [2, 3]

After loop:
  - result = [[1]]
  - queue = [2, 3]
```

---

**Level 2: Process nodes 2, 3**

```
queue = [2, 3]
levelSize = 2 (nodes 2 and 3)

Iteration 1:
  - removeFirst() → node = 2
  - Add 2 to currentLevel → [2]
  - Add left child (4) to queue → queue = [3, 4]
  - Add right child (5) to queue → queue = [3, 4, 5]

Iteration 2:
  - removeFirst() → node = 3
  - Add 3 to currentLevel → [2, 3]
  - No children
  - queue = [4, 5]

After loop:
  - result = [[1], [2, 3]]
  - queue = [4, 5]
```

---

**Level 3: Process nodes 4, 5**

```
queue = [4, 5]
levelSize = 2 (nodes 4 and 5)

Iteration 1:
  - removeFirst() → node = 4
  - Add 4 to currentLevel → [4]
  - No children
  - queue = [5]

Iteration 2:
  - removeFirst() → node = 5
  - Add 5 to currentLevel → [4, 5]
  - No children
  - queue = []

After loop:
  - result = [[1], [2, 3], [4, 5]]
  - queue = [] (empty)
```

---

**Final Loop Check:**

```
queue.isNotEmpty()? NO
Exit loop!

Return result = [[1], [2, 3], [4, 5]]
```

---

### Why This Works:

**BFS Property:** Process all nodes at distance k before processing nodes at distance k+1

**Key Technique:** Track `levelSize` to know how many nodes are at current level
- Ensures we process exactly one level per loop iteration
- When we add children, they'll be processed in the next iteration

---

### Real Test Cases:

```kotlin
// Single node
levelOrder(TreeNode(1))
// [[1]]

// Empty tree
levelOrder(null)
// []

// Full tree
//     1
//    / \
//   2   3
levelOrder(root)
// [[1], [2, 3]]
```

---

## Pattern Comparison

| Pattern | Use Case | Key Data Structure | Time |
|---------|----------|-------------------|------|
| **Frequency Counter** | Anagrams, character counts | HashMap | O(n) |
| **Two-Sum** | Find pair matching condition | HashMap | O(n) |
| **Adjacency List** | Graph representation | Map<Node, List<Neighbors>> | O(V+E) |
| **BFS** | Level-order, shortest path | Queue | O(V+E) |

---

## When to Use Each Pattern

**Use Frequency Counter when:**
- Comparing character/element counts
- Checking if arrays are permutations
- Finding missing/duplicate elements

**Use Two-Sum when:**
- Finding pair that matches condition
- Need to track seen elements
- Want O(n) instead of O(n²)

**Use Adjacency List when:**
- Building/representing graphs
- Need to store connections between nodes
- Want quick neighbor lookup

**Use BFS when:**
- Need level-order traversal
- Finding shortest path
- Exploring nodes by distance
- Avoiding recursion (DFS requires stack/recursion)
