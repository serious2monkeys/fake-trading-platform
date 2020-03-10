package ru.doronin.spring.trading.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@SpringBootApplication
@EnableReactiveMongoRepositories
class SpringTradingCoreApplication

fun main(args: Array<String>) {
    runApplication<SpringTradingCoreApplication>(*args)
}
