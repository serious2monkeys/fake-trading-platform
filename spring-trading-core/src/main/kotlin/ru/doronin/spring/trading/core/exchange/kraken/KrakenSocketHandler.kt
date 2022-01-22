package ru.doronin.spring.trading.core.exchange.kraken

import basics.CurrencyPair
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import enums.Currency
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
 * Socket handler implementation for Kraken exchange
 */
class KrakenSocketHandler(
    private val sink: FluxSink<SocketMessage<*>>,
    private val mapper: ObjectMapper,
    private val candleService: CandleService
) : WebSocketHandler {
    private val logger = KotlinLogging.logger {}

    override fun handle(session: WebSocketSession): Mono<Void> =
        Flux.just(
            engines.kraken.KrakenSocketEvent(
                pair = listOf(
                    CurrencyPair.of(Currency.BTC, Currency.EUR),
                    CurrencyPair.of(Currency.BTC, Currency.USD),
                    CurrencyPair.of(Currency.ETH, Currency.EUR),
                    CurrencyPair.of(Currency.ETH, Currency.USD)
                ).map(CurrencyPair::toString)
            )
        ).map { messageObj -> session.textMessage(mapper.writeValueAsString(messageObj)) }
            .`as`(session::send)
            .thenMany(session.receive())
            .map { receivedFrame -> mapper.readTree(receivedFrame.payloadAsText) }
            .filter(engines.kraken.isMessageValid::test)
            .publishOn(Schedulers.boundedElastic())
            .flatMap { node -> Flux.fromIterable(engines.kraken.convertToPriceMessages(node as ArrayNode)) }
            .doOnNext { message ->
                candleService.save(message.toCandle()).subscribe { logger.debug { "$it saved" } }
                sink.next(message)
            }
            .then()
}
