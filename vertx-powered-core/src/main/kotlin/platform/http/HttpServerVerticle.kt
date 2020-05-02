package platform.http

import enums.UserRole
import io.vertx.core.*
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.auth.AbstractUser
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import platform.database.MongoVerticle
import platform.database.PlatformUser
import platform.utils.verifyPassword

/**
 * Http request processing verticle
 */
@ExperimentalCoroutinesApi
class HttpServerVerticle : AbstractVerticle() {
  private lateinit var templateEngine: ThymeleafTemplateEngine
  private lateinit var customAuthProvider: AuthProvider

  override fun start(startPromise: Promise<Void>) {
    configureTemplateEngine()
      .compose { configureAuthProvider() }
      .compose { configureWebServer() }
      .onComplete(startPromise)
  }

  private fun configureTemplateEngine(): Future<Void> {
    templateEngine = ThymeleafTemplateEngine.create(vertx)
    val templateResolver = ClassLoaderTemplateResolver().apply {
      prefix = "templates/"
      suffix = ".html"
      isCacheable = true
      cacheTTLMs = 60000L
    }
    templateEngine.thymeleafTemplateEngine.setTemplateResolver(templateResolver)
    return Future.succeededFuture()
  }

  /**
   * Dedicated configuration of Http-server
   */
  private fun configureWebServer(): Future<Void> {
    val router = Router.router(vertx)
    router.route()
      .handler(SessionHandler.create(LocalSessionStore.create(vertx)))
      .handler(UserSessionHandler.create(customAuthProvider))
      .handler(BodyHandler.create())

    router.route("/")
      .handler(RedirectAuthHandler.create(customAuthProvider, "/login"))

    router.route("/").handler { routingContext ->
      templateEngine.render(JsonObject(), "index") { renderResult ->
        if (renderResult.succeeded()) {
          routingContext.response().end(renderResult.result())
        } else {
          routingContext.fail(500, renderResult.cause())
        }
      }
    }

    router.route(HttpMethod.GET, "/login").handler { routingContext ->
      templateEngine.render(JsonObject(), "login") { renderResult ->
        if (renderResult.succeeded()) {
          routingContext.response().end(renderResult.result())
        } else {
          routingContext.fail(500, renderResult.cause())
        }
      }
    }

    router.route(HttpMethod.POST, "/login")
      .handler(FormLoginHandler.create(customAuthProvider)
        .setReturnURLParam("/login")
        .setDirectLoggedInOKURL("/"))

    router.route("/logout").handler { context ->
      context.clearUser()
      context.response().putHeader("location", "/login").setStatusCode(302).end()
    }

    vertx.createHttpServer().requestHandler(router).listen(8080)
    return Future.succeededFuture()
  }

  /**
   * Configure authentication provider
   */
  private fun configureAuthProvider(): Future<Void> {
    customAuthProvider = AuthProvider { authInfo, resultHandler ->
      if (authInfo.containsKey("username") && authInfo.containsKey("password")) {
        val username = authInfo.getString("username")
        val password = authInfo.getString("password")
        vertx.eventBus().request<JsonObject>(MongoVerticle.SEARCH_USER_BUS, username) { userMessage ->
          if (userMessage.failed()) {
            resultHandler.handle(Future.failedFuture("Not found"))
          } else {
            val user = DatabindCodec.mapper().readValue(userMessage.result().body().encode(), PlatformUser::class.java)
            if (verifyPassword(password, user.password)) {
              resultHandler.handle(Future.succeededFuture(object : AbstractUser() {
                private var provider: AuthProvider = customAuthProvider

                override fun doIsPermitted(permission: String, resultHandler: Handler<AsyncResult<Boolean>>) {
                  if (user.role == UserRole.ADMIN) {
                    resultHandler.handle(Future.succeededFuture(true))
                  } else {
                    val result = when (permission) {
                      "ticker", "order" -> true
                      else -> false
                    }
                    resultHandler.handle(Future.succeededFuture(result))
                  }
                }

                override fun setAuthProvider(authProvider: AuthProvider) {
                  provider = authProvider
                }

                override fun principal(): JsonObject {
                  return JsonObject.mapFrom(user)
                }
              }))
            }
          }
        }
      }
    }
    return Future.succeededFuture()
  }
}
