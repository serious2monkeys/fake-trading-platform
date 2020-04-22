package ru.doronin.spring.trading.core.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.util.*


@Configuration
class RoutingConfiguration {

    @Bean("routerFunction")
    fun routerFunction(): RouterFunction<ServerResponse> =
            RouterFunctions.route()
                    .GET("/") { renderIndexPage() }
                    .GET("/index") { renderIndexPage() }
                    .GET("/login") { renderLoginPage() }
                    .build()

    private fun renderIndexPage(): Mono<ServerResponse> =
            ServerResponse.ok().render("index", Collections.singletonMap(
                    "authenticated",
                    ReactiveSecurityContextHolder.getContext()
                            .map { securityContext: SecurityContext ->
                                securityContext.authentication.isAuthenticated
                            }))

    private fun renderLoginPage(): Mono<ServerResponse> = ServerResponse.ok().render("login")
}