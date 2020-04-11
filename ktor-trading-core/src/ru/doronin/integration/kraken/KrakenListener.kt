package ru.doronin.integration.kraken

import basics.CurrencyPair
import com.fasterxml.jackson.databind.node.ArrayNode
import engines.kraken.FEED_ADDRESS
import engines.kraken.KrakenSocketEvent
import engines.kraken.convertToPriceMessages
import engines.kraken.isMessageValid
import enums.Currency
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.FrameType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import messaging.PriceMessage
import ru.doronin.utils.JsonMapper
import java.util.function.Predicate

/**
 * Component required to connect Kraken feed
 */
@ExperimentalCoroutinesApi
@FlowPreview
@KtorExperimentalAPI
object KrakenListener {

    /**
     * Send all incoming price messaged to passed channel
     */
    suspend fun sendTo(channel: SendChannel<PriceMessage>) {
        GlobalScope.launch(Dispatchers.IO) {
            val subscriptionEvent = KrakenSocketEvent(
                    pair = listOf(
                            CurrencyPair.of(Currency.BTC, Currency.EUR),
                            CurrencyPair.of(Currency.BTC, Currency.USD),
                            CurrencyPair.of(Currency.ETH, Currency.EUR),
                            CurrencyPair.of(Currency.ETH, Currency.USD)
                    ).map(CurrencyPair::toString)
            )
            val textSubscription = JsonMapper.defaultMapper.writeValueAsString(subscriptionEvent)
            val filterFunction = Predicate<Frame> { frame ->
                if (frame.frameType == FrameType.TEXT) {
                    isMessageValid.test(JsonMapper.defaultMapper.readTree(frame.data))
                } else {
                    false
                }
            }
            val mappingFunction: (Frame) -> Flow<PriceMessage> = { frame ->
                val array = JsonMapper.defaultMapper.readTree(frame.data) as ArrayNode
                convertToPriceMessages(array).asFlow()
            }

            val client = HttpClient(CIO).config { install(WebSockets) }
            client.wss(urlString = FEED_ADDRESS) {
                send(Frame.Text(textSubscription))
                incoming.receiveAsFlow().filter { filterFunction.test(it) }
                        .flatMapConcat { frame -> mappingFunction.invoke(frame) }
                        .flowOn(Dispatchers.Unconfined)
                        .collect {
                            channel.send(it)
                        }
            }
        }
    }
}