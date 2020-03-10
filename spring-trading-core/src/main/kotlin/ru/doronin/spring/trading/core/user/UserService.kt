package ru.doronin.spring.trading.core.user

import mu.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Service for operations with user data
 */
@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun findUserByUsername(login: String): Mono<PlatformUser> {
        return userRepository.findFirstByLogin(login)
    }

    fun save(user: PlatformUser): Mono<PlatformUser> {
        val moment = Instant.now()
        user.modified = moment
        user.password = passwordEncoder.encode(user.password)
        return userRepository.save(user)
    }
}