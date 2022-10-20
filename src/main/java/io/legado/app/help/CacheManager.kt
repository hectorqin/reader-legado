package io.legado.app.help

import io.legado.app.data.entities.Cache
import io.legado.app.model.analyzeRule.QueryTTF
import io.legado.app.utils.ACache
import io.legado.app.adapters.ReaderAdapterHelper
import java.io.File

class CacheManager(val userNameSpace: String) {

    private val queryTTFMap = hashMapOf<String, Pair<Long, QueryTTF>>()
    val cacheInstance: ACache

    init {
        val cacheDir = File(ReaderAdapterHelper.getAdapter().getWorkDir("storage", "cache", "runtimeCache", userNameSpace))
        // 缓存 50M 的运行时缓存信息
        cacheInstance = ACache.get(cacheDir, 1000 * 1000 * 50L, 1000000)
    }

    /**
     * saveTime 单位为秒
     */
    @JvmOverloads
    fun put(key: String, value: Any, saveTime: Int = 0) {
        if (key.isEmpty()) {
            return
        }
        val deadline =
            if (saveTime == 0) 0 else System.currentTimeMillis() + saveTime * 1000
        when (value) {
            is QueryTTF -> queryTTFMap[key] = Pair(deadline, value)
            is ByteArray -> {
                cacheInstance.put(key, value, saveTime)
            }
            else -> {
                cacheInstance.put(key, value.toString(), saveTime)
            }
        }
    }

    fun get(key: String): String? {
        if (key.isEmpty()) {
            return null
        }
        return cacheInstance.getAsString(key)
    }

    fun getInt(key: String): Int? {
        return get(key)?.toIntOrNull()
    }

    fun getLong(key: String): Long? {
        return get(key)?.toLongOrNull()
    }

    fun getDouble(key: String): Double? {
        return get(key)?.toDoubleOrNull()
    }

    fun getFloat(key: String): Float? {
        return get(key)?.toFloatOrNull()
    }

    fun getByteArray(key: String): ByteArray? {
        if (key.isEmpty()) {
            return null
        }
        return cacheInstance.getAsBinary(key)
    }

    fun getQueryTTF(key: String): QueryTTF? {
        val cache = queryTTFMap[key] ?: return null
        if (cache.first == 0L || cache.first > System.currentTimeMillis()) {
            return cache.second
        }
        return null
    }

    fun putFile(key: String, value: String, saveTime: Int = 0) {
        if (key.isEmpty()) {
            return
        }
        cacheInstance.put(key, value, saveTime)
    }

    fun getFile(key: String): String? {
        if (key.isEmpty()) {
            return null
        }
        return cacheInstance.getAsString(key)
    }

    fun delete(key: String) {
        if (key.isEmpty()) {
            return
        }
        cacheInstance.remove(key)
    }
}