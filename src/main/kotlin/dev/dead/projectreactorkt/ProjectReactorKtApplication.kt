package dev.dead.projectreactorkt

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@SpringBootApplication
class ProjectReactorKtApplication
{
    @Bean
    fun webClient(): WebClient = WebClient.create("http://jsonplaceholder.typicode.com/")
}

fun main(args: Array<String>) {
    runApplication<ProjectReactorKtApplication>(*args)
}
