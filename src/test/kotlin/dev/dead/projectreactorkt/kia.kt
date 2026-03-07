package dev.dead.projectreactorkt

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val reactorStartTime = System.currentTimeMillis()

fun logThreadInfo(message: Any?, tag: String = "") {
    val elapsed = System.currentTimeMillis() - reactorStartTime
    val thread = Thread.currentThread().name
    val prefix = if (tag.isNotBlank()) "[$tag] " else ""
    println("%6dms [%-45s] %s%s".format(elapsed, thread, prefix, message ?: "null"))
}

class Kia {

    // Initialize the Mutex here
    private val mutex = Mutex()

    @Test
    fun testLogging() {
        logThreadInfo("This is a test log message", "TEST")
        Thread.sleep(50)
        logThreadInfo("Another message after some work", "TEST")
    }

    @Test
    fun main() {
        logThreadInfo("Starting the application", "INIT")
        Thread.sleep(100)
        logThreadInfo("Application is running", "STATUS")
        Thread.sleep(200)
        logThreadInfo("Application is shutting down", "SHUTDOWN")
    }

    @Test
    fun dumbyAssert() {
        assert(true)
    }

    @Test
    fun runBlockingDemoss() = runBlocking {
        logThreadInfo("The first, parent, coroutine starts")

        launch {
            logThreadInfo("The second coroutine starts and is ready to be suspended")
            delay(100.milliseconds)
            logThreadInfo("The second coroutine is resumed")
        }

        launch {
            logThreadInfo("The third coroutine can run in the meantime")
        }

        logThreadInfo("The first coroutine has launched two more coroutines")
    }

    @Test
    fun testAsyncComputation() = runBlocking {
        logThreadInfo("Starting the async computation")

        val myFirstDeferred = async { slowlyAddNumbers(2, 2) }
        val mySecondDeferred = async { slowlyAddNumbers(4, 4) }

        logThreadInfo("Waiting for the deferred value to be available")
        logThreadInfo("The first result: ${myFirstDeferred.await()}")
        logThreadInfo("The second result: ${mySecondDeferred.await()}")
    }

    suspend fun slowlyAddNumbers(a: Int, b: Int): Int {
        logThreadInfo("Waiting a bit before calculating $a + $b")
        delay(100.milliseconds * a)
        return a + b
    }

    @Test
    fun testAsyncMultiplication() = runBlocking {
        logThreadInfo("Starting the async multiplication")
        val a = async { slowlyMultiplyNumbers(2, 2) }
        val b = async { slowlyMultiplyNumbers(4, 4) }
        logThreadInfo("Waiting for the multiplication")
        logThreadInfo("The first result: ${a.await()}")
        logThreadInfo("The second result: ${b.await()}")

        val c = launch { slowlyMultiplyNumbers(4, 4) }
        logThreadInfo("the job : $c : active=${c.isActive}")
        c.join() // Best practice: wait for the launched job to finish
    }

    suspend fun slowlyMultiplyNumbers(a: Int, b: Int): Int {
        logThreadInfo("Waiting a bit after calculating $a * $b")
        delay(2.seconds)
        return a * b
    }

    @Test
    fun testRaceConditionWithRunBlock(): Unit = runBlocking {
        logThreadInfo("Starting the race condition")
        // Dispatchers.Default uses a thread pool, enabling true parallelism
        launch(Dispatchers.Default) {
            var x = 0L
            repeat(10_000) {
                x++
            }
            logThreadInfo("The first result: $x")
        }
    }

    @Test
    fun failTestRaceConditionWithRunBlock(): Unit = runBlocking {
        logThreadInfo("Starting the intentional race condition fail")
        var x = 0L
        val jobs = List(10_000) {
            launch(Dispatchers.Default) {
                x++ // Critical section unprotected
            }
        }
        jobs.joinAll()
        logThreadInfo("The result (likely < 10000): $x")
    }


    @Test
    fun mutexLockCriticalSection(): Unit = runBlocking {
        logThreadInfo("Starting the mutex lock")
        var x = 0L
        val jobs = List(10_000) {
            launch(Dispatchers.Default) {
                mutex.withLock {
                    x++ // Protected critical section only 1 coroutines gets to run here atomically
                }
            }
        }
        jobs.joinAll()
        withContext(Dispatchers.IO)
        {
            repeat(3) {
                launch {

                    logThreadInfo("running in IO context ", "IO DIS")
                }
            }
        }
        logThreadInfo("The result (guaranteed 10000): $x")
    }

