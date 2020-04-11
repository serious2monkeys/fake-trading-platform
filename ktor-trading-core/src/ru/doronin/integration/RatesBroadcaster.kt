package ru.doronin.integration

import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import messaging.PriceMessage
import mu.KotlinLogging
import ru.doronin.integration.coinbase.CoinbaseListener
import ru.doronin.integration.kraken.KrakenListener
import ru.doronin.utils.JsonMapper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@KtorExperimentalAPI
@FlowPreview
@ExperimentalCoroutinesApi
object RatesBroadcaster {
    private val feedClients = ConcurrentHashMap<String, MutableList<WebSocketSession>>()
    private lateinit var previousMessage: PriceMessage
    private val logger = KotlinLogging.logger {}

    /**
     * Initial configuration
     */
    fun startup() {
        GlobalScope.async(Dispatchers.IO) {
            val channel = Channel<PriceMessage>(capacity = Channel.BUFFERED)
            CoinbaseListener.sendTo(channel)
            KrakenListener.sendTo(channel)
            for (message in channel) {
                previousMessage = message
                broadcast(message)
            }
        }.invokeOnCompletion { logger.info { "Startup configuration done" } }
    }

    /**
     * Send message to all websocket sessions
     */
    private suspend fun broadcast(priceMessage: PriceMessage) {
        withContext(Dispatchers.IO) {
            logger.info { "Received $priceMessage" }
            val textValue = JsonMapper.defaultMapper.writeValueAsString(priceMessage)
            if (feedClients.isNotEmpty()) {
                feedClients.forEach { (id, sessions) ->
                    logger.info { "Sending message to $id" }
                    for (session in sessions) {
                        session.send(Frame.Text(textValue))
                    }
                    logger.info { "Sent" }
                }
                logger.info { "Broadcasting done" }
            }
        }
    }

    /**
     * Connect client to feed
     */
    fun connectClient(id: String, session: WebSocketSession) {
        feedClients.computeIfAbsent(id) { CopyOnWriteArrayList() }.add(session)
        runBlocking(Dispatchers.IO) {
            session.send(Frame.Text(JsonMapper.defaultMapper.writeValueAsString(previousMessage)))
        }
    }

    /**
     * Disconnect client from feed
     */
    fun disconnect(id: String, session: WebSocketSession) {
        feedClients[id]?.remove(session)
        if (feedClients[id]?.isEmpty() == true) {
            feedClients.remove(id)
        }
    }
}