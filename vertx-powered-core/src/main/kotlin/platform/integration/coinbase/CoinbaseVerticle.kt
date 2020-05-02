package platform.integration.coinbase

import com.fasterxml.jackson.module.kotlin.treeToValue
import engines.coinbase.*
import io.vertx.config.ConfigRetriever
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.config.configRetrieverOptionsOf
import io.vertx.kotlin.config.configStoreOptionsOf
import kotlinx.coroutines.*
import platform.integration.ExchangesVerticle
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Coinbase integration unit
 */
class CoinbaseVerticle : AbstractVerticle() {
  private val log = LoggerFactory.getLogger(javaClass)

  override fun start(startPromise: Promise<Void>) {
    val configurationSteps = generateConfigRetriever()
      .compose(this::loadAccessCredentials)
      .compose(this::configureSocketClient)

    configurationSteps.onComplete(startPromise)
  }

  /**
   * Load access credentials from configuration file
   */
  private fun loadAccessCredentials(configRetriever: ConfigRetriever): Future<CoinbaseProCredentials> {
    val credentialsPromise = Promise.promise<CoinbaseProCredentials>()
    configRetriever.getConfig { configurations ->
      if (configurations.failed()) {
        credentialsPromise.fail(configurations.cause())
      } else {
        val actualConfigurations = configurations.result()
        if (actualConfigurations.containsKey("coinbase")) {
          val coinbaseConfiguration = actualConfigurations.getJsonObject("coinbase")
          if (coinbaseConfiguration.isEmpty) {
            credentialsPromise.fail("Empty access credentials")
          } else {
            if (coinbaseConfiguration.containsKey("key")
              and coinbaseConfiguration.containsKey("passphrase")
              and coinbaseConfiguration.containsKey("secret")) {
              credentialsPromise.complete(CoinbaseProCredentials(
                apiKey = coinbaseConfiguration.getString("key"),
                secret = coinbaseConfiguration.getString("secret"),
                passphrase = coinbaseConfiguration.getString("passphrase")
              ))
            } else {
              credentialsPromise.fail("Incomplete credentials")
            }
          }
        } else {
          credentialsPromise.fail("Coinbase Pro access credentials not found")
        }
      }
    }
    return credentialsPromise.future()
  }

  /**
   * WS client configuration
   */
  private fun configureSocketClient(credentials: CoinbaseProCredentials): Future<Void> {
    val configurationPromise = Promise.promise<Void>()
    val client = vertx.createHttpClient(
      HttpClientOptions()
        .setDefaultHost(URI(FEED_ADDRESS).host)
        .setKeepAlive(true)
        .setDefaultPort(443)
        .setSsl(true)
    )
    client.webSocket("/") { socketConnected ->
      if (socketConnected.failed()) {
        configurationPromise.fail(socketConnected.cause())
      } else {
        val websocket = socketConnected.result()
        websocket.textMessageHandler(messageOperator)
        websocket.closeHandler { restartWsClient(credentials, client, 10) }
        websocket.exceptionHandler { exceptionEvent ->
          log.error("Error occurred: ", exceptionEvent.cause)
          restartWsClient(credentials, client, 5)
        }

        val subscription = generateSubscriptionMessage(credentials)
        websocket.writeTextMessage(Json.encode(subscription))
        configurationPromise.complete()
      }
    }
    return configurationPromise.future()
  }

  /**
   * Function to operate incoming websocket messages
   */
  @ExperimentalCoroutinesApi
  private val messageOperator: (testMessage: String) -> Unit = { textMessage ->
    GlobalScope.launch(Dispatchers.IO) {
      val jsonNode = DatabindCodec.mapper().readTree(textMessage)
      if (isCoinbaseTick.test(jsonNode)) {
        DatabindCodec.mapper().treeToValue<CoinbaseTick>(jsonNode)?.let { tick: CoinbaseTick ->
          vertx.eventBus().send(ExchangesVerticle.PRICE_MESSAGE_BUS, JsonObject.mapFrom(tick.toPriceMessage()).encode())
        }
      }
    }
  }

  /**
   * Restarting websocket client
   */
  private fun restartWsClient(credentials: CoinbaseProCredentials,
                              client: HttpClient,
                              delay: Int) {
    try {
      client.close()
    } catch (ex: Throwable) {
      log.warn("Client already closed")
    }

    vertx.setTimer(TimeUnit.SECONDS.toMillis(delay.toLong())) {
      configureSocketClient(credentials).onComplete { socketConfigurationResult ->
        if (socketConfigurationResult.succeeded()) {
          log.info("Successfully restarted")
        } else {
          log.warn("Failed to restart. Try again in 10 seconds")
          restartWsClient(credentials, client, 10)
        }
      }
    }
  }

  /**
   * Creating loader for extra configurations
   */
  private fun generateConfigRetriever(): Future<ConfigRetriever> {
    val store = configStoreOptionsOf(
      config = JsonObject().put("path", "configurations.yml"),
      type = "file",
      format = "yaml"
    )

    return Future.succeededFuture(ConfigRetriever.create(
      vertx,
      configRetrieverOptionsOf(
        stores = listOf(store)
      )
    ))
  }
}
