package ru.doronin.spring.trading.core.exchange.coinbase

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import engines.coinbase.CoinbaseProCredentials
import engines.coinbase.CoinbaseTick
import engines.coinbase.isCoinbaseTick
import messaging.SocketMessage
import mu.KotlinLogging
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ru.doronin.spring.trading.core.candles.CandleService
import ru.doronin.spring.trading.core.exchange.toCandle

/**
 * Socket handler implementation for Coinbase exchange
 */
class CoinbaseSocketHandler(
    private val sink: FluxSink<SocketMessage<*>>,
    private val mapper: ObjectMapper,
    private val credentials: CoinbaseProCredentials,
    private val candleService: CandleService
) : WebSocketHandler {
    private val logger = KotlinLogging.logger {}

    override fun handle(session: WebSocketSession): Mono<Void> =
        Flux.just(engines.coinbase.generateSubscriptionMessage(credentials))
            .map { messageObj -> session.textMessage(mapper.writeValueAsString(messageObj)) }
            .`as`(session::send)
            .thenMany(session.receive())
            .map { receivedFrame -> mapper.readTree(receivedFrame.payloadAsText) }
            .filter(isCoinbaseTick::test)
            .publishOn(Schedulers.boundedElastic())
            .map { node -> mapper.treeToValue<CoinbaseTick>(node)!!.toPriceMessage() }
            .doOnNext { message ->
                candleService.save(message.toCandle()).subscribe { logger.debug { "$it saved" } }
                sink.next(message)
            }
            .then()
}
