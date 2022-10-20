package io.legado.app.data.entities

import com.script.SimpleBindings
import io.legado.app.utils.Base64
import io.legado.app.constant.AppConst
import io.legado.app.help.CacheManager
import io.legado.app.help.JsExtensions
import io.legado.app.help.http.CookieStore
import io.legado.app.utils.*

/**
 * 可在js里调用,source.xxx()
 */
@Suppress("unused")
interface BaseSource : JsExtensions {

    var concurrentRate: String? // 并发率
    var loginUrl: String?       // 登录地址
    var loginUi: String?   // 登录UI
    var header: String?         // 请求头
    var enabledCookieJar: Boolean?    //启用cookieJar

    fun getTag(): String

    fun getKey(): String

    override fun getSource(): BaseSource? {
        return this
    }

    fun getLoginJs(): String? {
        val loginJs = loginUrl
        return when {
            loginJs == null -> null
            loginJs.startsWith("@js:") -> loginJs.substring(4)
            loginJs.startsWith("<js>") ->
                loginJs.substring(4, loginJs.lastIndexOf("<"))
            else -> loginJs
        }
    }

    fun login() {
        getLoginJs()?.let {
            evalJS(it)
        }
    }

    /**
     * 解析header规则
     */
    fun getHeaderMap(hasLoginHeader: Boolean = false) = HashMap<String, String>().apply {
        this[AppConst.UA_NAME] = AppConst.userAgent
        header?.let {
            GSON.fromJsonObject<Map<String, String>>(
                when {
                    it.startsWith("@js:", true) ->
                        evalJS(it.substring(4)).toString()
                    it.startsWith("<js>", true) ->
                        evalJS(it.substring(4, it.lastIndexOf("<"))).toString()
                    else -> it
                }
            ).getOrNull()?.let { map ->
                putAll(map)
            }
        }
        if (hasLoginHeader) {
            getLoginHeaderMap()?.let {
                putAll(it)
            }
        }
    }

    /**
     * 获取用于登录的头部信息
     */
    fun getLoginHeader(): String? {
        var cacheInstance = CacheManager(getUserNameSpace())
        return cacheInstance.get("loginHeader_${getKey()}")
    }

    fun getLoginHeaderMap(): Map<String, String>? {
        val cache = getLoginHeader() ?: return null
        return GSON.fromJsonObject<Map<String, String>>(cache).getOrNull()
    }

    /**
     * 保存登录头部信息,map格式,访问时自动添加
     */
    fun putLoginHeader(header: String) {
        var cacheInstance = CacheManager(getUserNameSpace())
        cacheInstance.put("loginHeader_${getKey()}", header)
    }

    fun removeLoginHeader() {
        var cacheInstance = CacheManager(getUserNameSpace())
        cacheInstance.delete("loginHeader_${getKey()}")
    }

    /**
     * 获取用户信息,可以用来登录
     * 用户信息采用aes加密存储
     */
    fun getLoginInfo(): String? {
        try {
            val key = AppConst.userAgent.encodeToByteArray(0, 8)
            var cacheInstance = CacheManager(getUserNameSpace())
            val cache = cacheInstance.get("userInfo_${getKey()}") ?: return null
            val encodeBytes = EncoderUtils.base64Decode(cache, Base64.DEFAULT).toByteArray()
            val decodeBytes = EncoderUtils.decryptAES(encodeBytes, key)
                ?: return null
            return String(decodeBytes)
        } catch (e: Exception) {
            log("获取登陆信息出错 " + e.localizedMessage)
            return null
        }
    }

    fun getLoginInfoMap(): Map<String, String>? {
        return GSON.fromJsonObject<Map<String, String>>(getLoginInfo()).getOrNull()
    }

    /**
     * 保存用户信息,aes加密
     */
    fun putLoginInfo(info: String): Boolean {
        return try {
            val key = (AppConst.userAgent).encodeToByteArray(0, 8)
            val encodeBytes = EncoderUtils.encryptAES(info.toByteArray(), key)
            val encodeStr = Base64.encodeToString(encodeBytes, Base64.DEFAULT)
            var cacheInstance = CacheManager(getUserNameSpace())
            cacheInstance.put("userInfo_${getKey()}", encodeStr)
            true
        } catch (e: Exception) {
            log("保存登陆信息出错 " + e.localizedMessage)
            false
        }
    }

    fun removeLoginInfo() {
        var cacheInstance = CacheManager(getUserNameSpace())
        cacheInstance.delete("userInfo_${getKey()}")
    }

    fun setVariable(variable: String?) {
        var cacheInstance = CacheManager(getUserNameSpace())
        if (variable != null) {
            cacheInstance.put("sourceVariable_${getKey()}", variable)
        } else {
            cacheInstance.delete("sourceVariable_${getKey()}")
        }
    }

    fun getVariable(): String? {
        var cacheInstance = CacheManager(getUserNameSpace())
        return cacheInstance.get("sourceVariable_${getKey()}")
    }

    /**
     * 执行JS
     */
    @Throws(Exception::class)
    fun evalJS(jsStr: String, bindingsConfig: SimpleBindings.() -> Unit = {}): Any? {
        val bindings = SimpleBindings()
        bindings.apply(bindingsConfig)
        bindings["java"] = this
        bindings["source"] = this
        bindings["baseUrl"] = getKey()
        bindings["cookie"] = CookieStore(getUserNameSpace())
        bindings["cache"] = CacheManager(getUserNameSpace())
        return AppConst.SCRIPT_ENGINE.eval(jsStr, bindings)
    }
}