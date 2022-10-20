package io.legado.app.adapters

import io.legado.app.model.DebugLog
import io.legado.app.help.http.StrResponse

interface ReaderAdapterInterface {
    fun getWorkDir(subPath: String = ""): String

    fun getWorkDir(vararg subDirFiles: String): String

    fun getCacheDir(): String

    suspend fun getStrResponseByRemoteWebview(
        url: String? = null,
        html: String? = null,
        encode: String? = null,
        tag: String? = null,
        headerMap: Map<String, String>? = null,
        sourceRegex: String? = null,
        javaScript: String? = null,
        proxy: String? = null,
        post: Boolean = false,
        body: String? = null,
        userNameSpace: String = "",
        debugLog: DebugLog? = null
    ): StrResponse
}