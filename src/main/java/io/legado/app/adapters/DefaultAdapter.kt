package io.legado.app.adapters

import java.nio.file.Paths
import java.io.File
import io.legado.app.model.DebugLog
import io.legado.app.help.http.StrResponse

class DefaultAdpater: ReaderAdapterInterface {
    override fun getWorkDir(subPath: String): String {
        var workDirPath = ""
        var osName = System.getProperty("os.name")
        var currentDir = System.getProperty("user.dir")
        // MacOS 存放目录为用户目录
        if (osName.startsWith("Mac OS", true) && !currentDir.startsWith("/Users/")) {
            workDirPath = Paths.get(System.getProperty("user.home"), ".reader").toString()
        } else {
            workDirPath = currentDir
        }
        var path = Paths.get(workDirPath, subPath);

        return path.toString();
    }

    override fun getWorkDir(vararg subDirFiles: String): String {
        return getWorkDir(getRelativePath(*subDirFiles))
    }

    fun getRelativePath(vararg subDirFiles: String): String {
        val path = StringBuilder("")
        subDirFiles.forEach {
            if (it.isNotEmpty()) {
                path.append(File.separator).append(it)
            }
        }
        return path.toString().let{
            if (it.startsWith("/")) {
                it.substring(1)
            } else {
                it
            }
        }
    }

    override fun getCacheDir(): String {
        return getWorkDir("storage", "cache")
    }

    override suspend fun getStrResponseByRemoteWebview(
        url: String?,
        html: String?,
        encode: String?,
        tag: String?,
        headerMap: Map<String, String>?,
        sourceRegex: String?,
        javaScript: String?,
        proxy: String?,
        post: Boolean,
        body: String?,
        userNameSpace: String,
        debugLog: DebugLog?
    ): StrResponse {
        throw Exception("不支持webview");
    }
}