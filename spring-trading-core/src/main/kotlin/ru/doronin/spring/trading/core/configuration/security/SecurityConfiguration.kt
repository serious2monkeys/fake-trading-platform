package ru.doronin.spring.trading.core.configuration.security

import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

/**
 * Simple security configuration for reactive Spring Apps
 */
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(proxyTargetClass = true)
class SecurityConfiguration {

    @Bean
    fun encoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
            http
                    .authorizeExchange()
                    .pathMatchers("/login/**").permitAll()
                    .anyExchange().authenticated()
                    .and()
                    .formLogin().loginPage("/login")
                    .and()
                    .logout()
                    .logoutUrl("/logout")
                    .requiresLogout(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/logout"))
                    .logoutHandler(SecurityContextServerLogoutHandler())
                    .and()
                    .csrf().disable().build()
}