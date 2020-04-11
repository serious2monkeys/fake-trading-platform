package ru.doronin.spring.trading.core.configuration.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import messaging.SocketMessage
import messaging.TradeMessage
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Component
import org.springframework.util.MimeType
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ru.doronin.spring.trading.core.exchange.ExchangeService

/**
 * First-level implementation of websocket messages handler
 */
@Component
class MainWebsocketHandler(
    private val exchangeServices: List<ExchangeService>,
    objectMapper: ObjectMapper
) : WebSocketHandler {

    private val encoder = Jackson2JsonEncoder(
        objectMapper,
        MimeType.valueOf(MediaType.APPLICATION_JSON_VALUE),
        MimeType.valueOf(MediaType.APPLICATION_STREAM_JSON_VALUE)
    )

    private val decoder = Jackson2JsonDecoder(
        objectMapper,
        MimeType.valueOf(MediaType.APPLICATION_JSON_VALUE),
        MimeType.valueOf(MediaType.APPLICATION_STREAM_JSON_VALUE)
    )

    override fun handle(session: WebSocketSession): Mono<Void> =
        session
            .receive()
            .map(WebSocketMessage::retain)
            .map(WebSocketMessage::getPayload)
            .publishOn(Schedulers.boundedElastic())
            .transform(this::decode)
            .transform(this::operate)
            .onBackpressureBuffer(100)
            .transform { message -> encode(message, session.bufferFactory()) }
            .map { buffer -> WebSocketMessage(WebSocketMessage.Type.TEXT, buffer) }
            .`as`(session::send)

    /**
     * Encoding contents of outbound messages to JSON
     */
    private fun encode(
        outbound: Flux<SocketMessage<*>>,
        dataBufferFactory: DataBufferFactory
    ): Flux<DataBuffer> = outbound
        .flatMap { element: SocketMessage<*> ->
            encoder.encode(
                Mono.just(element),
                dataBufferFactory,
                ResolvableType.forType(element.javaClass),
                MediaType.APPLICATION_JSON, emptyMap()
            )
        }

    /**
     * Decoding incoming messages as trade messages
     */
    fun decode(inbound: Flux<DataBuffer>): Flux<SocketMessage<*>> = inbound.flatMap { buffer: DataBuffer ->
        decoder.decode(
            Mono.just(buffer),
            ResolvableType.forType(object : ParameterizedTypeReference<TradeMessage>() {}),
            MediaType.APPLICATION_JSON, emptyMap<String, Any>()
        )
    }.map { it as SocketMessage<*> }


    /**
     * Actual processing of incoming messages
     */
    private fun operate(inboundMessages: Flux<SocketMessage<*>>): Flux<SocketMessage<*>> =
        Flux.merge(
            Flux.fromIterable(exchangeServices).flatMap { exchangeService ->
                exchangeService.operateControlMessages(inboundMessages).onErrorResume { Mono.empty() }
            },
            Flux.fromIterable(exchangeServices).flatMap(ExchangeService::messageStream)
        )
}