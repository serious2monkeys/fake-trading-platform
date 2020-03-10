package ru.doronin.spring.trading.core.configuration.security

import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.doronin.spring.trading.core.user.UserService

@Service
class CustomUserDetailsService(private val userService: UserService) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> =
        userService.findUserByUsername(login = username)
            .switchIfEmpty(Mono.error { UsernameNotFoundException("Unable to find this user") })
            .map { userData ->
                User.builder()
                    .username(userData.login)
                    .password(userData.password)
                    .roles(userData.role.name)
                    .accountExpired(false)
                    .accountLocked(false)
                    .disabled(false)
                    .credentialsExpired(false)
                    .build()
            }
}