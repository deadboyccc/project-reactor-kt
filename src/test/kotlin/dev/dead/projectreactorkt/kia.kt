package dev.dead.projectreactorkt

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.random.nextInt
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
        delay(3.seconds)
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
    fun AnothertestStructuredConcurrencySum() = runBlocking(Dispatchers.Default + CoroutineName("test_coroutine")) {
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


    // flows
    @Test
    fun flows() = runBlocking(Dispatchers.Default + CoroutineName("test_coroutine")) {
        logThreadInfo("Starting the flows")
        val list = createListFlowy()
        list.forEach { logThreadInfo("${it}") }

    }

    suspend fun createListFlowy(): List<Int> {
        return buildList {
            add(1)
            delay(1.seconds)
            add(2)
            delay(1.seconds)
            add(3)
            delay(1.seconds)
            add(4)

        }
    }

    @Test
    fun testFlowOutput() = runBlocking {
        returnFlow().collect { value ->
            logThreadInfo("Received: $value")
        }
    }

    fun returnFlow(): Flow<Int> = flow {
        emit(1)
        delay(1.seconds)
        emit(2)
        delay(1.seconds)
        emit(3)
    }

    @Test
    fun testCoroutineScopeConstructor() {
        ComponentWithScope().apply {
            start()
            Thread.sleep(Duration.ofSeconds(3))
            stop()
        }
    }

    class ComponentWithScope(dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        companion object {
            val identifier = UUID.randomUUID().toString().slice(1..5)
        }

        private val scope = CoroutineScope(dispatcher + SupervisorJob())
        fun start() {
            log("Starting!")
            scope.launch {
                while (true) {
                    delay(500.milliseconds)
                    log("Component working!")
                }
            }
            scope.launch {
                log("Doing a one-off task...")
                delay(500.milliseconds)
                log("Task done!")
            }
        }

        fun stop() {
            log("Stopping!")
            scope.cancel()
        }

    }

    @Test
    fun cancelDemo() {
        runBlocking {
            val launchedJob = launch {
                log("I'm launched!")
                delay(1000.milliseconds)
                log("I'm done!")
            }
            val asyncDeferred = async {
                log("I'm async")
                delay(1000.milliseconds)
                log("I'm done!")
            }
            delay(200.milliseconds)
            launchedJob.cancel()
            asyncDeferred.cancel()
        }
    }

    @Test
    fun timeoutApi(): Unit = runBlocking {
        val num1 = withTimeoutOrNull(1.seconds) {
            doSlowCalculation()
        }
        println(num1)
        val num2 = withTimeoutOrNull(4.seconds) {
            doSlowCalculation()
        }
        println(num2)
    }

    suspend fun doSlowCalculation(): Int {
        delay(1.seconds)
        return Random.nextInt(10)
    }

    @Test
    fun cancelGrandChildren(): Unit = runBlocking(Dispatchers.Default) {
        val job = launch {
            logThreadInfo("Starting Grandfather")
            launch {
                delay(1.seconds)
                logThreadInfo("Starting Father")
                launch {
                    delay(1.seconds)
                    logThreadInfo("Starting Child")
                }
            }

        }
        delay(1.5.seconds)
        job.cancel()

    }

    @Test
    fun testCancelOnlyHappensAtSuspension(): Unit = runBlocking() {
        val job = launch {

            logThreadInfo("A")
            delay(1.seconds)
            logThreadInfo("B")
            logThreadInfo("C")
            //A or ABC never AB
        }
        delay(3.seconds)
        job.cancel()


    }

    suspend fun doFailingWorkThrowError(): Unit {
        logThreadInfo("Failing work started!")
        delay(1.seconds)
        throw UnsupportedOperationException("work failed")
    }

    @Test
    fun testSwallowingErrors(): Unit = runBlocking() {
        withTimeoutOrNull(2.seconds) {
            while (true) {
                try {
                    doFailingWorkThrowError()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logThreadInfo("Failing work exception caught! ${e.message}")
                }
            }
        }


    }

    suspend fun createInfiniteFlow(): Flow<Int> = flow {
        var x = 0
        while (true) {
            emit(++x)
            delay(1.seconds)
        }
    }

    @Test
    fun collectingInfiniteColdStream(): Unit {
        runBlocking(Dispatchers.Default) {
            createInfiniteFlow()
                .takeWhile { it < 11 }
                .collect { value ->
                    logThreadInfo("Collecting $value")
                }
        }
    }

    @Test
    fun `test if job cancelled`(): Unit = runBlocking(Dispatchers.Default) {
        val job = launch {
            repeat(5) {
                while (isActive) {
                    logThreadInfo(doSlowCalculation(), "Launch")
                }
            }


        }
        delay(3.seconds)
        job.cancel()
    }

    @Test
    fun `test no yield`(): Unit = runBlocking() {
        launch {
            repeat(5)
            {
                logThreadInfo(doCpuHeavyWork())
            }
        }
        launch {
            repeat(2) {
                logThreadInfo(doSlowCalculation())
            }
        }

    }

    suspend fun doCpuHeavyWork(): Int {
        var counter = 0
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() < startTime + 500) {
            counter++
            yield()
        }

        return counter

    }

    @Test
    fun `resource leak`(): Unit = runBlocking() {
        val job = launch {
            TestDemoClosable().use {
                it.println()
            }


        }
        delay(200.milliseconds)
        job.cancel()
    }


    class TestDemoClosable : AutoCloseable {
        override fun close() {
            println("Closed")
        }

        fun println() {
            println("in Demo of Closable")
        }
    }

    // -> channel flows
    suspend fun createChannelFlow() = channelFlow {
        repeat(5)
        {
            launch { send(generateValue()) }
        }
    }

    @Test
    fun testingChannelFlow(): Unit = runBlocking(Dispatchers.Default) {
        val time = measureTimeMillis {
            createChannelFlow().collect { value -> logThreadInfo("Collected $value") }
        }
        assertTrue(time < 4_000)
        logThreadInfo("Collected in $time ms")


    }

    @Test
    fun `sharedFlows testing`(): Unit = runBlocking(Dispatchers.Default) {
        // queue a task
        launch {
            cancel()
        }
        // use the same thread to broadcast, but it has delay so it should yield the thread
        val radioStation = RadioStation()
        radioStation.beginBroadcasting(this)

        // delay the thread
        delay(600.milliseconds)

        // collect the share flow with the thread( has no suspension unless explicit )
        radioStation.messageFlow.collect { value ->
            yield() // Manually yield the thread to the canceled task
            ensureActive()
            logThreadInfo("Collected $value")
        }


    }

    @Test
    fun `sharedFlows cancel testing`(): Unit = runBlocking(Dispatchers.Default) {
        // 1. Launch the cancellation task
        val cancelJob = launch {
            logThreadInfo("Cancellation task triggered")
            this@runBlocking.cancel()
        }

        val radioStation = RadioStation()
        radioStation.beginBroadcasting(this)


        try {
            radioStation.messageFlow.collect { value ->
                logThreadInfo("Collected $value")
                yield()
            }
        } catch (e: CancellationException) {
            logThreadInfo("Flow collection cancelled successfully")
        }

        // 2. FORCE the dispatcher to run the cancelJob before starting collection
        delay(3000.milliseconds)
        cancelJob.join()
    }

    @Test
    fun `review of scope functions`() {
        val user = User(null, null, null)
        user.email = "fa"
        user.email?.let {
            println(it)
        }
        user.apply {
            age = 12
        }
        // use with res
        println(File(".").absolutePath)
        File("./hello.txt").bufferedReader().use {
            it.forEachLine { println(it) }
        }
    }


}

