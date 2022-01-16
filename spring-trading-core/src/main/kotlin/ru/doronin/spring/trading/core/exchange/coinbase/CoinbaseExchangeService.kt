package ru.doronin.spring.trading.core.exchange.coinbase

import com.fasterxml.jackson.databind.ObjectMapper
import engines.coinbase.CoinbaseProCredentials
import messaging.SocketMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
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
 * Exchange service implementation to integrate with Coinbase
 */
@Service
class CoinbaseExchangeService(
    private val mapper: ObjectMapper,
    private val candleService: CandleService
) : ExchangeService, ApplicationListener<ContextRefreshedEvent> {
    @Value(value = "\${coinbase.credentials.key}")
    private lateinit var apiKey: String

    @Value(value = "\${coinbase.credentials.passphrase}")
    private lateinit var passphrase: String

    @Value(value = "\${coinbase.credentials.secret}")
    private lateinit var secret: String

    private lateinit var stream: Flux<SocketMessage<*>>

    /**
     * {@inheritDoc}
     */
    override fun messageStream(): Flux<out SocketMessage<*>> = stream

    /**
     * Connection to the crypto exchange on system startup
     */
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        stream = Flux.create { sink: FluxSink<SocketMessage<*>> -> connectToCoinbase(sink) }.publish().autoConnect(0)
    }

    private fun connectToCoinbase(sink: FluxSink<SocketMessage<*>>) {
        ReactorNettyWebSocketClient()
            .execute(
                URI.create(engines.coinbase.FEED_ADDRESS),
                CoinbaseSocketHandler(sink, mapper, CoinbaseProCredentials(apiKey, passphrase, secret), candleService)
            )
            .log("Connected to Coinbase", Level.INFO, SignalType.ON_SUBSCRIBE)
            .retryWhen(Retry.withThrowable { e: Flux<Throwable> ->
                e.zipWith(Flux.range(0, Int.MAX_VALUE))
                    .delayElements(Duration.ofMillis(2000))
            })
            .subscribe()
    }
}
