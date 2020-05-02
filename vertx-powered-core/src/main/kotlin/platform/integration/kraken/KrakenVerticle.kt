package platform.integration.kraken

import basics.CurrencyPair
import com.fasterxml.jackson.databind.node.ArrayNode
import engines.kraken.FEED_ADDRESS
import engines.kraken.KrakenSocketEvent
import engines.kraken.convertToPriceMessages
import engines.kraken.isMessageValid
import enums.Currency
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocket
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.logging.LoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import platform.integration.ExchangesVerticle
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Kraken integration unit
 */
@ExperimentalCoroutinesApi
class KrakenVerticle : AbstractVerticle() {
  private val log = LoggerFactory.getLogger(javaClass)

  override fun start(startPromise: Promise<Void>) {
    configureSocketConnection().onComplete(startPromise)
  }

  /**
   * Configuration of websocket connection
   */
  private fun configureSocketConnection(): Future<Void> {
    val executionPromise = Promise.promise<Void>()
    val client = vertx.createHttpClient(
      HttpClientOptions()
        .setDefaultHost(URI(FEED_ADDRESS).host)
        .setKeepAlive(true)
        .setDefaultPort(443)
        .setSsl(true)
    )
    client.webSocket("/") { socketConnected ->
      if (socketConnected.failed()) {
        executionPromise.fail(socketConnected.cause())
      } else {
        val websocket = socketConnected.result()
        websocket.textMessageHandler(this::handleTextMessage)
        websocket.closeHandler { reconnectSocket(client, 5L) }
        websocket.exceptionHandler { exceptionEvent ->
          log.error("Error occurred: ", exceptionEvent.cause)
          reconnectSocket(client, 5)
        }
        sendSubscriptionMessage(websocket)
        executionPromise.complete()
      }
    }
    return executionPromise.future()
  }


  private fun sendSubscriptionMessage(websocket: WebSocket) {
    val subscriptionEvent = KrakenSocketEvent(
      pair = listOf(
        CurrencyPair.of(Currency.BTC, Currency.EUR),
        CurrencyPair.of(Currency.BTC, Currency.USD),
        CurrencyPair.of(Currency.ETH, Currency.EUR),
        CurrencyPair.of(Currency.ETH, Currency.USD)
      ).map(CurrencyPair::toString)
    )
    websocket.writeTextMessage(Json.encode(subscriptionEvent))
  }

  /**
   * Forced socket reconnection
   */
  private fun reconnectSocket(client: HttpClient, delay: Long) {
    try {
      client.close()
    } catch (ex: Throwable) {
      log.warn("Client already closed")
    }

    vertx.setTimer(TimeUnit.SECONDS.toMillis(delay)) {
      configureSocketConnection()
        .onComplete { restartResult ->
          if (restartResult.succeeded()) {
            log.info("Successfully restarted")
          } else {
            log.warn("Failed to restart. Try again in 10 seconds")
            reconnectSocket(client, 10)
          }
        }
    }
  }

  /**
   * Custom text message operation
   */
  private fun handleTextMessage(message: String) {
    GlobalScope.launch(Dispatchers.IO) {
      val jsonNode = DatabindCodec.mapper().readTree(message)
      if (isMessageValid.test(jsonNode)) {
        val array = jsonNode as ArrayNode
        convertToPriceMessages(array).forEach {priceMessage ->
          vertx.eventBus().send(ExchangesVerticle.PRICE_MESSAGE_BUS, JsonObject.mapFrom(priceMessage).encode())
        }
      }
    }
  }
}
