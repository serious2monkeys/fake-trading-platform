package ru.doronin.spring.trading.core.exchange.kraken

import com.fasterxml.jackson.databind.ObjectMapper
import messaging.SocketMessage
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.SignalType
import reactor.util.retry.Retry
import ru.doronin.spring.trading.core.candles.CandleService
import ru.doronin.spring.trading.core.exchange.ExchangeService
import java.net.URI
import java.time.Duration
import java.util.logging.Level

/**
 * Exchange service implementation to integrate with Kraken
 */
@Service
class KrakenExchangeService(
    private val mapper: ObjectMapper,
    private val candleService: CandleService
) : ExchangeService {
    private val stream: Flux<SocketMessage<*>> =
        Flux.create { sink: FluxSink<SocketMessage<*>> -> connectToKraken(sink) }.publish().autoConnect(0)

    override fun messageStream(): Flux<out SocketMessage<*>> = stream

    private fun connectToKraken(sink: FluxSink<SocketMessage<*>>) {
        ReactorNettyWebSocketClient()
            .execute(
                URI.create(engines.kraken.FEED_ADDRESS),
                KrakenSocketHandler(sink, mapper, candleService)
            )
            .log("Connected to Kraken", Level.INFO, SignalType.ON_SUBSCRIBE)
            .retryWhen(Retry.withThrowable { e: Flux<Throwable> ->
                e.zipWith(Flux.range(0, Int.MAX_VALUE))
                    .delayElements(Duration.ofMillis(2000))
            })
            .subscribe()
    }
}
