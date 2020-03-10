package ru.doronin.spring.trading.core.exchange

import messaging.SocketMessage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Abstraction of Crypto exchange integration service
 */
interface ExchangeService {
    /**
     * Stream of incoming messages received by actual service
     */
    fun messageStream(): Flux<out SocketMessage<*>>

    fun operateControlMessages(controlMessages: Flux<SocketMessage<*>>): Mono<out SocketMessage<*>> = Mono.empty()
}