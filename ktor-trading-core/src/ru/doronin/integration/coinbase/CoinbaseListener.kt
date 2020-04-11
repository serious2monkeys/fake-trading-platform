package ru.doronin.integration.coinbase

import com.typesafe.config.ConfigFactory
import engines.coinbase.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.config.HoconApplicationConfig
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
 * Component required to connect Coinbase Pro feed
 */
@ExperimentalCoroutinesApi
@KtorExperimentalAPI
object CoinbaseListener {

    /**
     * Send all received price messages to passed channel
     */
    suspend fun sendTo(channel: SendChannel<PriceMessage>) {
        GlobalScope.launch(Dispatchers.IO) {
            val credentialsConfig = HoconApplicationConfig(ConfigFactory.load()).config("coinbase")
            val credentials = CoinbaseProCredentials(
                    apiKey = credentialsConfig.property("key").getString(),
                    passphrase = credentialsConfig.property("passphrase").getString(),
                    secret = credentialsConfig.property("secret").getString()
            )
            val client = HttpClient(CIO).config { install(WebSockets) }
            val textCredentials = JsonMapper.defaultMapper.writeValueAsString(generateSubscriptionMessage(credentials))
            val filterFunction = Predicate<Frame> { frame: Frame ->
                if (frame.frameType == FrameType.TEXT) {
                    isCoinbaseTick.test(JsonMapper.defaultMapper.readTree(frame.data))
                } else {
                    false
                }
            }
            val mappingFunction: (Frame) -> PriceMessage = { frame ->
                JsonMapper.defaultMapper.readValue(frame.data, CoinbaseTick::class.java).toPriceMessage()
            }

            client.wss(urlString = FEED_ADDRESS) {
                send(Frame.Text(textCredentials))
                incoming.receiveAsFlow().filter { filterFunction.test(it) }.map { mappingFunction.invoke(it) }
                        .flowOn(Dispatchers.Unconfined)
                        .collect {
                            channel.send(it)
                        }
            }
        }
    }
}