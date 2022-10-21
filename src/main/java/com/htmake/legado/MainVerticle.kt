package com.htmake.legado

import com.google.gson.GsonBuilder
import com.htmake.legado.utils.*
import io.legado.app.data.entities.BookSource
import io.legado.app.model.Debugger
import io.legado.app.model.webBook.WebBook
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.StaticHandler
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

class ReturnData {

    var isSuccess: Boolean = false
        private set

    var errorMsg: String = "未知错误,请联系开发者!"
        private set

    var data: Any? = null
        private set

    fun setErrorMsg(errorMsg: String): ReturnData {
        this.isSuccess = false
        this.errorMsg = errorMsg
        return this
    }

    fun setData(data: Any): ReturnData {
        this.isSuccess = true
        this.errorMsg = ""
        this.data = data
        return this
    }
}

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
            if (origin != null && origin.isNotEmpty() && it.request().method() == HttpMethod.OPTIONS
            ) {
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

        // web界面
        router.route("/*")
                .handler(StaticHandler.create("bookSourceDebug").setDefaultContentEncoding("UTF-8"))

        router.route("/getBookSources").coroutineHandler { getBookSources(it) }
        router.route("/saveBookSources").coroutineHandler { saveBookSources(it) }
        router.route("/saveBookSource").coroutineHandler { saveBookSource(it) }
        router.route("/deleteBookSources").coroutineHandler { deleteBookSources(it) }
        router.route("/bookSourceDebugSSE").coroutineHandlerWithoutRes { bookSourceDebugSSE(it) }

        var port = 9080
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
        if (!checkTcpPort(port)) {
            port = getAvailableTcpPort()
        }
        vertx.createHttpServer()
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

    fun checkTcpPort(port: Int): Boolean {
        try {
            ServerSocket(port).close()
            return true
        } catch (e: IOException) {}

        return false
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

    fun jsonEncode(value: Any, pretty: Boolean = false): String {
        if (pretty) {
            return prettyGson.toJson(value)
        }
        return gson.toJson(value)
    }

    fun getUserBookSourceJson(userNameSpace: String): JsonArray? {
        var bookSourceList: JsonArray? = asJsonArray(getUserStorage(userNameSpace, "bookSource"))
        if (bookSourceList == null && !userNameSpace.equals("default")) {
            // 用户书源文件不存在，拷贝系统书源
            var systemBookSourceList: JsonArray? =
                    asJsonArray(getUserStorage("default", "bookSource"))
            if (systemBookSourceList != null) {
                saveUserStorage(userNameSpace, "bookSource", systemBookSourceList.getList())
                bookSourceList = systemBookSourceList
            }
        }
        return bookSourceList
    }

    fun getUserNameSpace(context: RoutingContext): String {
        return "default"
    }

    fun getUserStorage(context: Any, vararg path: String): String? {
        var userNameSpace = ""
        when (context) {
            is RoutingContext -> userNameSpace = getUserNameSpace(context)
            is String -> userNameSpace = context
        }
        if (userNameSpace.isEmpty()) {
            return getStorage("data", *path)
        }
        return getStorage("data", userNameSpace, *path)
    }

    fun saveUserStorage(context: Any, path: String, value: Any) {
        var userNameSpace = ""
        when (context) {
            is RoutingContext -> userNameSpace = getUserNameSpace(context)
            is String -> userNameSpace = context
        }
        if (userNameSpace.isEmpty()) {
            return saveStorage("data", path, value = value)
        }
        return saveStorage("data", userNameSpace, path, value = value)
    }

    fun loadBookSourceStringList(userNameSpace: String, bookSourceGroup: String = ""): List<String> {
        var bookSourceList: JsonArray? = asJsonArray(getUserStorage(userNameSpace, "bookSource"))
        var userBookSourceList = arrayListOf<String>()
        if (bookSourceList != null) {
            for (i in 0 until bookSourceList.size()) {
                var isAdd = true
                if (!bookSourceGroup.isEmpty()) {
                    val bookSource = BookSource.fromJson(bookSourceList.getJsonObject(i).toString()).getOrNull()
                    if (bookSource == null || bookSource.bookSourceGroup.isNullOrEmpty() || (bookSource.bookSourceGroup + ",").indexOf(bookSourceGroup + ",") < 0) {
                        isAdd = false
                    }
                }
                if (isAdd) {
                    userBookSourceList.add(bookSourceList.getJsonObject(i).toString())
                }
            }
        }
        return userBookSourceList
    }

    fun getBookSourceBySourceURL(sourceUrl: String, userNameSpace: String, bookSourceList: List<String>? = null): Pair<String?, Int> {
        var bookSourceString: String? = null
        var index: Int = -1
        if (sourceUrl.isNullOrEmpty()) {
            return Pair(bookSourceString, index)
        }
        // 优先查找用户的书源
        var userBookSourceList = bookSourceList ?: loadBookSourceStringList(userNameSpace)
        for (i in 0 until userBookSourceList.size) {
            val sourceMap = userBookSourceList.get(i).toMap()
            if (sourceUrl.equals(sourceMap.get("bookSourceUrl") as String)) {
                bookSourceString = userBookSourceList.get(i)
                index = i
                break;
            }
        }
        return Pair(bookSourceString, index)
    }

    /** controller */
    suspend fun getBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        var simple: Int
        if (context.request().method() == HttpMethod.POST) {
            // post 请求
            simple = context.bodyAsJson.getInteger("simple", 0)
        } else {
            // get 请求
            simple = context.queryParam("simple").firstOrNull()?.toInt() ?: 0
        }
        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList != null) {
            if (simple > 0) {
                var list = arrayListOf<Map<String, Any?>>()
                for (i in 0 until bookSourceList.size()) {
                    val bookSource =
                            BookSource.fromJson(bookSourceList.getJsonObject(i).toString())
                                    .getOrNull()
                    if (bookSource != null) {
                        list.add(
                                mapOf<String, Any?>(
                                        "bookSourceGroup" to bookSource.bookSourceGroup,
                                        "bookSourceName" to bookSource.bookSourceName,
                                        "bookSourceUrl" to bookSource.bookSourceUrl,
                                        "exploreUrl" to bookSource.exploreUrl
                                )
                        )
                    }
                }
                return returnData.setData(list)
            }
            return returnData.setData(bookSourceList.getList())
        }
        return returnData.setData(arrayListOf<Int>())
    }

    suspend fun saveBookSource(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        val bookSource = BookSource.fromJson(context.bodyAsString).getOrNull()
        if (bookSource == null) {
            return returnData.setErrorMsg("参数错误")
        }

        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList == null) {
            bookSourceList = JsonArray()
        }
        // 遍历判断书本是否存在
        var existIndex: Int = -1
        for (i in 0 until bookSourceList.size()) {
            var _bookSource =
                    BookSource.fromJson(bookSourceList.getJsonObject(i).toString()).getOrNull()

            if (_bookSource != null && _bookSource.bookSourceUrl.equals(bookSource.bookSourceUrl)) {
                existIndex = i
                break
            }
        }
        if (existIndex >= 0) {
            var sourceList = bookSourceList.getList()
            sourceList.set(existIndex, JsonObject.mapFrom(bookSource))
            bookSourceList = JsonArray(sourceList)
        } else {
            bookSourceList.add(JsonObject.mapFrom(bookSource))
        }

        // logger.info("bookSourceList: {}", bookSourceList)
        saveUserStorage(userNameSpace, "bookSource", bookSourceList)
        return returnData.setData("")
    }

    suspend fun saveBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        val bookSourceJsonArray = context.bodyAsJsonArray
        if (bookSourceJsonArray == null) {
            return returnData.setErrorMsg("参数错误")
        }
        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList == null) {
            bookSourceList = JsonArray()
        }
        for (k in 0 until bookSourceJsonArray.size()) {
            val bookSource =
                    BookSource.fromJson(bookSourceJsonArray.getJsonObject(k).toString()).getOrNull()
            if (bookSource == null) {
                continue
            }
            // 遍历判断书本是否存在
            var existIndex: Int = -1
            for (i in 0 until bookSourceList!!.size()) {
                var _bookSource =
                        BookSource.fromJson(bookSourceList.getJsonObject(i).toString()).getOrNull()
                if (_bookSource != null &&
                                _bookSource.bookSourceUrl.equals(bookSource.bookSourceUrl)
                ) {
                    existIndex = i
                    break
                }
            }
            if (existIndex >= 0) {
                var sourceList = bookSourceList.getList()
                sourceList.set(existIndex, JsonObject.mapFrom(bookSource))
                bookSourceList = JsonArray(sourceList)
            } else {
                bookSourceList.add(JsonObject.mapFrom(bookSource))
            }
        }

        // logger.info("bookSourceList: {}", bookSourceList)
        saveUserStorage(userNameSpace, "bookSource", bookSourceList!!)
        return returnData.setData("")
    }

    suspend fun deleteBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        val bookSourceJsonArray = context.bodyAsJsonArray

        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList == null) {
            bookSourceList = JsonArray()
        }
        for (k in 0 until bookSourceJsonArray.size()) {
            var bookSource =
                    BookSource.fromJson(bookSourceJsonArray.getJsonObject(k).toString()).getOrNull()
            if (bookSource == null) {
                continue
            }
            // 遍历判断书本是否存在
            var existIndex: Int = -1
            for (i in 0 until bookSourceList.size()) {
                var _bookSource =
                        BookSource.fromJson(bookSourceList.getJsonObject(i).toString()).getOrNull()
                if (_bookSource != null &&
                                _bookSource.bookSourceUrl.equals(bookSource.bookSourceUrl)
                ) {
                    existIndex = i
                    break
                }
            }
            if (existIndex >= 0) {
                bookSourceList.remove(existIndex)
            }
        }

        // logger.info("bookSourceList: {}", bookSourceList)
        saveUserStorage(userNameSpace, "bookSource", bookSourceList)
        return returnData.setData("")
    }

    suspend fun bookSourceDebugSSE(context: RoutingContext) {
        val returnData = ReturnData()
        // 返回 event-stream
        val response =
                context.response()
                        .putHeader("Content-Type", "text/event-stream")
                        .putHeader("Cache-Control", "no-cache")
                        .setChunked(true)

        var bookSourceUrl = context.queryParam("bookSourceUrl").firstOrNull() ?: ""
        var keyword = context.queryParam("keyword").firstOrNull() ?: ""

        if (bookSourceUrl.isNullOrEmpty()) {
            response.write("event: error\n")
            response.end("data: " + jsonEncode(returnData.setErrorMsg("未配置书源"), false) + "\n\n")
            return
        }
        if (keyword.isNullOrEmpty()) {
            response.write("event: error\n")
            response.end("data: " + jsonEncode(returnData.setErrorMsg("请输入搜索关键词"), false) + "\n\n")
            return
        }

        var userNameSpace = getUserNameSpace(context)
        var bookSourceString = getBookSourceBySourceURL(bookSourceUrl, userNameSpace).first
        if (bookSourceString.isNullOrEmpty()) {
            response.write("event: error\n")
            response.end("data: " + jsonEncode(returnData.setErrorMsg("未配置书源"), false) + "\n\n")
            return
        }
        context.request().connection().closeHandler {
            logger.info("客户端已断开链接，停止 bookSourceDebugSSE")
            coroutineContext.cancel()
        }

        logger.info("bookSourceDebugSSE bookSource: {} keyword: {}", bookSourceString, keyword)

        val debugger = Debugger { msg ->
            response.write("data: " + jsonEncode(mapOf("msg" to msg), false) + "\n\n")
        }

        val webBook = WebBook(bookSourceString, userNameSpace = userNameSpace)

        debugger.startDebug(webBook, keyword)

        response.write("event: end\n")
        response.end("data: " + jsonEncode(mapOf("end" to true), false) + "\n\n")
    }
}
