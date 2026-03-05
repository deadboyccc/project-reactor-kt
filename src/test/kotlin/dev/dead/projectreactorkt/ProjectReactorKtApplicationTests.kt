package dev.dead.projectreactorkt

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import java.time.Duration
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class ProjectReactorKtApplicationTests {

    /**
     * 1. THE BRIDGE: FLUX TO FLOW
     * Reactor is "Hot/Cold Push", Flow is "Cold Pull".
     * Conversion handles backpressure automatically.
     */
    @Test
    fun `test flow from flux with operators`() = runBlocking {
        val flux = Flux.range(1, 5).delayElements(Duration.ofMillis(50))

        val flow = flux.asFlow() // Conversion
            .map { it * 10 }
            .filter { it > 20 }

        flow.collect { value ->
            println("Collected from Flux: $value")
        }
    }

    /**
     * 2. THE REVERSE BRIDGE: FLOW TO PUBLISHER
     * Useful when you have a Coroutine-based service but a Reactor-based WebFilter/Security layer.
     */
    @Test
    fun `test flow back to flux`() {
        val flow = flow {
            emit("A")
            delay(10)
            emit("B")
        }

        val flux = flow.asPublisher().let { Flux.from(it) }

        flux.test() // Using reactor-test StepVerifier logic
            .expectNext("A", "B")
            .verifyComplete()
    }

    /**
     * 3. STRUCTURED CONCURRENCY: ASYNC AND AWAIT
     * Coroutines won't "leak" because they are bound to the scope.
     */
    @Test
    fun `test parallel decomposition with async`() = runBlocking {
        val time = measureTimeMillis {
            val one = async { doWork(100) }
            val two = async { doWork(150) }

            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms") // Should be ~150ms, not 250ms
    }

    /**
     * 4. FLOW OPERATORS: CONFLATION AND BUFFERING
     * Handling "fast producers" and "slow consumers".
     */
    @Test
    fun `test flow buffering`() = runBlocking {
        val fastFlow = flow {
            repeat(5) {
                delay(100) // Producer produces every 100ms
                emit(it)
            }
        }

        val time = measureTimeMillis {
            fastFlow
                .buffer() // Allows producer to keep going while consumer is busy
                .collect {
                    delay(300) // Consumer takes 300ms
                    println("Processed $it")
                }
        }
        println("Collected in $time ms")
    }

    /**
     * 5. CONTEXT PRESERVATION: WITHCONTEXT VS FLOWON
     * In Reactor, you use publishOn/subscribeOn. In Coroutines, you use flowOn.
     */
    @Test
    fun `test context switching`() = runBlocking {
        flow {
            println("Emitting on ${Thread.currentThread().name}")
            emit(1)
        }
            .flowOn(Dispatchers.IO) // Changes the upstream context
            .map {
                println("Mapping on ${Thread.currentThread().name}")
                it
            }
            .collect {
                println("Collecting on ${Thread.currentThread().name}")
            }
    }

    /**
     * 6. MONO INTEROP
     * Suspends until the Mono completes without blocking the thread.
     */
    @Test
    fun `test mono await`() = runBlocking {
        val mono = Mono.just("Hello from Reactor")
            .delayElement(Duration.ofSeconds(1))

        // This is the "magic" extension function that suspends
        val result = mono.awaitFirst()
        println("Result: $result")
    }

    private suspend fun doWork(ms: Long): Int {
        delay(ms)
        return (ms / 10).toInt()
    }
    private fun getRandomIntFlow(): Flow<Int> = flow {
        repeat(20) {
            emit(Random.nextInt(1, 100))
            delay(100)
        }

    }

    @Test
    fun getFlowOfIntsAndRunInParallel(): Unit = runBlocking {
        getRandomIntFlow().filter { it > 10 }

            .flowOn(Dispatchers.IO)
            .conflate()
            .onCompletion { println("Completed") }
            .collectIndexed { index, value ->
                println("Collected $index: $value")
            }

    }

    @Test
    fun testRunTestingDelay() = runTest {
        val measureTimeMillis = measureTimeMillis {

            delay(5000)
            println("Test done")
        }
        println("Time : ${measureTimeMillis}ms")
        assertTrue(measureTimeMillis < 5000)

    }

    @Test
    fun testCancellation() {
        // We expect the original cause (IllegalStateException), not the side-effect (Cancellation)
        assertThrows<IllegalStateException> {
            runBlocking {
                // Child 1: Will be cancelled when Child 2 fails
                launch {
                    repeat(100) {
                        println("Test $it")
                        delay(80)
                    }
                }

                // Child 2: Fails and triggers scope-wide cancellation
                launch {
                    delay(200)
                    throw IllegalStateException()
                }
            }
        }
    }
}