    @Test
    fun seeContextDemo(): Unit = runBlocking(Dispatchers.Default + CoroutineName("test_coroutine")) {
        logThreadInfo("Starting the see context")
        interospectCoroutine()
        logThreadInfo("ending the see context")
    }

    suspend fun interospectCoroutine() {
        logThreadInfo("Context : ${Thread.currentThread().name} -> ${currentCoroutineContext()}")
    }

    @Test
    fun usingAtomicInt(): Unit = runBlocking {
        logThreadInfo("Starting the usingAtomicInt")
        val x = AtomicInteger(0)
        val jobs = List(10_000) {
            launch(Dispatchers.Default) {
                x.getAndIncrement()
            }
        }
        jobs.joinAll()
        logThreadInfo("The result is ${x.get()}")
    }

    @Test
    fun testList() {
        val a = List(10) {
            Random.nextInt(10)
        }
        println(a)

    }

    // Structured Concurrency
    @Test
    fun `Executors with launch`() = runBlocking {
        val t = measureTimeMillis {
            // 1. Create the Executor
            val executor = Executors.newVirtualThreadPerTaskExecutor()

            // 2. Convert it to a CoroutineDispatcher
            val dispatcher = executor.asCoroutineDispatcher()

            val jobs = List(10_000) {
                // 3. Explicitly use the virtual thread dispatcher
                launch(dispatcher) {
                    delay(1.seconds)
                }
            }

            jobs.joinAll()

            // 4. Best practice: close the executor/dispatcher to free resources
            executor.close()
        }
        println("Total time: ${t}ms")
    }

    @Test
    fun testStructuredConcurrencySum(): Unit = runBlocking() {

        logThreadInfo("Computing a sum...")

        // suspending 1
        // suspending function that can suspect and waits for all children to finish before returning job complete
        val sum = coroutineScope {
            val a = async { generateValue() }
            val b = async { generateValue() }

            logThreadInfo("The result is A: ${a.await()} B: ${b.await()}")
            // why is it not yielding the thread to the 2nd suspend ? and while it's waiting why it's not yielding to the second suspedning 2
            yield()
            launch() {
                delay(3.seconds)
            }
            a.await() + b.await()
        }

        logThreadInfo("Sum is $sum")
        // suspending 2
        launch {
            println("Hello this works")
        }
        println("helllllllllllllllllo")
    }

    suspend fun generateValue(): Int {
        delay(500.milliseconds)
        return Random.nextInt(10)
    }

    @Test
    fun `demo scope`(): Unit = runBlocking {
        logThreadInfo("Starting the demo scope")
        // goes top to bot scheduling, then runs and suspects 3rd -> 2nd-> 1st
        launch {

            delay(500.milliseconds)
            logThreadInfo("Ending the demo scope 1")
            println("Hello1")
        }
        launch {
            delay(100.milliseconds)
            logThreadInfo("Ending the demo scope 2")
            println("Hello2")
        }
        launch {
            println("Hello3")
            logThreadInfo("Ending the demo scope 3")
        }

    }

    @Test
    fun coscopes() {
        runBlocking {

            launch {
                delay(1.seconds)
                launch {
                    delay(250.milliseconds)
                    log("Grandchild done")
                }
                log("Child 1 done!")
            }

            launch {
                delay(500.milliseconds)
                log("Child 2 done!")
            }

            log("Parent done!")
        }
    }

    @Test
    fun AnothertestStructuredConcurrencySum() = runBlocking {
        // 1. Schedule Suspending 2 FIRST
        launch {
            logThreadInfo("Suspending 2: Hello this works")
        }
        launch {
            logThreadInfo("Suspending 3: Hello this works")

        }
        launch {
            logThreadInfo("Suspending 4: Hello this works")
        }

        // 2. Now start the scope
        val sum = coroutineScope {
            // When this scope suspends at a.await(),
            // the thread is now free to pick up "Suspending 2"
            logThreadInfo("Summing thread")
            val a = async { generateValue() }
            val b = async { generateValue() }

            a.await() + b.await()
        }
        println("Main")
        logThreadInfo("runBlocking")


        logThreadInfo("Sum is $sum")
    }



}
