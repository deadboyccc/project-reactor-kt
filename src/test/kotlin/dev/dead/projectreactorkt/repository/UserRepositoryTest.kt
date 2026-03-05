package dev.dead.projectreactorkt.repository

import dev.dead.projectreactorkt.model.User
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
class UserRepositoryTest @Autowired constructor(
    val userRepository: UserRepository,
    val databaseClient: DatabaseClient,
) {

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 0 — Test setup
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeEach
    fun cleanUp() = runTest {
        userRepository.deleteAll()
    }

    private suspend fun saveUser(
        name: String = "Alice",
        email: String = "alice@example.com",
        age: Int = 30
    ) = userRepository.save(User(name = name, email = email, age = age))

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 1 — runTest: the ONLY correct way to test coroutines
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * runTest { } replaces runBlocking { } in tests.
     *
     * WHY NOT runBlocking?
     *   runBlocking blocks the real thread. In tests this can cause deadlocks
     *   when the test runner and coroutines share the same thread pool.
     *   It also doesn't support virtual time (delay() will actually wait).
     *
     * WHY runTest?
     *   - Runs on a TestCoroutineScheduler with VIRTUAL TIME
     *   - delay(1_000) completes instantly — no real waiting
     *   - Fails the test if any uncaught exception is thrown in a child coroutine
     *   - Detects "leaked" coroutines that outlive the test block
     */
    @Test
    fun `basic CRUD - save and findById`() = runTest {
        val saved = saveUser()

        assertNotNull(saved.id)

        val found = userRepository.findById(saved.id!!)
        assertNotNull(found)
        assertEquals("Alice", found.name)
        assertEquals("alice@example.com", found.email)
    }

    @Test
    fun `findById returns null for missing record`() = runTest {
        val result = userRepository.findById(99999L)
        assertNull(result)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 2 — Flow: collecting reactive streams
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Flow<T> is Kotlin's cold stream — nothing runs until .collect() is called.
     * It is the coroutine equivalent of Flux<T> in Project Reactor.
     *
     * "Cold" means: a new execution runs for EACH collector.
     * Contrast with SharedFlow / StateFlow which are "hot" (shared among collectors).
     *
     * Useful terminal operators:
     *   .toList()       — collect all into a List (suspends until stream ends)
     *   .first()        — first element (suspends)
     *   .firstOrNull()  — first or null
     *   .count()        — count elements
     *   .collect { }   — custom action per element
     *   .onEach { }    — side-effect without consuming
     */
    @Test
    fun `findAll returns a Flow of all users`() = runTest {
        saveUser("Alice", "alice@test.com", 30)
        saveUser("Bob", "bob@test.com", 25)
        saveUser("Carol", "carol@test.com", 35)

        val users = userRepository.findAll().toList()
        assertEquals(3, users.size)
    }

    @Test
    fun `findAllByName - derived query returns Flow`() = runTest {
        saveUser("Alice", "alice1@test.com", 30)
        saveUser("Alice", "alice2@test.com", 25)
        saveUser("Bob", "bob@test.com", 28)

        val alices = userRepository.findAllByName("Alice").toList()
        assertEquals(2, alices.size)
        assertTrue(alices.all { it.name == "Alice" })
    }

    @Test
    fun `Flow intermediate operators - filter and map before collect`() = runTest {
        saveUser("Alice", "alice@test.com", 17)
        saveUser("Bob", "bob@test.com", 25)
        saveUser("Carol", "carol@test.com", 30)

        val adults = userRepository.findAll()
            .filter { it.age >= 18 }
            .map { it.name.uppercase() }
            .toList()

        assertEquals(listOf("BOB", "CAROL"), adults.sorted())
    }

    @Test
    fun `custom @Query with age range`() = runTest {
        saveUser("Teen", "teen@test.com", 16)
        saveUser("Young", "young@test.com", 22)
        saveUser("Mid", "mid@test.com", 35)
        saveUser("Senior", "senior@test.com", 60)

        val results = userRepository.findAllByAgeBetween(20, 40).toList()
        assertEquals(2, results.size)
        assertTrue(results.map { it.name }.containsAll(listOf("Young", "Mid")))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 3 — Structured Concurrency: async / await
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Structured Concurrency guarantees:
     *   1. A parent coroutine cannot finish until ALL children finish
     *   2. If a child fails, ALL siblings are cancelled and the parent fails
     *   3. Cancellation propagates downward through the tree
     *
     * coroutineScope { } — creates a child scope. All async {} inside MUST complete
     *   before coroutineScope returns. If ANY child throws, all others are cancelled.
     *
     * async { } + .await() — parallel execution returning a value (Deferred<T>)
     * launch { }           — parallel execution returning Unit (fire-and-forget)
     *
     * SERIAL:   val a = repo.findById(1); val b = repo.findById(2)  → 20ms total
     * PARALLEL: coroutineScope { async{findById(1)}; async{findById(2)} } → ~10ms total
     */
    @Test
    fun `parallel async - fetch two users simultaneously`() = runTest {
        val u1 = saveUser("Alice", "alice@test.com", 30)
        val u2 = saveUser("Bob", "bob@test.com", 25)

        val (foundAlice, foundBob) = coroutineScope {
            val deferredAlice = async { userRepository.findById(u1.id!!) }
            val deferredBob = async { userRepository.findById(u2.id!!) }
            deferredAlice.await() to deferredBob.await()
        }

        assertEquals("Alice", foundAlice?.name)
        assertEquals("Bob", foundBob?.name)
    }

    @Test
    fun `awaitAll - parallel fetch of N items`() = runTest {
        val saved = listOf(
            saveUser("A", "a@test.com", 20),
            saveUser("B", "b@test.com", 21),
            saveUser("C", "c@test.com", 22),
        )

        val found: List<User?> = coroutineScope {
            saved.map { user ->
                async { userRepository.findById(user.id!!) }
            }.awaitAll()
        }

        assertEquals(3, found.filterNotNull().size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 4 — supervisorScope: isolated failure handling
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * coroutineScope:   one failure cancels ALL siblings
     * supervisorScope:  one failure is ISOLATED — siblings keep running
     *
     * Use supervisorScope when partial failure is acceptable:
     *   e.g. fetching from 3 APIs — if one 404s, still show the other 2
     */
    @Test
    fun `supervisorScope - one failure does not cancel siblings`() = runTest {
        val alice = saveUser("Alice", "alice@test.com", 30)
        val results = mutableListOf<String>()

        supervisorScope {
            val failing = async {
                val user = userRepository.findById(99999L)
                    ?: throw RuntimeException("User not found!")
                user.name
            }
            val succeeding = async {
                userRepository.findById(alice.id!!)?.name ?: "missing"
            }

            results += try {
                failing.await()
            } catch (e: Exception) {
                "error: ${e.message}"
            }
            results += succeeding.await()
        }

        assertEquals(listOf("error: User not found!", "Alice"), results)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 5 — Flow error handling
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * .catch { e -> emit(fallback) }   — catches upstream exceptions, can re-emit fallback
     * .onCompletion { e -> }           — runs when flow ends (normally or with error)
     * .retry(n) { e -> bool }          — retries upstream on exception
     * .retryWhen { e, attempt -> }     — full retry control with delay support
     *
     * Note: .catch only catches UPSTREAM exceptions (before it in the chain).
     * Exceptions thrown inside .collect { } are NOT caught by .catch.
     */
    @Test
    fun `Flow catch operator - recover from upstream error`() = runTest {
        val errorFlow = flow<User> {
            emit(saveUser("Alice", "alice@test.com", 30))
            throw RuntimeException("Database exploded!")
        }

        val results = errorFlow
            .catch { e ->
                println("Caught: ${e.message} — emitting fallback")
                emit(User(name = "FALLBACK", email = "none", age = 0))
            }
            .toList()

        assertEquals(2, results.size)
        assertEquals("FALLBACK", results.last().name)
    }

    @Test
    fun `Flow retry - attempt upstream again on failure`() = runTest {
        var attempts = 0

        val flakyFlow = flow {
            attempts++
            if (attempts < 3) throw RuntimeException("Flaky error (attempt $attempts)")
            emit("success after $attempts attempts")
        }

        val result = flakyFlow
            .retry(3) { e -> e is RuntimeException }
            .first()

        assertEquals("success after 3 attempts", result)
        assertEquals(3, attempts)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 6 — Flow backpressure and buffering
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Flow handles backpressure via suspension by default:
     *   Producer suspends when consumer is slow → no data loss, no OOM.
     *
     * .buffer(capacity)   — producer runs ahead; items queued up to capacity
     * .conflate()         — skip intermediate values, keep only the latest
     * .collectLatest { }  — cancel current collection block if new value arrives
     *
     * flowOn(Dispatcher)  — changes dispatcher for UPSTREAM only.
     *   The collector (downstream) stays on its original dispatcher.
     */
    @Test
    fun `Flow buffer - decouple producer and consumer`() = runTest {
        val received = mutableListOf<Int>()

        flow { repeat(5) { emit(it) } }
            .buffer(3)
            .collect { value -> received += value }

        assertEquals(listOf(0, 1, 2, 3, 4), received)
    }

    @Test
    fun `Flow conflate - keep only the latest value`() = runTest {
        val collected = mutableListOf<Int>()

        flow { repeat(10) { emit(it) } }
            .conflate()
            .onEach { delay(10) }
            .collect { collected += it }

        assertTrue(collected.size < 10)
        println("Conflate received: $collected")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 7 — Virtual time with runTest
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * runTest uses a TestCoroutineScheduler — delay() does NOT block real time.
     *
     * advanceTimeBy(ms)   — advance virtual clock by N milliseconds
     * advanceUntilIdle()  — run all pending coroutines to completion
     * currentTime         — read current virtual clock value
     */
    @Test
    fun `virtual time - delay does not actually wait`() = runTest {
        val log = mutableListOf<String>()

        launch {
            delay(1_000)
            log += "after 1s"
        }
        launch {
            delay(5_000)
            log += "after 5s"
        }

        advanceUntilIdle()

        assertEquals(listOf("after 1s", "after 5s"), log)
        assertEquals(5_000L, currentTime)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 8 — Cancellation and timeouts
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Coroutines are COOPERATIVE — they only cancel at suspension points.
     * A tight CPU loop will NOT cancel until it hits delay()/yield()/IO.
     *
     * withTimeout(ms)       — throws TimeoutCancellationException if exceeded
     * withTimeoutOrNull(ms) — returns null instead of throwing
     */
    @Test
    fun `withTimeout - cancel slow operation`() = runTest {
        val result = withTimeoutOrNull(500) {
            delay(1_000)
            "completed"
        }
        assertNull(result)
    }

    @Test
    fun `withTimeout - fast operation completes fine`() = runTest {
        val result = withTimeoutOrNull(500) {
            delay(100)
            "completed"
        }
        assertEquals("completed", result)
    }

    @Test
    fun `cancellation propagates - parent cancel kills children`() = runTest {
        val log = mutableListOf<String>()

        val parent = launch {
            launch {
                try {
                    delay(10_000)
                    log += "child completed"
                } catch (e: CancellationException) {
                    log += "child cancelled"
                    throw e   // ALWAYS re-throw CancellationException!
                }
            }
            delay(100)
        }

        parent.cancelAndJoin()
        assertTrue(log.contains("child cancelled"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 9 — Dispatcher awareness
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dispatchers.Default     — CPU pool (cores count). For CPU-bound work.
     * Dispatchers.IO          — expandable pool (64 threads). For blocking IO.
     * Dispatchers.Unconfined  — caller's thread until first suspension. Avoid in prod.
     *
     * withContext(Dispatcher) — switch for a block, returns to original after.
     *
     * In Spring WebFlux + R2DBC: NEVER block Netty's event loop thread.
     * All suspend functions are non-blocking by design.
     * If you must call legacy blocking code → wrap in withContext(Dispatchers.IO).
     */
    @Test
    fun `withContext - switch to IO for blocking call`() = runTest {
        val saved = saveUser()

        val name = withContext(Dispatchers.IO) {
            Thread.sleep(10)   // Blocking! Only safe inside Dispatchers.IO
            "fetched: ${saved.name}"
        }

        assertEquals("fetched: Alice", name)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 10 — Combining flows
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `zip two flows together`() = runTest {
        saveUser("Alice", "alice@test.com", 30)
        saveUser("Bob", "bob@test.com", 25)

        val namesFlow = userRepository.findAll().map { it.name }
        val emailsFlow = userRepository.findAll().map { it.email }

        // zip pairs by position: (name[0], email[0]), (name[1], email[1])
        val pairs = namesFlow.zip(emailsFlow) { name, email -> "$name → $email" }.toList()

        println("Zipped: $pairs")
        assertEquals(2, pairs.size)
    }

    @Test
    fun `flatMapMerge - fan-out concurrent subflows`() = runTest {
        saveUser("Alice", "alice@test.com", 30)
        saveUser("Bob", "bob@test.com", 25)

        // For each user, launch a subflow CONCURRENTLY (not sequentially)
        val results = userRepository.findAll()
            .flatMapMerge(concurrency = 4) { user ->
                flow {
                    delay(10)
                    emit("${user.name} (age ${user.age})")
                }
            }
            .toList()
            .sorted()

        assertEquals(listOf("Alice (age 30)", "Bob (age 25)"), results)
    }

    @Test
    fun `modifying query returns rows affected`() = runTest {
        val saved = saveUser("Alice", "alice@test.com", 30)

        val rowsUpdated = userRepository.updateNameById(saved.id!!, "Alicia")
        assertEquals(1, rowsUpdated)

        val updated = userRepository.findById(saved.id!!)
        assertEquals("Alicia", updated?.name)
    }
}
