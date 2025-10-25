package com.fit4j.examples.coroutines

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TestApplication {
}

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args)
}