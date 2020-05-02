package platform.integration

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import platform.integration.coinbase.CoinbaseVerticle
import platform.integration.kraken.KrakenVerticle

/**
 * Unit for composition of cryptocurrency exchanges data
 */
class ExchangesVerticle : AbstractVerticle() {

  override fun start(startPromise: Promise<Void>) {
    vertx.deployVerticle(CoinbaseVerticle()) { coinbaseDeploy ->
      if (coinbaseDeploy.succeeded()) {
        vertx.deployVerticle(KrakenVerticle()) { krakenDeploy ->
          if (krakenDeploy.succeeded()) {
            startPromise.complete()
          } else {
            startPromise.fail("Failed to deploy Kraken verticle: ${krakenDeploy.cause().message}")
          }
        }
      } else {
        startPromise.fail("Failed to deploy Coinbase verticle: ${coinbaseDeploy.cause().message}")
      }
    }
  }

  companion object {
    const val PRICE_MESSAGE_BUS = "price_message_bus"
  }
}
