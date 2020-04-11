package ru.doronin.database

import ch.qos.logback.classic.LoggerContext
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import io.ktor.util.InternalAPI
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.LoggerFactory
import ru.doronin.domains.PlatformUser
import ru.doronin.utils.encodePassword

@InternalAPI
object MongoIntegrator {
    private var asyncDatabaseClient: CoroutineClient
    private var asyncDatabase: CoroutineDatabase
    private val logger = KotlinLogging.logger {}

    init {
        val mongoConfiguration = MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(Net(27017, Network.localhostIsIPv6()))
                .configServer(false)
                .build()
        MongodStarter.getDefaultInstance().prepare(mongoConfiguration).start()

        asyncDatabaseClient = KMongo.createClient().coroutine
        asyncDatabase = asyncDatabaseClient.getDatabase("test")
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val rootLogger = loggerContext.getLogger("org.mongodb.driver")
        rootLogger.level = ch.qos.logback.classic.Level.OFF
        createCollectionsIfRequired()
    }

    private fun createCollectionsIfRequired() {
        GlobalScope.launch(Dispatchers.IO) {
            if (!asyncDatabase.listCollectionNames().contains(USERS_COLLECTION)) {
                asyncDatabase.createCollection(USERS_COLLECTION)
                logger.info { "Collection $USERS_COLLECTION created" }
            }
        }
    }

    fun saveAsync(entity: Any) {
        GlobalScope.async(Dispatchers.IO) {
            when (entity) {
                is PlatformUser -> asyncDatabase.getCollection<PlatformUser>(USERS_COLLECTION)
                        .save(entity.copy(password = encodePassword(entity.password)))
                else -> throw IllegalArgumentException("Entities of ${entity.javaClass.name} are not supported")
            }
        }.invokeOnCompletion { cause ->
            if (cause == null) {
                logger.info { "Successfully saved" }
            } else {
                logger.error { "Failed to save: ${cause.message}" }
                cause.printStackTrace()
            }
        }
    }

    fun findUser(username: String): Deferred<PlatformUser?> =
        GlobalScope.async(Dispatchers.IO) {
            asyncDatabase.getCollection<PlatformUser>(USERS_COLLECTION).find(PlatformUser::login eq username).first()
        }

    private const val USERS_COLLECTION = "users_collection"
}