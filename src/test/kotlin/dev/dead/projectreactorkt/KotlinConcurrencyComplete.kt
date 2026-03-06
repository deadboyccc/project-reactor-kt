package dev.dead.projectreactorkt

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

// ─────────────────────────────────────────────────────────────────────────────
// LOGGING UTILITY
// ─────────────────────────────────────────────────────────────────────────────
//
// Every log line shows three pieces of information:
//
//   +-------ms elapsed since test started
//   |          +--- coroutine/thread name (shows dispatcher & coroutine id in debug mode)
//   |          |              +--- your message
//   ↓          ↓              ↓
//  [  42ms] [DefaultDispatcher-worker-1 @coroutine#3]  emitting 1
//
// HOW TO SEE COROUTINE NAMES IN THREAD LABELS:
//   Add this JVM flag to your run config:
//   -Dkotlinx.coroutines.debug
//   Then thread names become: "DefaultDispatcher-worker-1 @coroutine#3"
//   which tells you BOTH the dispatcher pool AND which coroutine is running.

/** Wall-clock anchor — captured once when the file is loaded. */
private val zeroTime = System.currentTimeMillis()

/**
 * Primary log function. Mirrors the pattern from Kotlin in Action:
 *   elapsed-ms  [thread-name]  message
 *
 * @param message  Anything — the toString() value is printed.
 * @param tag      Optional prefix that appears before the message,
 *                 useful for grouping related log lines (e.g. "SEND", "RECV").
 */
fun log(message: Any?, tag: String = "") {
    val elapsed = System.currentTimeMillis() - zeroTime    // ms since program start
    val thread = Thread.currentThread().name              // includes coroutine id with debug flag
    val prefix = if (tag.isNotEmpty()) "[$tag] " else ""  // optional label for grouping lines
    println("%6dms [%-45s] $prefix$message".format(elapsed, thread))
}

/** Log a section header so test output is easy to scan. */
fun logSection(title: String) {
    val line = "─".repeat(60)
    println("\n$line\n  $title\n$line")
}

// ─────────────────────────────────────────────────────────────────────────────
// Suspend helpers used by multiple tests
// ─────────────────────────────────────────────────────────────────────────────

suspend fun fetchUser(id: Int): String {
    log("→ fetchUser($id) start")
    delay(500)                         // simulates a network call
    log("← fetchUser($id) done")
    return "User#$id"
}

