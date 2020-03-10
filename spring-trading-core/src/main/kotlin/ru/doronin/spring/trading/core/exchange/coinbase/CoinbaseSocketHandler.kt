package ru.doronin.spring.trading.core.exchange.coinbase

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import engines.coinbase.CoinbaseProCredentials
import engines.coinbase.CoinbaseTick
import engines.coinbase.isCoinbaseTick
import messaging.SocketMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * Socket handler implementation for Coinbase exchange
 */
class CoinbaseSocketHandler(
    private val sink: FluxSink<SocketMessage<*>>,
    private val mapper: ObjectMapper,
    private val credentials: CoinbaseProCredentials
) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> =
        Flux.just(engines.coinbase.generateSubscriptionMessage(credentials))
            .map { messageObj -> session.textMessage(mapper.writeValueAsString(messageObj)) }
            .`as`(session::send)
            .thenMany(session.receive())
            .map { receivedFrame -> mapper.readTree(receivedFrame.payloadAsText) }
            .filter(isCoinbaseTick::test)
            .publishOn(Schedulers.boundedElastic())
            .map { node -> mapper.treeToValue<CoinbaseTick>(node)!!.toPriceMessage() }
            .log()
            .doOnNext { sink.next(it) }
            .then()
}