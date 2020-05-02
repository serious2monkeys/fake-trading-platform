package platform.database

import ch.qos.logback.classic.LoggerContext
import com.github.javafaker.Faker
import de.flapdoodle.embed.mongo.MongodProcess
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import enums.UserRole
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.mongo.MongoClient
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.ext.mongo.saveAwait
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.apache.commons.lang3.RandomStringUtils
import platform.utils.encodePassword
import java.security.SecureRandom

/**
 * Verticle dedicated for interacting with MongoDb
 */
@ExperimentalCoroutinesApi
class MongoVerticle : AbstractVerticle() {
  private lateinit var mongoClient: MongoClient
  private lateinit var mongoProcess: MongodProcess
  private val log = LoggerFactory.getLogger(javaClass)

  override fun start(startPromise: Promise<Void>) {
    val configurationSteps = configureEmbeddedMongo()
      .compose { configureMongoClient() }
      .compose { initCollections() }
      .compose { createMockUsers() }
      .compose { configureBusConsumers() }
    configurationSteps.onComplete(startPromise)
  }

  /**
   * Starting embedded MongoDB
   */
  private fun configureEmbeddedMongo(): Future<Void> {
    val starterPromise = Promise.promise<Void>()
    try {
      val mongoConfiguration = MongodConfigBuilder()
        .version(Version.Main.PRODUCTION)
        .net(Net(27017, Network.localhostIsIPv6()))
        .configServer(false)
        .build()
      mongoProcess = MongodStarter.getDefaultInstance().prepare(mongoConfiguration).start()
      log.info("Embedded database started")
      disableVerboseMongoLogging()
      starterPromise.complete()
    } catch (ex: Throwable) {
      log.error("Failed to configure embedded databes", ex)
    }
    return starterPromise.future()
  }

  /**
   * Creating needed collections
   */
  private fun initCollections(): Future<Void> {
    val collectionsPromise = Promise.promise<Void>()
    mongoClient.getCollections { collectionsResult ->
      if (collectionsResult.failed()) {
        collectionsPromise.fail(collectionsResult.cause())
      } else {
        val collections = collectionsResult.result()
        if (collections.contains(USERS_COLLECTION)) {
          log.info("Database already have users collection")
          collectionsPromise.complete()
        } else {
          mongoClient.createCollection(USERS_COLLECTION) { collectionCreated ->
            if (collectionCreated.failed()) {
              collectionsPromise.fail(collectionCreated.cause())
            } else {
              log.info("Collection created")
              collectionsPromise.complete()
            }
          }
        }
      }
    }
    return collectionsPromise.future()
  }

  /**
   * MongoDB driver logging is enabled by default when using embedded DB.
   * Let's disable it
   */
  private fun disableVerboseMongoLogging() {
    val loggerContext = org.slf4j.LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = loggerContext.getLogger("org.mongodb.driver")
    rootLogger.level = ch.qos.logback.classic.Level.OFF
  }

  /**
   * Configuring Mongo Client for further requests
   */
  private fun configureMongoClient(): Future<Void> {
    return try {
      mongoClient = MongoClient.createShared(
        vertx,
        JsonObject()
          .put("connection_string", "mongodb://localhost:27017/rates_data")
          .put("db_name", "text")
      )
      Future.succeededFuture()
    } catch (ex: Throwable) {
      log.error("Failed to configure MongoDB client", ex)
      Future.failedFuture(ex)
    }
  }

  private fun createMockUsers(): Future<Void> {
    GlobalScope.async(vertx.dispatcher()) {
      val admin = PlatformUser(
        login = "admin",
        email = "admin@fake.platform",
        password = "password",
        role = UserRole.ADMIN
      )
      mongoClient.saveAwait(USERS_COLLECTION, JsonObject.mapFrom(admin.copy(password = encodePassword(admin.password))))
    }.invokeOnCompletion {
      log.info("Admin user saved")
    }
    var actions = Future.succeededFuture<Void>()
    repeat(10) {
      actions = actions.compose { saveMockUser() }
    }
    return actions
  }

  private fun saveMockUser(): Future<Void> {
    val savePromise = Promise.promise<Void>()
    val user = PlatformUser(
      login = FAKER_INSTANCE.dune().character().replace(Regex("\\s+"), "_"),
      email = FAKER_INSTANCE.internet().emailAddress(),
      password = RandomStringUtils.randomAlphanumeric(16)
    )
    log.info("Saving user with login ${user.login} and password ${user.password}")
    mongoClient.save(USERS_COLLECTION, JsonObject.mapFrom(user.copy(password = encodePassword(user.password)))) { savedEvent ->
      if (savedEvent.failed()) {
        log.warn("Failed to save user")
        savePromise.fail(savedEvent.cause())
      } else {
        log.info("Saved ${user.login}")
        savePromise.complete()
      }
    }
    return savePromise.future()
  }

  /**
   * Configuration of Event Bus messages consumers
   */
  private fun configureBusConsumers(): Future<Void> {
    vertx.eventBus().consumer<String>(SEARCH_USER_BUS) { incomingMessage ->
      mongoClient.findOne(USERS_COLLECTION, JsonObject().put("login", incomingMessage.body()), null) { searchResult ->
        if (searchResult.failed()) {
          incomingMessage.fail(-1, "User not found")
        } else {
          incomingMessage.reply(searchResult.result())
        }
      }
    }
    return Future.succeededFuture()
  }

  override fun stop(stopPromise: Promise<Void>) {
    mongoProcess.stop()
    super.stop(stopPromise)
  }

  companion object {
    private const val USERS_COLLECTION = "users"
    const val SEARCH_USER_BUS = "search_users_bus"
    private val SECURE_RANDOM = SecureRandom()
    private val FAKER_INSTANCE = Faker(SECURE_RANDOM)
  }
}
