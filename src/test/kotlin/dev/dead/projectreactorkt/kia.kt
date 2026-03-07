package dev.dead.projectreactorkt

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
            logThreadInfo("running in IO context ", "IO DIS")
        }
        logThreadInfo("The result (guaranteed 10000): $x")
    }
}
