package ru.doronin.spring.trading.core.user

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

/**
 * Implementation of some database operations
 */
@Repository
interface UserRepository: ReactiveMongoRepository<PlatformUser, String> {
    fun findFirstByLogin(login: String) : Mono<PlatformUser>
}