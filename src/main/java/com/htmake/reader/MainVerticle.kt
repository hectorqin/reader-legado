package com.htmake.reader

import com.google.gson.GsonBuilder
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import java.io.IOException
import java.net.ServerSocket
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
val gson = GsonBuilder().disableHtmlEscaping().create()
val prettyGson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

data class BasicError(
    val error: String,
    val exception: String,
    val message: String,
    val path: String,
    val status: Int,
    val timestamp: Long
)

class MainVerticle : CoroutineVerticle() {

  override suspend fun start() {
    var router = Router.router(vertx)
    // CORS support
    router.route().handler {
      it.addHeadersEndHandler { _ ->
        val origin = it.request().getHeader("Origin")
        if (origin != null && origin.isNotEmpty()) {
          var res = it.response()
          res.putHeader("Access-Control-Allow-Origin", origin)
          res.putHeader("Access-Control-Allow-Credentials", "true")
          res.putHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, PUT, DELETE")
          res.putHeader(
              "Access-Control-Allow-Headers",
              "Authorization, Content-Type, If-Match, If-Modified-Since, If-None-Match, If-Unmodified-Since, X-Requested-With"
          )
        }
      }
      val origin = it.request().getHeader("Origin")
      if (origin != null && origin.isNotEmpty() && it.request().method() == HttpMethod.OPTIONS) {
        it.end("")
      } else {
        it.next()
      }
    }

    router.route().handler(BodyHandler.create())

    router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT))
    router.route("/reader3/*").handler {
      val rawMethod = it.request().method().toString()
      logger.info("{} {}", rawMethod, URLDecoder.decode(it.request().absoluteURI(), "UTF-8"))
      if (!rawMethod.equals("PUT") &&
              (it.fileUploads() == null || it.fileUploads().isEmpty()) &&
              it.bodyAsString != null &&
              it.bodyAsString.length > 0 &&
              it.bodyAsString.length < 1000
      ) {
        logger.info("Request body: {}", it.bodyAsString)
      }
      it.next()
    }

    router.get("/health").handler { it.success("ok!") }

    var port = getAvailableTcpPort()
    try {
      var serverPort = System.getProperty("SERVER_PORT")
      if (serverPort != null) {
        var _port = serverPort.toIntOrNull()
        if (_port != null && _port > 0) {
          port = _port
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    vertx
        .createHttpServer()
        .requestHandler(router)
        .exceptionHandler { error -> logger.error("vertx exception: {}", error) }
        .listen(port) { res ->
          if (res.succeeded()) {
            logger.info("Server running at: http://localhost:{}", port)
          } else {
            logger.error("Server start faild")
          }
        }
  }

  fun getAvailableTcpPort(): Int {
    try {
      val serverSocket = ServerSocket(0)
      val localPort = serverSocket.getLocalPort()
      serverSocket.close()
      return localPort
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return -1
  }

  fun onHandlerError(ctx: RoutingContext, error: Exception) {
    logger.error("Error: {}", error)
    ctx.error(error)
  }

  /** An extension method for simplifying coroutines usage with Vert.x Web routers */
  fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Any) {
    handler { ctx ->
      var job: Job? = null
      ctx.request().connection().closeHandler {
        logger.info("客户端已断开链接，终止运行")
        job?.cancel()
      }
      job =
          launch(Dispatchers.IO) {
            try {
              ctx.success(fn(ctx))
            } catch (e: Exception) {
              onHandlerError(ctx, e)
            }
          }
    }
  }

  fun Route.coroutineHandlerWithoutRes(fn: suspend (RoutingContext) -> Any) {
    handler { ctx ->
      var job: Job? = null
      ctx.request().connection().closeHandler {
        logger.info("客户端已断开链接，终止运行")
        job?.cancel()
      }
      job =
          launch(Dispatchers.IO) {
            try {
              fn(ctx)
            } catch (e: Exception) {
              onHandlerError(ctx, e)
            }
          }
    }
  }

  fun RoutingContext.success(any: Any?) {
    val toJson: String =
        if (any is JsonObject) {
          any.toString()
        } else {
          gson.toJson(any)
        }
    this.response().putHeader("content-type", "application/json; charset=utf-8").end(toJson)
  }

  fun RoutingContext.error(throwable: Throwable) {
    val path = URLDecoder.decode(this.request().absoluteURI(), "UTF-8")
    val basicError =
        BasicError(
            "Internal Server Error",
            throwable.toString(),
            throwable.message.toString(),
            path,
            500,
            System.currentTimeMillis()
        )

    val errorJson = gson.toJson(basicError)
    logger.error("Internal Server Error", throwable)
    logger.error { errorJson }

    this.response()
        .putHeader("content-type", "application/json; charset=utf-8")
        .setStatusCode(500)
        .end(errorJson)
  }
}
