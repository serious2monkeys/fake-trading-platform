package ru.doronin

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.JacksonConverter
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.sessions.*
import io.ktor.thymeleaf.Thymeleaf
import io.ktor.thymeleaf.respondTemplate
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.generateNonce
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import ru.doronin.bootstrap.runInitialBootstrapping
import ru.doronin.database.MongoIntegrator
import ru.doronin.integration.RatesBroadcaster
import ru.doronin.utils.JsonMapper
import ru.doronin.utils.verifyPassword
import java.time.Duration

private const val SOCKET_SESSION_NAME = "KTOR_SOCKET_SESSION"
private const val FORM_SESSION_NAME = "KTOR_FORM_SESSION"

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@FlowPreview
@KtorExperimentalAPI
@ExperimentalCoroutinesApi
@InternalAPI
@Suppress("unused")
fun Application.module() {
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }

    install(Compression) {
        gzip {
            priority = 1.0
        }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header("KtorTrading")
        allowCredentials = true
        allowSameOrigin = true
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(5)
        timeout = Duration.ofSeconds(5)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(Sessions) {
        cookie<SimpleSession>(name = SOCKET_SESSION_NAME, storage = SessionStorageMemory())
        cookie<UserIdPrincipal>(name = FORM_SESSION_NAME, storage = SessionStorageMemory())
    }

    /**
     * Common function for authentication
     */
    val authenticationValidationFunction: suspend ApplicationCall.(UserPasswordCredential) -> Principal? = {
        val savedUser = MongoIntegrator.findUser(it.name).await()
        if (savedUser != null && verifyPassword(it.password, savedUser.password)) {
            UserIdPrincipal(it.name)
        } else {
            null
        }
    }

    install(Authentication) {
        form("ktorFormAuth") {
            userParamName = "username"
            passwordParamName = "password"
            challenge("/login")
            validate(authenticationValidationFunction)
        }

        session<UserIdPrincipal>("ktorSessionAuth") {
            challenge("/login")
            validate { userIdPrincipal: UserIdPrincipal -> userIdPrincipal }
        }
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(JsonMapper.defaultMapper))
    }

    routing {
        static("/static") {
            resources("static")
        }

        install(StatusPages) {
            exception<AuthenticationException> {
                call.respond(HttpStatusCode.Unauthorized)
            }
            exception<AuthorizationException> {
                call.respond(HttpStatusCode.Forbidden)
            }

            exception<RuntimeException> { cause ->
                call.respond(HttpStatusCode.InternalServerError, cause)
            }
        }

        get("/login") {
            call.respondTemplate(template = "login")
        }

        authenticate("ktorFormAuth", "ktorSessionAuth") {
            intercept(ApplicationCallPipeline.Features) {
                if (call.sessions.get<SimpleSession>() == null) {
                    call.sessions.set(SimpleSession(generateNonce()))
                }
            }

            post("/login") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.sessions.set(principal)
                call.respondRedirect("/index")
            }

            get("/") {
                call.respondRedirect("/index")
            }

            get("/index") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondTemplate(
                        template = "index",
                        model = mapOf("user" to ThymeleafUser(1, principal.name))
                )
            }

            get("/logout") {
                call.sessions.clear<UserIdPrincipal>()
                call.sessions.clear<SimpleSession>()
                call.respondRedirect("/login")
            }

            webSocket("/streaming") {
                val session = call.sessions.get<SimpleSession>()
                if (session == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                    return@webSocket
                } else {
                    RatesBroadcaster.connectClient(session.identifier, this)
                }

                try {
                    incoming.consumeEach { frame ->
                        log.info(if (frame.frameType == FrameType.TEXT)
                            String(frame.data)
                        else "Unrecognized message with type ${frame.frameType}")
                    }
                } finally {
                    RatesBroadcaster.disconnect(session.identifier, this)
                }
            }
        }
    }
    runInitialBootstrapping()
    launch {
        RatesBroadcaster.startup()
    }
}

data class ThymeleafUser(val id: Int, val name: String)

data class SimpleSession(val identifier: String)

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

