package ru.doronin.spring.trading.core.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import ru.doronin.spring.trading.core.candles.CandleService
import java.time.Instant
import java.util.*


@Configuration
class RoutingConfiguration(
    private val candleService: CandleService
) {

    @Bean("routerFunction")
    fun routerFunction(): RouterFunction<ServerResponse> =
        RouterFunctions.route()
            .GET("/") { renderIndexPage() }
            .GET("/index") { renderIndexPage() }
            .GET("/login") { renderLoginPage() }
            .GET("/candles") { handleCandlesRequest() }
            .build()

    private fun renderIndexPage(): Mono<ServerResponse> =
        ServerResponse.ok().render(
            "index", Collections.singletonMap(
                "authenticated",
                ReactiveSecurityContextHolder.getContext()
                    .map { securityContext: SecurityContext ->
                        securityContext.authentication.isAuthenticated
                    })
        )

    private fun renderLoginPage(): Mono<ServerResponse> = ServerResponse.ok().render("login")

    private fun handleCandlesRequest(): Mono<ServerResponse> =
        ServerResponse.ok().body(
            candleService.loadAllForPeriod(
                Instant.now().minusSeconds(3600),
                Instant.now()
            )
        )
}