suspend fun fetchOrder(userId: String): String {
    log("→ fetchOrder($userId) start")
    delay(300)                         // simulates a DB query
    log("← fetchOrder($userId) done")
    return "Order for $userId"
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. COROUTINE BASICS
// ─────────────────────────────────────────────────────────────────────────────

class Ch01_CoroutineBasics {

    /**
     * runBlocking bridges the blocking world and suspending world.
     * Used in main() or tests. Never use inside a coroutine.
     */
    @Test
    fun `01 - runBlocking bridges blocking and suspending world`() = runBlocking {
        // runBlocking runs ON the calling thread — notice it says "main" or "Test worker"
        log("START — this is the calling (test) thread", tag = "MAIN")
        delay(100)
        // still the same thread after delay inside runBlocking
        log("END   — same thread as start", tag = "MAIN")
    }

    /**
     * launch starts a coroutine that does not return a result.
     * Returns a Job you can wait on or cancel.
     */
    @Test
    fun `02 - launch returns a Job`() = runBlocking {
        log("before launch", tag = "PARENT")
        val job: Job = launch {
            // With -Dkotlinx.coroutines.debug the thread name shows "@coroutine#N"
            log("started  | isActive=${coroutineContext[Job]?.isActive}", tag = "CHILD")
            delay(200)
            log("finished", tag = "CHILD")
        }
        log("job state: isActive=${job.isActive}, isCompleted=${job.isCompleted}", tag = "PARENT")
        job.join()   // suspends parent here until child finishes
        log("after join: isCompleted=${job.isCompleted}", tag = "PARENT")
        assertEquals(true, job.isCompleted)
    }

    /**
     * async returns a Deferred<T> — a future-like value.
     * Call .await() to get the result (suspends the caller).
     */
    @Test
    fun `03 - async and await for results`() = runBlocking {
        log("launching async block", tag = "PARENT")
        val deferred: Deferred<String> = async {
            log("computing…", tag = "ASYNC")
            delay(300)
            log("returning value", tag = "ASYNC")
            "async result"
        }
        // async block runs concurrently while we continue here
        log("doing other work while async runs", tag = "PARENT")
        val result = deferred.await()   // suspends until async finishes
        log("received: $result", tag = "PARENT")
        assertEquals("async result", result)
    }

    /**
     * Structured concurrency: child coroutines are scoped to their parent.
     * The parent only completes when ALL children complete.
     */
    @Test
    fun `04 - structured concurrency parent waits for children`() = runBlocking {
        log("starting — will launch two children", tag = "PARENT")
        // Both children are launched immediately and run concurrently
        launch { delay(100); log("done at ~100ms", tag = "CHILD1") }
        launch { delay(200); log("done at ~200ms", tag = "CHILD2") }
        // This prints BEFORE either child finishes — proves they run concurrently
        log("past launch calls, children still running", tag = "PARENT")
        // runBlocking won't exit until BOTH children complete (structured concurrency)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. COROUTINE CONTEXT & DISPATCHERS
// ─────────────────────────────────────────────────────────────────────────────

class Ch02_ContextAndDispatchers {

    /**
     * Dispatchers control which thread pool a coroutine runs on.
     * - Default  → CPU-bound work (shared thread pool)
     * - IO       → blocking I/O (elastic thread pool)
     * - Main     → UI thread (Android / JavaFX)
     * - Unconfined→ runs in caller thread until first suspension
     */
    @Test
    fun `05 - dispatchers`() = runBlocking {
        // Watch the thread column in output — each dispatcher uses a different pool
        launch(Dispatchers.Default) {
            // CPU-bound shared thread pool (usually named "DefaultDispatcher-worker-N")
            log("running on Default dispatcher", tag = "DEFAULT")
        }
        launch(Dispatchers.IO) {
            // Elastic pool for blocking I/O (usually "DefaultDispatcher-worker-N" but larger pool)
            log("running on IO dispatcher", tag = "IO")
        }
        launch(Dispatchers.Unconfined) {
            // Starts on the caller's thread...
            log("BEFORE first suspension", tag = "UNCONFINED")
            delay(10)
            // ...resumes on whatever thread delay() resumed on (may differ!)
            log("AFTER first suspension — thread may have changed", tag = "UNCONFINED")
        }
        joinAll() // wait for all three
    }

    /**
     * withContext switches dispatcher mid-coroutine without launching a new one.
     * Classic pattern: compute on Default, update UI on Main.
     */
    @Test
    fun `06 - withContext switches dispatcher`() = runBlocking {
        log("start — on calling thread", tag = "MAIN")
        val result = withContext(Dispatchers.Default) {
            // Thread switches here to a worker pool thread
            log("inside withContext — doing heavy work", tag = "COMPUTE")
            42 * 42
        }
        // Thread switches BACK to the original dispatcher automatically
        log("back on calling thread — result=$result", tag = "MAIN")
        assertEquals(1764, result)
    }

    /**
     * CoroutineName adds a debug label visible in thread names when
     * -Dkotlinx.coroutines.debug JVM flag is set.
     */
    @Test
    fun `07 - coroutine name for debugging`() = runBlocking {
        // With -Dkotlinx.coroutines.debug the thread name becomes:
        //   "DefaultDispatcher-worker-1 @my-worker#2"
        //                                 ^^^^^^^^^ ← CoroutineName shows here
        launch(CoroutineName("my-worker") + Dispatchers.Default) {
            log("thread name contains the coroutine name when debug flag is set", tag = "my-worker")
        }.join()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. JOB LIFECYCLE & CANCELLATION
// ─────────────────────────────────────────────────────────────────────────────

class Ch03_JobAndCancellation {

    /**
     * Cancellation is cooperative: the coroutine must check for it.
     * delay(), yield(), and other suspend functions are cancellation points.
     */
    @Test
    fun `08 - cancel a job`() = runBlocking {
        val job = launch {
            repeat(100) { i ->
                // Each iteration is a cancellation point because of delay()
                log("working iteration $i", tag = "WORKER")
                delay(50)
            }
        }
        delay(175)               // let a couple of iterations run (~3 iterations at 50ms each)
        log("cancelling job…", tag = "MAIN")
        job.cancel()
        job.join()               // wait for cancellation to fully propagate
        log("job.isCancelled=${job.isCancelled}", tag = "MAIN")
        assertTrue(job.isCancelled)
    }

    /**
     * isActive lets you check cancellation in CPU-bound loops
     * where there are no natural suspension points.
     */
    @Test
    fun `09 - cooperative cancellation with isActive`() = runBlocking {
        val job = launch(Dispatchers.Default) {
            var i = 0
            // Without isActive check, this loop would IGNORE cancel() calls
            // because there's no suspension point for the runtime to inject CancellationException
            while (isActive) {
                i++
                if (i % 1_000_000 == 0) log("computed $i iterations", tag = "WORKER")
            }
            // We reach here only because isActive became false
            log("loop exited — isActive is now false", tag = "WORKER")
        }
        delay(100)
        log("sending cancel…", tag = "MAIN")
        job.cancelAndJoin()      // cancel() + join() in one call
        log("done", tag = "MAIN")
    }

    /**
     * CancellationException is NOT an error — it's the normal way
     * coroutines signal cancellation. Do not swallow it.
     */
    @Test
    fun `10 - CancellationException is normal`() = runBlocking {
        val job = launch {
            try {
                log("waiting…", tag = "CHILD")
                delay(1000)
            } catch (e: CancellationException) {
                // delay() throws CancellationException when the coroutine is cancelled
                // This is the NORMAL signal — always re-throw it!
                log("caught CancellationException: ${e.message} — re-throwing", tag = "CHILD")
                throw e  // ← must re-throw so the parent knows the child is done
            } finally {
                // finally ALWAYS runs, even during cancellation
                log("finally block — good place for resource cleanup", tag = "CHILD")
            }
        }
        delay(100)
        log("cancelling…", tag = "MAIN")
        job.cancelAndJoin()
        log("job.isCancelled=${job.isCancelled}", tag = "MAIN")
    }

    /**
     * withTimeout throws TimeoutCancellationException if the block
     * doesn't finish in time. withTimeoutOrNull returns null instead.
     */
    @Test
    fun `11 - withTimeout and withTimeoutOrNull`() = runBlocking {
        // Case 1: completes in time → returns the value
        val result = withTimeoutOrNull(200) {
            log("started — 100ms work, 200ms budget", tag = "TIMEOUT")
            delay(100)
            log("finished within budget", tag = "TIMEOUT")
            "finished in time"
        }
        log("result=$result", tag = "MAIN")  // non-null
        assertEquals("finished in time", result)

        // Case 2: exceeds timeout → returns null instead of throwing
        val timedOut = withTimeoutOrNull(50) {
            log("started — 200ms work, 50ms budget", tag = "TIMEOUT")
            delay(200)
            "won't reach here"
        }
        log("timedOut result=$timedOut (null means timed out)", tag = "MAIN")
        assertNull(timedOut)
    }

    /**
     * NonCancellable allows cleanup work inside a cancelled coroutine.
     * Use only inside finally blocks.
     */
    @Test
    fun `12 - NonCancellable for cleanup`() = runBlocking {
        val job = launch {
            try {
                log("doing work…", tag = "CHILD")
                delay(1000)
            } finally {
                // At this point the coroutine is already cancelled.
                // Calling delay() here would throw again — so we wrap
                // critical cleanup in NonCancellable to allow suspension.
                withContext(NonCancellable) {
                    log("cleanup start — NonCancellable lets us suspend here", tag = "CLEANUP")
                    delay(50)                 // e.g. flushing a buffer, closing a socket
                    log("cleanup done", tag = "CLEANUP")
                }
            }
        }
        delay(100)
        log("cancelling…", tag = "MAIN")
        job.cancelAndJoin()
        log("job fully cancelled and cleaned up", tag = "MAIN")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. SEQUENTIAL vs CONCURRENT EXECUTION
// ─────────────────────────────────────────────────────────────────────────────

class Ch04_SequentialVsConcurrent {

    /**
     * Sequential: each suspend call waits for the previous one.
     */
    @Test
    fun `13 - sequential execution`() = runBlocking {
        log("start — sequential: each call blocks until the previous finishes", tag = "SEQ")
        val time = measureTimeMillis {
            val user = fetchUser(1)        // suspends ~500ms before continuing
            val order = fetchOrder(user)    // only starts AFTER user is returned (~300ms)
            log("result: $user → $order", tag = "SEQ")
        }
        log("total time: ${time}ms  ← sum of both delays (~800ms)", tag = "SEQ")
        assertTrue(time >= 800)
    }

    /**
     * Concurrent with async: both network calls run in parallel.
     */
    @Test
    fun `14 - concurrent with async`() = runBlocking {
        log("start — concurrent: both calls launched immediately", tag = "CONCURRENT")
        val time = measureTimeMillis {
            val userDeferred = async { fetchUser(1) }            // starts now
            val orderDeferred = async { fetchOrder("User#1") }    // also starts now
            // Both are running in parallel — we only block at await()
            val user = userDeferred.await()
            val order = orderDeferred.await()
            log("result: $user + $order", tag = "CONCURRENT")
        }
        // Total time ≈ max(500, 300) = ~500ms instead of 800ms
        log("total time: ${time}ms  ← only the longest delay (~500ms)", tag = "CONCURRENT")
        assertTrue(time < 700)
    }

    /**
     * async is lazy: starts only when await() is called (or start() explicitly).
     */
    @Test
    fun `15 - lazy async`() = runBlocking {
        // LAZY means the coroutine is created but NOT started yet
        val deferred = async(start = CoroutineStart.LAZY) {
            log("lazy async — only starts when await() is called", tag = "LAZY")
            delay(100)
            "lazy result"
        }
        log("deferred created — coroutine has NOT started yet", tag = "MAIN")
        delay(50)
        log("still not started — calling await() now", tag = "MAIN")
        val result = deferred.await()   // ← this is what triggers it
        log("got: $result", tag = "MAIN")
        assertEquals("lazy result", result)
    }

    /**
     * coroutineScope creates a child scope. All children must complete
     * before the scope returns. Cancels siblings on failure.
     */
    @Test
    fun `16 - coroutineScope groups concurrent work`() = runBlocking {
        log("entering coroutineScope — both fetches run in parallel", tag = "MAIN")
        val result = coroutineScope {
            // Both async blocks start immediately and run concurrently inside this scope
            val a = async { fetchUser(1) }
            val b = async { fetchUser(2) }
            // coroutineScope won't return until BOTH are done
            "${a.await()} & ${b.await()}"
        }
        // coroutineScope has completed — all children finished
        log("coroutineScope done — result: $result", tag = "MAIN")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. EXCEPTION HANDLING
// ─────────────────────────────────────────────────────────────────────────────

class Ch05_ExceptionHandling {

    /**
     * CoroutineExceptionHandler catches uncaught exceptions in launch.
     * Does NOT work for async — exceptions surface at await().
     */
    @Test
    fun `17 - CoroutineExceptionHandler`() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            // Called on the thread that ran the failing coroutine
            log("handler caught: ${exception.message}", tag = "HANDLER")
        }
        log("launching coroutine that will throw", tag = "MAIN")
        val job = launch(handler) {
            log("about to throw…", tag = "CHILD")
            throw RuntimeException("Something went wrong!")
        }
        job.join()
        log("job.isCancelled=${job.isCancelled}", tag = "MAIN")
    }

    /**
     * async exceptions are deferred until await() is called.
     */
    @Test
    fun `18 - async exception surfaces at await`() = runBlocking {
        log("launching async that will throw", tag = "MAIN")
        val deferred: Deferred<Unit> = async {
            delay(50)
            log("throwing now", tag = "ASYNC")
            throw IllegalStateException("async failure")
        }
        // The exception is STORED inside the Deferred — nothing throws yet
        log("async is running — no exception thrown yet", tag = "MAIN")
        try {
            deferred.await()  // ← exception re-thrown HERE
        } catch (e: IllegalStateException) {
            log("caught at await(): ${e.message}", tag = "MAIN")
        }
    }

    /**
     * SupervisorJob allows children to fail independently.
     * One failing child does NOT cancel siblings.
     */
    @Test
    fun `19 - SupervisorJob isolates child failures`() = runBlocking {
        // With a normal Job: one child failure cancels ALL siblings.
        // SupervisorJob changes that — each child lives or dies on its own.
        val supervisor = SupervisorJob()
        val scope = CoroutineScope(coroutineContext + supervisor)

        val child1 = scope.launch {
            delay(50)
            log("child1 throwing…", tag = "CHILD1")
            throw RuntimeException("child1 failed")
        }
        val child2 = scope.launch {
            delay(200)
            log("child2 completed — survived child1's failure", tag = "CHILD2")
        }

        child1.join()   // wait for child1 to finish (it failed)
        child2.join()   // child2 should still complete normally
        supervisor.cancel()
        log("child1.isCancelled=${child1.isCancelled} child2.isCompleted=${child2.isCompleted}", tag = "MAIN")
        assertTrue(child2.isCompleted)
    }

    /**
     * supervisorScope is a scope that uses SupervisorJob semantics.
     */
    @Test
    fun `20 - supervisorScope`() = runBlocking {
        supervisorScope {
            launch {
                delay(50)
                log("throwing — sibling should NOT be cancelled", tag = "CHILD1")
                throw RuntimeException("oops")
            }
            launch {
                delay(200)
                // This runs even though its sibling threw — SupervisorJob semantics
                log("survived sibling's failure ✓", tag = "CHILD2")
            }
            Unit  // explicit Unit so the function return type is unambiguously Unit
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. SHARED MUTABLE STATE & CONCURRENCY SAFETY
// ─────────────────────────────────────────────────────────────────────────────

class Ch06_SharedState {

    /**
     * Race condition: non-atomic increment is unsafe.
     */
    @Test
    fun `21 - race condition without synchronization (demo)`() = runBlocking {
        var counter = 0
        // 1000 coroutines all read-increment-write the same variable concurrently
        // The read+write is NOT atomic → data is lost when two coroutines interleave
        val jobs = List(1000) {
            launch(Dispatchers.Default) { counter++ }
        }
        jobs.joinAll()
        // Result is non-deterministic — usually less than 1000
        log("unsafe counter=$counter (expected 1000, got less due to race)", tag = "RACE")
    }

    /**
     * AtomicInteger: lock-free, thread-safe integer operations.
     */
    @Test
    fun `22 - AtomicInteger for thread safety`() = runBlocking {
        val counter = AtomicInteger(0)
        val jobs = List(1000) {
            // incrementAndGet() is a single hardware instruction — truly atomic
            launch(Dispatchers.Default) { counter.incrementAndGet() }
        }
        jobs.joinAll()
        log("atomic counter=${counter.get()} (always exactly 1000)", tag = "ATOMIC")
        assertEquals(1000, counter.get())
    }

    /**
     * Mutex: coroutine-aware mutual exclusion. withLock suspends (not blocks).
     */
    @Test
    fun `23 - Mutex for coroutine-safe locking`() = runBlocking {
        val mutex = Mutex()
        var counter = 0
        val jobs = List(1000) {
            launch(Dispatchers.Default) {
                // withLock SUSPENDS (yields the thread) if the lock is taken
                // unlike Java's synchronized which BLOCKS the thread entirely
                mutex.withLock { counter++ }
            }
        }
        jobs.joinAll()
        log("mutex counter=$counter (always exactly 1000)", tag = "MUTEX")
        assertEquals(1000, counter)
    }

    /**
     * Single-threaded dispatcher: confine mutable state to one thread.
     * Simple and effective; no locks needed.
     */
    @Test
    fun `24 - single thread confinement`() = runBlocking {
        // Only one thread ever touches 'counter' → no race condition possible
        val singleThread = newSingleThreadContext("CounterThread")
        var counter = 0
        val jobs = List(1000) {
            launch(Dispatchers.Default) {
                // All mutations are routed to a single thread → no synchronisation needed
                withContext(singleThread) { counter++ }
            }
        }
        jobs.joinAll()
        log("confined counter=$counter (always exactly 1000)", tag = "CONFINED")
        assertEquals(1000, counter)
        singleThread.close()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. CHANNELS
// ─────────────────────────────────────────────────────────────────────────────

class Ch07_Channels {

    /**
     * Channel is a coroutine-safe queue for communicating between coroutines.
     * send() suspends when full; receive() suspends when empty.
     */
    @Test
    fun `25 - basic Channel send and receive`() = runBlocking {
        val channel = Channel<Int>()   // default: rendezvous (no buffer)
        launch {
            for (i in 1..5) {
                log("sending $i", tag = "PRODUCER")
                channel.send(i)        // suspends until consumer calls receive()
            }
            channel.close()            // signals "no more items" — for loop on consumer ends
            log("channel closed", tag = "PRODUCER")
        }
        // for-loop on a Channel calls receive() and stops when the channel is closed
        for (item in channel) {
            log("received $item", tag = "CONSUMER")
        }
    }

    /**
     * Buffered channel: sender can send N items before suspending.
     */
    @Test
    fun `26 - buffered channel`() = runBlocking {
        // capacity=3 means producer can send 3 items without the consumer being ready
        val channel = Channel<Int>(capacity = 3)
        launch {
            repeat(5) {
                log("send $it  (buffer fills up after 3)", tag = "PRODUCER")
                channel.send(it)   // suspends on 4th item until consumer drains
            }
            channel.close()
        }
        delay(100)   // let producer run and fill the buffer
        for (item in channel) log("got $item", tag = "CONSUMER")
    }

    /**
     * produce() builder: convenient producer coroutine returning a ReceiveChannel.
     */
    @Test
    fun `27 - produce builder`() = runBlocking {
        // produce{} is syntactic sugar for launch + Channel — auto-closes on completion
        val squares: ReceiveChannel<Int> = produce {
            for (i in 1..5) {
                log("emitting $i² = ${i * i}", tag = "PRODUCER")
                send(i * i)
            }
        }
        squares.consumeEach { log("square: $it", tag = "CONSUMER") }
    }

    /**
     * Fan-out: multiple consumers from a single channel.
     */
    @Test
    fun `28 - fan-out multiple consumers`() = runBlocking {
        val channel = produce { repeat(10) { send(it) } }
        // Three workers compete for items — each item goes to exactly ONE worker
        repeat(3) { workerId ->
            launch {
                for (item in channel) {
                    log("got item=$item", tag = "WORKER-$workerId")
                    delay(10)
                }
            }
        }
        delay(500)
    }

    /**
     * select expression: await the first of multiple channel operations.
     */
    @Test
    fun `29 - select between channels`() = runBlocking {
        val fast = produce { delay(50); log("fast ready", tag = "FAST"); send("fast") }
        val slow = produce { delay(200); log("slow ready", tag = "SLOW"); send("slow") }

        // select{} returns as soon as ONE of the clauses can proceed
        val winner = select<String> {
            fast.onReceive { value -> log("fast won", tag = "SELECT"); value }
            slow.onReceive { value -> log("slow won", tag = "SELECT"); value }
        }
        log("winner=$winner", tag = "MAIN")
        assertEquals("fast", winner)
        fast.cancel(); slow.cancel()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. FLOWS — COLD ASYNCHRONOUS STREAMS
// ─────────────────────────────────────────────────────────────────────────────

class Ch08_Flows {

    /**
     * flow builder creates a cold stream. Nothing runs until collect() is called.
     */
    @Test
    fun `30 - basic flow`() = runBlocking {
        val myFlow: Flow<Int> = flow {
            for (i in 1..5) {
                log("about to emit $i", tag = "EMITTER")
                emit(i)      // suspends until collector is ready for next value
                delay(50)
            }
        }
        // Nothing has run yet — cold flow only starts when collected
        log("flow defined — collection not started yet", tag = "MAIN")
        myFlow.collect { value ->
            log("collected $value", tag = "COLLECTOR")
        }
        log("collection complete", tag = "MAIN")
    }

    /**
     * Flow operators: map, filter, transform, take, drop — all lazy.
     */
    @Test
    fun `31 - flow operators`() = runBlocking {
        // Each operator wraps the previous flow — nothing runs until collect()
        flowOf(1, 2, 3, 4, 5, 6)
            .filter { it % 2 == 0 }        // keeps 2, 4, 6
            .map { it * it }             // squares: 4, 16, 36
            .take(2)                        // stops after 2 items: 4, 16
            .collect { log("result: $it", tag = "COLLECT") }
    }

    /**
     * flowOn changes the upstream dispatcher without changing the downstream one.
     */
    @Test
    fun `32 - flowOn for upstream context`() = runBlocking {
        flow {
            // flowOn(Default) makes THIS block run on a worker thread
            log("emitting — runs on Default dispatcher", tag = "EMITTER")
            emit(1)
            emit(2)
        }
            .flowOn(Dispatchers.Default)   // ← only affects upstream (the flow{} block)
            .collect { value ->
                // Collector always stays on the downstream dispatcher (runBlocking's thread here)
                log("collecting $value — runs on original thread", tag = "COLLECTOR")
            }
    }

    /**
     * Terminal operators: collect, toList, first, single, reduce, fold.
     */
    @Test
    fun `33 - terminal operators`() = runBlocking {
        val list = flowOf(1, 2, 3).toList()
        log("toList: $list", tag = "TERMINAL")
        assertEquals(listOf(1, 2, 3), list)

        // reduce folds left with no initial value — accumulator starts as first element
        val sum = flowOf(1, 2, 3, 4, 5).reduce { acc, value -> acc + value }
        log("reduce (sum): $sum", tag = "TERMINAL")
        assertEquals(15, sum)

        // first() short-circuits — cancels the flow after the first element
        val first = flowOf(10, 20, 30).first()
        log("first: $first", tag = "TERMINAL")
        assertEquals(10, first)
    }

    /**
     * flatMapConcat — sequential inner flows.
     * flatMapMerge — concurrent inner flows.
     */
    @Test
    fun `34 - flatMap variants`() = runBlocking {
        // flatMapConcat: finishes inner flow A before starting inner flow B
        log("=== flatMapConcat (sequential — A completes before B starts) ===", tag = "FLAT")
        flowOf("A", "B").flatMapConcat { letter ->
            flow {
                log("inner start: ${letter}1", tag = letter)
                emit("${letter}1")
                delay(50)
                log("inner end:   ${letter}2", tag = letter)
                emit("${letter}2")
            }
        }.collect { log("got $it", tag = "COLLECT") }

        // flatMapMerge: all inner flows run concurrently — order may interleave
        log("=== flatMapMerge (concurrent — X and Y run simultaneously) ===", tag = "FLAT")
        flowOf("X", "Y").flatMapMerge { letter ->
            flow {
                log("inner start: ${letter}1", tag = letter)
                emit("${letter}1")
                delay(50)
                log("inner end:   ${letter}2", tag = letter)
                emit("${letter}2")
            }
        }.collect { log("got $it", tag = "COLLECT") }
    }

    /**
     * zip combines two flows pair-wise.
     * combine re-emits whenever EITHER flow emits (uses latest from the other).
     */
    @Test
    fun `35 - zip vs combine`() = runBlocking {
        val nums = flowOf(1, 2, 3)
        val letters = flowOf("A", "B", "C")

        // zip: pairs items 1-to-1; stops when the shorter flow ends
        log("=== zip: strict 1-to-1 pairing ===", tag = "ZIP")
        nums.zip(letters) { n, l -> "$n$l" }.collect { log(it, tag = "ZIP") }

        // combine: re-emits on ANY update, using the LATEST from the other side
        log("=== combine: emits on every update from either side ===", tag = "COMBINE")
        val slow = flow { emit(1); delay(100); emit(2) }
        val fast = flow { delay(50); emit("a"); emit("b") }
        slow.combine(fast) { n, l -> "$n$l" }
            .collect { log(it, tag = "COMBINE") }
    }

    /**
     * catch handles exceptions in the upstream flow.
     */
    @Test
    fun `36 - flow exception handling with catch`() = runBlocking {
        flow {
            emit(1)
            emit(2)
            log("about to throw upstream exception", tag = "EMITTER")
            throw RuntimeException("upstream error")
        }
            // catch{} intercepts upstream exceptions; can emit replacement values
            .catch { e ->
                log("caught: ${e.message} — emitting fallback", tag = "CATCH")
                emit(-1)
            }
            .collect { log("value: $it", tag = "COLLECTOR") }
    }

    /**
     * onCompletion runs after the flow completes (success or error).
     */
    @Test
    fun `37 - onCompletion`() = runBlocking {
        flowOf(1, 2, 3)
            .onCompletion { cause ->
                // cause==null means successful completion; non-null means error
                if (cause == null) log("completed successfully ✓", tag = "COMPLETE")
                else log("failed: $cause", tag = "COMPLETE")
            }
            .collect { log("item: $it", tag = "COLLECT") }
    }

    /**
     * Backpressure strategies:
     * buffer()    — producer and consumer run concurrently; results buffered.
     * conflate()  — collector only gets the latest; intermediate values dropped.
     * collectLatest — cancels previous collector if new value arrives.
     */
    @Test
    fun `38 - buffer conflate collectLatest`() = runBlocking {
        // Producer emits every 20ms; consumer takes 50ms per item (slow consumer)
        val fastFlow = flow {
            repeat(5) { i ->
                log("emitting $i", tag = "PRODUCER")
                emit(i)
                delay(20)
            }
        }

        log("=== buffer: producer and consumer run concurrently ===", tag = "STRATEGY")
        fastFlow.buffer().collect { value ->
            delay(50)   // slow consumer — but producer isn't blocked (buffered)
            log("buffered: processed $value", tag = "CONSUMER")
        }

        log("=== conflate: only latest value is delivered ===", tag = "STRATEGY")
        fastFlow.conflate().collect { value ->
            delay(50)   // slow consumer — intermediate values are DROPPED
            log("conflated: processed $value (some values skipped)", tag = "CONSUMER")
        }

        log("=== collectLatest: restarts collector on each new emission ===", tag = "STRATEGY")
        fastFlow.collectLatest { value ->
            // If a new value arrives before delay(50) finishes, this block is cancelled
            log("collectLatest: starting $value", tag = "CONSUMER")
            delay(50)
            log("collectLatest: finished $value (only last value completes)", tag = "CONSUMER")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. STATE FLOW & SHARED FLOW (HOT FLOWS)
// ─────────────────────────────────────────────────────────────────────────────

class Ch09_HotFlows {

    /**
     * StateFlow: always has a value; emits only distinct values.
     * Replaces LiveData in Kotlin multiplatform.
     */
    @Test
    fun `39 - StateFlow`() = runBlocking {
        // StateFlow is HOT — it always has a value and keeps the latest one
        val stateFlow = MutableStateFlow(0)   // initial value = 0

        val collector = launch {
            // collect() on StateFlow is infinite — must be cancelled explicitly
            stateFlow.collect { value ->
                log("received value=$value", tag = "COLLECTOR")
            }
        }

        delay(50)
        stateFlow.value = 1             // collector gets 1
        delay(50)
        stateFlow.value = 1             // DUPLICATE — StateFlow drops it (distinctUntilChanged)
        log("set value=1 again — duplicate will NOT be re-emitted", tag = "PRODUCER")
        delay(50)
        stateFlow.value = 2             // collector gets 2
        delay(50)

        collector.cancel()
        log("final stateFlow.value=${stateFlow.value}", tag = "MAIN")
    }

    /**
     * SharedFlow: multi-cast, configurable replay and buffer.
     * Does NOT have a current value concept.
     */
    @Test
    fun `40 - SharedFlow`() = runBlocking {
        // replay=1 means new collectors immediately receive the last emitted value
        val sharedFlow = MutableSharedFlow<String>(replay = 1)

        // Two independent collectors — BOTH receive every emission (multicast)
        launch { sharedFlow.collect { log("got: $it", tag = "COLLECTOR-1") } }
        launch { sharedFlow.collect { log("got: $it", tag = "COLLECTOR-2") } }

        delay(50)
        log("emitting Hello", tag = "PRODUCER")
        sharedFlow.emit("Hello")   // both collectors receive "Hello"
        delay(50)
        log("emitting World", tag = "PRODUCER")
        sharedFlow.emit("World")   // both collectors receive "World"
        delay(50)

        coroutineContext.cancelChildren()
    }

    /**
     * stateIn converts a cold Flow into a StateFlow.
     */
    @Test
    fun `41 - stateIn converts cold flow to StateFlow`() = runBlocking {
        val coldFlow = flow {
            var i = 0
            // Cold flow: only runs when collected; emits a counter every 100ms
            while (true) {
                log("cold emitting $i", tag = "COLD")
                emit(i++)
                delay(100)
            }
        }

        // stateIn starts the cold flow eagerly and caches its latest value
        // Any future collector immediately gets the latest cached value
        val stateFlow = coldFlow.stateIn(
            scope = this,
            started = SharingStarted.Eagerly,   // starts immediately, no collectors needed
            initialValue = -1                         // value before first emission
        )

        delay(350)   // let 3-4 emissions happen
        log("current value without collecting: ${stateFlow.value}", tag = "MAIN")
        coroutineContext.cancelChildren()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 10. COROUTINE SCOPES IN PRACTICE
// ─────────────────────────────────────────────────────────────────────────────

class Ch10_CoroutineScopes {

    /**
     * CoroutineScope factory: create a scope you control.
     * Always cancel it when done to avoid leaks.
     */
    @Test
    fun `42 - manual CoroutineScope`() = runBlocking {
        // SupervisorJob: one child failing doesn't cancel the others
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        scope.launch { delay(100); log("Task 1 done", tag = "SCOPE") }
        scope.launch { delay(200); log("Task 2 done", tag = "SCOPE") }

        delay(300)
        scope.cancel()   // cancels all running children and prevents new launches
        log("scope cancelled — all children stopped", tag = "MAIN")
    }

    /**
     * Encapsulating scope in a class: the component owns its scope and
     * cancels it when no longer needed (like ViewModel.onCleared()).
     */
    class WorkManager : CoroutineScope by CoroutineScope(
        Dispatchers.Default + SupervisorJob()
    ) {
        fun doWork() = launch {
            delay(100)
            // The class's scope controls this coroutine's lifetime
            log("WorkManager doing work", tag = "WORK-MANAGER")
        }

        fun shutdown() {
            log("WorkManager shutting down — cancelling scope", tag = "WORK-MANAGER")
            cancel()   // cancels the scope and all coroutines launched in it
        }
    }

    @Test
    fun `43 - class owns its CoroutineScope`() = runBlocking {
        val manager = WorkManager()
        log("calling doWork()", tag = "TEST")
        manager.doWork().join()
        manager.shutdown()
        log("manager shut down", tag = "TEST")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 11. TESTING COROUTINES WITH kotlinx-coroutines-test
// ─────────────────────────────────────────────────────────────────────────────

class Ch11_TestingCoroutines {

    /**
     * runTest replaces runBlocking for tests.
     * It uses a TestCoroutineScheduler that controls virtual time.
     * delay() calls are skipped instantly — tests run in milliseconds.
     */
    @Test
    fun `44 - runTest skips delays`() = runTest {
        // This delay would take 10 seconds in real life — runTest skips it instantly
        log("before 10-second delay (virtual time)", tag = "TEST")
        val result = withContext(Dispatchers.Default) {
            delay(10_000)   // ← skipped instantly by the test scheduler
            "done"
        }
        log("after delay — currentTime=${currentTime}ms (virtual)", tag = "TEST")
        assertEquals("done", result)
    }

    /**
     * advanceTimeBy moves virtual time forward by N milliseconds.
     */
    @Test
    fun `45 - advanceTimeBy for precise time control`() = runTest {
        var fired = false
        launch {
            delay(1000)
            fired = true
            log("fired at virtual time=${currentTime}ms", tag = "CHILD")
        }
        assertFalse(fired)
        advanceTimeBy(999)   // move to 999ms — delay(1000) not yet elapsed
        log("at 999ms — fired=$fired", tag = "TEST")
        assertFalse(fired)
        advanceTimeBy(1)     // move to exactly 1000ms — delay completes
        log("at 1000ms — fired=$fired", tag = "TEST")
        assertTrue(fired)
    }

    /**
     * advanceUntilIdle runs all pending coroutines to completion.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `46 - advanceUntilIdle`() = runTest {
        var count = 0
        launch { delay(100); count++; log("child1 done count=$count", tag = "CHILD") }
        launch { delay(200); count++; log("child2 done count=$count", tag = "CHILD") }
        launch { delay(300); count++; log("child3 done count=$count", tag = "CHILD") }

        log("before advanceUntilIdle — count=$count", tag = "TEST")
        assertEquals(0, count)
        advanceUntilIdle()   // runs ALL pending coroutines to completion instantly
        log("after advanceUntilIdle  — count=$count", tag = "TEST")
        assertEquals(3, count)
    }

    /**
     * TestScope.backgroundScope: coroutines that don't need to complete before
     * the test ends (e.g., infinite collectors).
     */
    @Test
    fun `47 - backgroundScope for infinite flows`() = runTest {
        val stateFlow = MutableStateFlow(0)
        val collected = mutableListOf<Int>()

        backgroundScope.launch {
            stateFlow.collect { collected.add(it) }
        }

        stateFlow.value = 1
        stateFlow.value = 2
        advanceUntilIdle()

        assertTrue(collected.containsAll(listOf(0, 1, 2)))
    }

    /**
     * UnconfinedTestDispatcher: collectors start immediately without needing
     * explicit advancement. Useful for StateFlow / SharedFlow tests.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `48 - UnconfinedTestDispatcher eager collection`() = runTest(UnconfinedTestDispatcher()) {
        val flow = MutableStateFlow(0)
        val values = mutableListOf<Int>()

        backgroundScope.launch { flow.collect { values.add(it) } }

        flow.value = 1
        flow.value = 2
        flow.value = 3

        assertEquals(listOf(0, 1, 2, 3), values)
    }

    /**
     * Testing flows with toList() — collects a finite flow into a list.
     */
    @Test
    fun `49 - testing a finite flow with toList`() = runTest {
        val flow = flow {
            emit(1); delay(100)
            emit(2); delay(100)
            emit(3)
        }
        // toList() is a terminal operator — collects everything into memory
        // delays are virtual so this runs instantly
        val result = flow.toList()
        log("collected: $result  virtualTime=${currentTime}ms", tag = "TEST")
        assertEquals(listOf(1, 2, 3), result)
    }

    /**
     * Testing a class that uses coroutines internally.
     * Inject TestScope / TestDispatcher to control time.
     */
    class DataRepository(private val scope: CoroutineScope) {
        private val _data = MutableStateFlow<String?>(null)
        val data: StateFlow<String?> = _data.asStateFlow()

        fun loadData() {
            scope.launch {
                delay(500)                  // simulated network call
                _data.value = "loaded data"
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `50 - testing a class with injected TestScope`() = runTest {
        // Inject `this` (the TestScope) so the repository's delays are virtual
        val repo = DataRepository(this)

        log("data before load: ${repo.data.value}", tag = "TEST")
        assertNull(repo.data.value)
        repo.loadData()
        // Jump virtual time past the 500ms delay
        advanceUntilIdle()
        log("data after advanceUntilIdle: ${repo.data.value}", tag = "TEST")
        assertEquals("loaded data", repo.data.value)
    }

    /**
     * Testing exception propagation in coroutines.
     */
    @Test
    fun `51 - asserting exceptions from async`() = runTest {
        val deferred = async {
            delay(100)
            log("throwing IllegalArgumentException", tag = "ASYNC")
            throw IllegalArgumentException("bad input")
        }
        // Exception is stored in the Deferred — only thrown when await() is called
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { deferred.await() }
        }
        log("exception correctly propagated from async", tag = "TEST")
    }

    /**
     * Testing cancellation behaviour.
     */
    @Test
    fun `52 - testing cancellation`() = runTest {
        var completed = false
        val job = launch {
            try {
                log("waiting 10 seconds (virtual)", tag = "CHILD")
                delay(10_000)
                completed = true   // should never reach here if cancelled
            } catch (e: CancellationException) {
                log("cancelled at virtualTime=${currentTime}ms", tag = "CHILD")
            }
        }
        advanceTimeBy(100)      // move virtual time — coroutine is mid-delay
        log("cancelling job at virtual 100ms", tag = "TEST")
        job.cancel()
        job.join()
        log("completed=$completed (expected false)", tag = "TEST")
        assertFalse(completed)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top-level helpers for Ch12
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Parallel map: run suspend transformations concurrently with a concurrency limit.
 * The Semaphore acts as a gate — only [concurrency] coroutines hold a permit at once.
 */
suspend fun <T, R> Iterable<T>.parallelMap(
    concurrency: Int = Int.MAX_VALUE,
    transform: suspend (T) -> R
): List<R> = coroutineScope {
    val semaphore = Semaphore(concurrency)
    map { item ->
        async {
            semaphore.withPermit {
                log("transforming $item (slot acquired)", tag = "PARALLEL")
                transform(item)
            }
        }
    }.awaitAll()  // collect all results in original order
}

class Ch12_RealWorldPatterns {

    // ── 12. REAL-WORLD PATTERNS ─────────────────────────────────────────────

    /**
     * Retry with exponential back-off: a common pattern for network calls.
     */
    suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 100,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                log("attempt ${attempt + 1} failed: ${e.message} — retrying in ${currentDelay}ms", tag = "RETRY")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()   // double the wait each time
        }
        log("final attempt", tag = "RETRY")
        return block()   // last attempt — let exception propagate if it fails
    }

    @Test
    fun `53 - retry with exponential backoff`() = runTest {
        var attempts = 0
        val result = retryWithBackoff(times = 3, initialDelay = 10) {
            attempts++
            log("attempt $attempts", tag = "CALL")
            if (attempts < 3) throw RuntimeException("transient error")
            "success on attempt $attempts"
        }
        log("final result: $result", tag = "TEST")
        assertEquals("success on attempt 3", result)
    }

    /**
     * Debounce: wait for a pause in events before acting.
     * Useful for search-as-you-type UI.
     */
    fun <T> Flow<T>.debounce(windowMs: Long): Flow<T> = channelFlow {
        var debounceJob: Job? = null
        collect { value ->
            // Cancel the pending send — the user is still typing
            debounceJob?.cancel()
            debounceJob = launch {
                delay(windowMs)   // wait for the pause
                send(value)       // only fires if no new value arrived during the window
            }
        }
    }

    @Test
    fun `54 - debounce operator`() = runTest {
        val results = mutableListOf<String>()
        flow {
            // Rapid emissions — only "hello" and "world" survive the 100ms debounce
            emit("h"); delay(50)
            emit("he"); delay(50)
            emit("hel"); delay(50)
            emit("hell"); delay(50)
            emit("hello"); delay(200)   // ← 200ms pause → debounce fires with "hello"
            emit("world"); delay(200)   // ← 200ms pause → debounce fires with "world"
        }
            .debounce(100)
            .collect { value ->
                log("debounced emission: $value", tag = "DEBOUNCE")
                results.add(value)
            }

        log("all debounced results: $results", tag = "TEST")
        assertEquals(listOf("hello", "world"), results)
    }

    /**
     * Polling: produce values at regular intervals using a flow.
     */
    fun pollEvery(intervalMs: Long, block: suspend () -> String): Flow<String> = flow {
        while (true) {
            val value = block()
            log("poll result: $value", tag = "POLL")
            emit(value)
            delay(intervalMs)
        }
    }

    @Test
    fun `55 - polling flow`() = runTest {
        var count = 0
        pollEvery(100) { "tick ${++count}" }
            .take(3)         // stop after 3 emissions — cancels the infinite loop
            .collect { value -> log("got: $value", tag = "TEST") }
        log("total polls: $count", tag = "TEST")
        assertEquals(3, count)
    }

    @Test
    fun `56 - parallel map with concurrency limit`() = runTest {
        val ids = listOf(1, 2, 3, 4, 5)
        log("starting parallelMap — max 2 concurrent transforms", tag = "TEST")
        val users = ids.parallelMap(concurrency = 2) { fetchUser(it) }
        log("all users: $users", tag = "TEST")
        assertEquals(5, users.size)
    }
}
