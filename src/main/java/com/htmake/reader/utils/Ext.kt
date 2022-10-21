package com.htmake.reader.utils

import io.vertx.core.buffer.Buffer
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.io.OutputStream
import java.io.InputStream
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.google.common.base.Throwables
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.json.JsonArray
import mu.KotlinLogging
import java.nio.file.Paths
import com.google.gson.reflect.TypeToken
import kotlin.reflect.KProperty1
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import io.legado.app.data.entities.Book
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.FileUtils

val logger = KotlinLogging.logger {}

val gson = GsonBuilder().disableHtmlEscaping().create()
val prettyGson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

var storageFinalPath = ""
var workDirPath = ""
var workDirInit = false


fun String.url(): String {
    if (this.startsWith("//")) {
        return ("http:" + this).toHttpUrl().toString()
    } else if (this.startsWith("http")) {
        return this.toHttpUrl().toString()
    }
    return this
}

fun String.toDir(absolute: Boolean = false): String {
    var path = this
    if (path.endsWith("/")) {
        path = path.substring(0, path.length - 1)
    }
    if (absolute && !path.startsWith("/")) {
        path = "/" + path
    }
    return path
}

fun File.deleteRecursively() {
    if (this.exists()) {
        if (this.isFile() ) {
            this.delete();
        } else {
            this.listFiles().forEach{
                it.deleteRecursively()
            }
            this.delete()
        }
    }
}

fun File.listFilesRecursively(): List<File> {
    var list = arrayListOf<File>()
    if (this.exists()) {
        if (this.isFile() ) {
            list.add(this)
        } else {
            this.listFiles().forEach{
                list.add(it)
                if (it.isDirectory()) {
                    list.addAll(it.listFilesRecursively())
                }
            }
        }
    }
    return list;
}

fun File.unzip(descDir: String): Boolean {
    if (!this.exists()) {
        return false
    }
    val buffer = ByteArray(1024)
    var outputStream: OutputStream? = null
    var inputStream: InputStream? = null
    try {
        val zf = ZipFile(this.toString())
        val entries = zf.entries()
        while (entries.hasMoreElements()) {
            val zipEntry: ZipEntry = entries.nextElement() as ZipEntry
            val zipEntryName: String = zipEntry.name

            val descFilePath: String = descDir + File.separator + zipEntryName
            if (zipEntry.isDirectory) {
                createDir(descFilePath)
            } else {
                inputStream = zf.getInputStream(zipEntry)
                val descFile: File = createFile(descFilePath)
                outputStream = FileOutputStream(descFile)

                var len: Int
                while (inputStream.read(buffer).also { len = it } > 0) {
                    outputStream.write(buffer, 0, len)
                }
                inputStream.close()
                outputStream.close()
            }
        }
        return true
    } catch(e: Exception) {
        e.printStackTrace()
    } finally {
        inputStream?.close()
        outputStream?.close()
    }
    return false
}

fun File.zip(zipFilePath: String): Boolean {
    if (!this.exists()) {
        return false
    }
    if (this.isDirectory()) {
        val files = this.listFiles()
        val filesList: List<File> = files.toList()
        return zip(filesList, zipFilePath)
    } else {
        return zip(arrayListOf(this), zipFilePath)
    }
}

fun zip(files: List<File>, zipFilePath: String): Boolean {
    if (files.isEmpty()) {
        return false
    }

    val zipFile = createFile(zipFilePath)
    val buffer = ByteArray(1024)
    var zipOutputStream: ZipOutputStream? = null
    var inputStream: FileInputStream? = null
    try {
        zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))
        for (file in files) {
            if (!file.exists()) continue
            zipOutputStream.putNextEntry(ZipEntry(file.name))
            inputStream = FileInputStream(file)
            var len: Int
            while (inputStream.read(buffer).also { len = it } > 0) {
                zipOutputStream.write(buffer, 0, len)
            }
            zipOutputStream.closeEntry()
        }
        return true
    } catch(e: Exception) {
        e.printStackTrace()
    } finally {
        inputStream?.close()
        zipOutputStream?.close()
    }
    return false
}

fun createDir(filePath: String): File {
    logger.debug("createDir filePath {}", filePath)
    val file = File(filePath)
    if (!file.exists()) {
        file.mkdirs()
    }
    return file
}

fun createFile(filePath: String): File {
    logger.debug("createFile filePath {}", filePath)
    val file = File(filePath)
    val parentFile = file.parentFile!!
    if (!parentFile.exists()) {
        parentFile.mkdirs()
    }
    if (!file.exists()) {
        file.createNewFile()
    }
    return file
}

// fun getWorkDir(subPath: String = ""): String {
//     if (!workDirInit && workDirPath.isEmpty()) {
//         if (workDirPath.isEmpty()) {
//             var osName = System.getProperty("os.name")
//             var currentDir = System.getProperty("user.dir")
//             logger.info("osName: {} currentDir: {}", osName, currentDir)
//             workDirPath = currentDir
//         }
//         workDirPath = ""
//         logger.info("Using workdir: {}", workDirPath)
//         workDirInit = true
//     }
//     var path = Paths.get(workDirPath, subPath);

