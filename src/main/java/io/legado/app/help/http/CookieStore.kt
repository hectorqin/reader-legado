@file:Suppress("unused")

package io.legado.app.help.http

import io.legado.app.utils.TextUtils
import io.legado.app.data.entities.Cookie
import io.legado.app.help.http.api.CookieManager
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.ACache
import java.io.File
import io.legado.app.adapters.ReaderAdapterHelper

// TODO 处理cookie
class CookieStore(val userNameSpace: String) : CookieManager {
    val cacheInstance: ACache

    init {
        val cacheDir = File(ReaderAdapterHelper.getAdapter().getWorkDir("storage", "cache", "cookie", userNameSpace))
        // 缓存 50M 的cookie信息
        cacheInstance = ACache.get(cacheDir, 1000 * 1000 * 50L, 1000000)
    }

    override fun setCookie(url: String, cookie: String?) {
        val domain = NetworkUtils.getSubDomain(url)
        if (domain.isNotEmpty()) {
            cacheInstance.put(domain, cookie ?: "")
        }
    }

    override fun replaceCookie(url: String, cookie: String) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(cookie)) {
            return
        }
        val oldCookie = getCookie(url)
        if (TextUtils.isEmpty(oldCookie)) {
            setCookie(url, cookie)
        } else {
            val cookieMap = cookieToMap(oldCookie)
            cookieMap.putAll(cookieToMap(cookie))
            val newCookie = mapToCookie(cookieMap)
            setCookie(url, newCookie)
        }
    }

    override fun getCookie(url: String): String {
        val domain = NetworkUtils.getSubDomain(url)
        if (domain.isEmpty()) {
            return ""
        }
        return cacheInstance.getAsString(domain) ?: ""
    }

    fun getKey(url: String, key: String): String {
        val cookie = getCookie(url)
        val cookieMap = cookieToMap(cookie)
        return cookieMap[key] ?: ""
    }

    override fun removeCookie(url: String) {
        val domain = NetworkUtils.getSubDomain(url)
        if (domain.isEmpty()) {
            return
        }
        cacheInstance.remove(domain)
    }

    override fun cookieToMap(cookie: String): MutableMap<String, String> {
        val cookieMap = mutableMapOf<String, String>()
        if (cookie.isBlank()) {
            return cookieMap
        }
        val pairArray = cookie.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (pair in pairArray) {
            val pairs = pair.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (pairs.size == 1) {
                continue
            }
            val key = pairs[0].trim { it <= ' ' }
            val value = pairs[1]
            if (value.isNotBlank() || value.trim { it <= ' ' } == "null") {
                cookieMap[key] = value.trim { it <= ' ' }
            }
        }
        return cookieMap
    }

    override fun mapToCookie(cookieMap: Map<String, String>?): String? {
        if (cookieMap == null || cookieMap.isEmpty()) {
            return null
        }
        val builder = StringBuilder()
        for (key in cookieMap.keys) {
            val value = cookieMap[key]
            if (value?.isNotBlank() == true) {
                builder.append(key)
                    .append("=")
                    .append(value)
                    .append(";")
            }
        }
        return builder.deleteCharAt(builder.lastIndexOf(";")).toString()
    }

    fun clear() {
        cacheInstance.clear()
    }

}