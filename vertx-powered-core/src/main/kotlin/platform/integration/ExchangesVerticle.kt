package platform.integration

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import platform.http.HttpServerVerticle
import platform.integration.coinbase.CoinbaseVerticle
import platform.integration.kraken.KrakenVerticle

/**
 * Unit for composition of cryptocurrency exchanges data
 */
@ExperimentalCoroutinesApi
class ExchangesVerticle : AbstractVerticle() {
  private val log = LoggerFactory.getLogger(javaClass)

  override fun start(startPromise: Promise<Void>) {
    vertx.deployVerticle(CoinbaseVerticle()) { coinbaseDeploy ->
      if (coinbaseDeploy.succeeded()) {
        vertx.deployVerticle(KrakenVerticle()) { krakenDeploy ->
          if (krakenDeploy.succeeded()) {
            configureBusConsumer().onComplete(startPromise)
          } else {
            startPromise.fail("Failed to deploy Kraken verticle: ${krakenDeploy.cause().message}")
          }
        }
      } else {
        startPromise.fail("Failed to deploy Coinbase verticle: ${coinbaseDeploy.cause().message}")
      }
    }
  }

  /**
   * Creation consumer for event bus messages
   */
  private fun configureBusConsumer(): Future<Void> {
    vertx.eventBus().consumer<JsonObject>(PRICE_MESSAGE_BUS) { priceMessage ->
      vertx.eventBus().send(HttpServerVerticle.PRICES_FEED_BUS, priceMessage.body())
    }
    return Future.succeededFuture()
  }

  companion object {
    const val PRICE_MESSAGE_BUS = "price_message_bus"
  }
}