class User(var name: String?, var age: Int?, var email: String?) {

}

class RadioStation {
    private val _messageFlow = MutableSharedFlow<Int>()
    val messageFlow = _messageFlow.asSharedFlow()

    fun beginBroadcasting(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                delay(500.milliseconds)
                val number = Random.nextInt(0..10)
                log("Emitting $number!")
                _messageFlow.emit(number)
                ensureActive()
                yield()
            }
        }

    }

    @Test
    fun testConvertingColdToshared() = runBlocking(Dispatchers.Default) {
        val sharedFlow = queryTemperature().shareIn(this, SharingStarted.Lazily)

        repeat(3) {
            launch {
                sharedFlow.collect {
                    logThreadInfo("Collected $it")
                }
            }
        }
    }


    fun returnRandomTemp(): Int = Random.nextInt(0..10)
    fun queryTemperature(): Flow<Int> = flow {
        repeat(5) {
            returnRandomTemp().also {
                log("Emitting $it")
                emit(it)
            }

        }
    }

    @Test
    fun `test transform on flows`(): Unit = runBlocking(Dispatchers.Default) {
        val a = flow<String> {
            emit("John")
            emit("Doe")
            emit("Jane")
            emit("Mary")
        }
        a.transform { value ->
            emit(value)
            emit(value.uppercase())
            emit(value.repeat(2))
        }.collect { value ->
            logThreadInfo("Collected $value")
        }
    }


}