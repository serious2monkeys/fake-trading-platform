package platform

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.Json
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.logging.LoggerFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import platform.database.MongoVerticle
import platform.http.HttpServerVerticle
import platform.integration.ExchangesVerticle

@ExperimentalCoroutinesApi
class MainVerticle : AbstractVerticle() {
  private val log = LoggerFactory.getLogger(javaClass)

  override fun start(startPromise: Promise<Void>) {
    configureMapper()
    vertx.deployVerticle(MongoVerticle()) { mongoDeployed ->
      if (mongoDeployed.failed()) {
        startPromise.fail(mongoDeployed.cause())
      } else {
        log.info("Mongo verticle deployed")
        vertx.deployVerticle(ExchangesVerticle()) { exchangesConfigured ->
          if (exchangesConfigured.failed()) {
            startPromise.fail(exchangesConfigured.cause())
          } else {
            log.info("Exchanges verticle deployed")
            vertx.deployVerticle(HttpServerVerticle()) { httpServerDeployed ->
              if (httpServerDeployed.failed()) {
                startPromise.fail(httpServerDeployed.cause())
              } else {
                log.info("Http verticle deployed")
                startPromise.complete()
                log.info("Deploy done")
              }
            }
          }
        }
      }
    }
  }

  /**
   * Настройка Json-преобразователя
   */
  private fun configureMapper() {
    DatabindCodec.mapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    DatabindCodec.mapper().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
    DatabindCodec.mapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
    DatabindCodec.mapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    DatabindCodec.mapper().configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
    DatabindCodec.mapper().registerModule(JavaTimeModule())
    DatabindCodec.mapper().registerModule(KotlinModule())
    DatabindCodec.mapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    log.info("Mapper configured")
  }
}