//     return path.toString();
// }

// fun getWorkDir(vararg subDirFiles: String): String {
//     return getWorkDir(getRelativePath(*subDirFiles))
// }

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

fun getStoragePath(): String {
    if (storageFinalPath.isNotEmpty()) {
        return storageFinalPath;
    }
    var storagePath = File("storage").path

    logger.info("Using storagePath: {}", storagePath)
    return storagePath;
}

fun saveStorage(vararg name: String, value: Any, pretty: Boolean = false) {
    val toJson: String = if (value is JsonObject || value is JsonArray) {
        value.toString()
    } else if (pretty) {
        prettyGson.toJson(value)
    } else {
        gson.toJson(value)
    }

    var storagePath = getStoragePath()
    var storageDir = File(storagePath)
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }

    val filename = name.last()
    val path = getRelativePath(*name.copyOfRange(0, name.size - 1), "${filename}.json")
    val file = File(storagePath + File.separator + path)
    logger.info("Save file to storage name: {} path: {}", name, file.absoluteFile)

    if (!file.parentFile.exists()) {
        file.parentFile.mkdirs()
    }

    if (!file.exists()) {
        file.createNewFile()
    }
    file.writeText(toJson)
}

fun getStorage(vararg name: String): String?  {
    var storagePath = getStoragePath()
    var storageDir = File(storagePath)
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }

    val filename = name.last()
    val path = getRelativePath(*name.copyOfRange(0, name.size - 1), "${filename}.json")
    val file = File(storagePath + File.separator + path)
    logger.info("Read file from storage name: {} path: {}", name, file.absoluteFile)
    if (!file.exists()) {
        return null
    }
    val content = file.readText()
    return content
}

fun asJsonArray(value: Any?): JsonArray? {
    if (value is JsonArray) {
        return value
    } else if (value is String) {
        try {
            return JsonArray(value)
        } catch(e: Exception) {
            logger.error("解析内容出错: {}  内容: \n{}", e, value)
            throw e
        }
    }
    return null
}

fun asJsonObject(value: Any?): JsonObject? {
    if (value is JsonObject) {
        return value
    } else if (value is String) {
        try {
            return JsonObject(value)
        } catch(e: Exception) {
            logger.error("解析内容出错: {}  内容: \n{}", e, value)
            throw e
        }
    }
    return null
}

//convert a data class to a map
fun <T> T.serializeToMap(): Map<String, Any> {
    return convert()
}

//convert string to a map
fun <T> T.toMap(): Map<String, Any> {
    return convert()
}

//convert a map to a data class
inline fun <reified T> Map<String, Any>.toDataClass(): T {
    return convert()
}

//convert an object of type I to type O
inline fun <I, reified O> I.convert(): O {
    val json = if (this is String) {
        this
    } else {
        gson.toJson(this)
    }
    return gson.fromJson(json, object : TypeToken<O>() {}.type)
}

@Suppress("UNCHECKED_CAST")
fun <R> readInstanceProperty(instance: Any, propertyName: String): R {
    val property = instance::class.memberProperties
                     // don't cast here to <Any, R>, it would succeed silently
                     .first { it.name == propertyName } as KProperty1<Any, *>
    // force a invalid cast exception if incorrect type here
    return property.get(instance) as R
}

@Suppress("UNCHECKED_CAST")
fun setInstanceProperty(instance: Any, propertyName: String, propertyValue: Any) {
    val property = instance::class.memberProperties
                     .first { it.name == propertyName }
    if(property is KMutableProperty<*>) {
        property.setter.call(instance, propertyValue)
    }
}

fun Book.fillData(newBook: Book, keys: List<String>): Book {
    keys.let {
        for (key in it) {
            var current = readInstanceProperty<String>(this, key)
            if (current.isNullOrEmpty()) {
                var cacheValue = readInstanceProperty<String>(newBook, key)
                if (!cacheValue.isNullOrEmpty()) {
                    setInstanceProperty(this, key, cacheValue)
                }
            }
        }
    }
    return this
}

fun getRandomString(length: Int) : String {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun genEncryptedPassword(password: String, salt: String): String {
    return MD5Utils.md5Encode(
        MD5Utils.md5Encode(password + salt).toString() + salt
    ).toString()
}

fun jsonEncode(value: Any, pretty: Boolean = false): String {
    if (pretty) {
        return prettyGson.toJson(value)
    }
    return gson.toJson(value)
}

/**
 * 列出指定目录下的所有文件
 */
fun File.deepListFiles(allowExtensions: Array<String>?): List<File> {
    val fileList = arrayListOf<File>()
    this.listFiles().forEach { it ->
        //返回当前目录所有以某些扩展名结尾的文件
        if (it.isDirectory()) {
            fileList.addAll(it.deepListFiles(allowExtensions))
        } else {
            val extension = FileUtils.getExtension(it.name)
            if(allowExtensions?.contentDeepToString()?.contains(extension) == true
                || allowExtensions == null) {
                fileList.add(it)
            }
        }
    }
    return fileList
}
