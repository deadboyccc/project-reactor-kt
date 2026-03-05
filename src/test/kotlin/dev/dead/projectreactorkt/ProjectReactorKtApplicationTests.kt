package dev.dead.projectreactorkt

import kotlinx.coroutines.reactive.asFlow
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Flux

//@SpringBootTest
class ProjectReactorKtApplicationTests {

    @Test
    fun contextLoads() {
    }
    @Test
    suspend fun flowsFromReactiveTypes(){
        // convert flux to flow
        val asFlow = Flux.just(1, 2, 3,4,5,6,7,8,9,10)
            .asFlow();
        asFlow.collect { println(it) }
    }

}
