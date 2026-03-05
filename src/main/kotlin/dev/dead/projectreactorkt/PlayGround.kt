package dev.dead.projectreactorkt

import kotlinx.coroutines.*
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import kotlin.system.measureTimeMillis

@Component
class PlayGround(private val webClient: WebClient) : CommandLineRunner {
    override fun run(vararg args: String) {
        runBlocking (Dispatchers.IO) {
            println("--- Starting Parallel Requests ---")

            val time = measureTimeMillis {
                val results = fetchInParallel(20)
                results.forEachIndexed { index, content ->
                    // Printing just the first 20 chars to keep logs clean
                    println("Request ${index + 1} completed. Preview: ${content.take(20)}...")
                }
            }

            println("--- Total time: ${time}ms ---")
        }
    }

    private suspend fun fetchInParallel(count: Int): List<String> = coroutineScope {
        (1..count).map { id ->
            async {
                // Since WebClient is non-blocking, async without a specific Dispatcher
                // will inherit the context and still perform parallel network calls.
                webClient.get()
                    .uri("https://jsonplaceholder.typicode.com/posts/$id")
                    .retrieve()
                    .awaitBody<String>()
            }
        }.awaitAll() // Parallel execution starts above and suspends here until all finish
    }
}
