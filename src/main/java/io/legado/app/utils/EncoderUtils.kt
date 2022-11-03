package io.legado.app.utils

import io.legado.app.utils.Base64
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

@Suppress("unused")
object EncoderUtils {

    fun escape(src: String): String {
        val tmp = StringBuilder()
        for (char in src) {
            val charCode = char.code
            if (charCode in 48..57 || charCode in 65..90 || charCode in 97..122) {
                tmp.append(char)
                continue
            }

            val prefix = when {
                charCode < 16 -> "%0"
                charCode < 256 -> "%"
                else -> "%u"
            }
            tmp.append(prefix).append(charCode.toString(16))
        }
        return tmp.toString()
    }

    @JvmOverloads
    fun base64Decode(str: String, flags: Int = Base64.DEFAULT): String {
        val bytes = Base64.decode(str, flags)
        return String(bytes)
    }

    @JvmOverloads
    fun base64Encode(str: String, flags: Int = Base64.NO_WRAP): String? {
        return Base64.encodeToString(str.toByteArray(), flags)
    }

    //////////AES Start

    /**
     * Return the Base64-encode bytes of AES encryption.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DES/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the Base64-encode bytes of AES encryption
     */
    @Throws(Exception::class)
    fun encryptAES2Base64(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String? = "DES/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return Base64.encode(encryptAES(data, key, transformation, iv), Base64.NO_WRAP)
    }

    /**
     * Return the bytes of AES encryption.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DES/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the bytes of AES encryption
     */
    @Throws(Exception::class)
    fun encryptAES(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String? = "DES/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return symmetricTemplate(data, key, "AES", transformation!!, iv, true)
    }


    /**
     * Return the bytes of AES decryption for Base64-encode bytes.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DES/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the bytes of AES decryption for Base64-encode bytes
     */
    @Throws(Exception::class)
    fun decryptBase64AES(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String = "DES/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return decryptAES(Base64.decode(data, Base64.NO_WRAP), key, transformation, iv)
    }

    /**
     * Return the bytes of AES decryption.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DES/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the bytes of AES decryption
     */
    @Throws(Exception::class)
    fun decryptAES(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String = "DES/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return symmetricTemplate(data, key, "AES", transformation, iv, false)
    }


    /**
     * Return the bytes of symmetric encryption or decryption.
     *
     * @param data           The data.
     * @param key            The key.
     * @param algorithm      The name of algorithm.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, <i>DES/CBC/PKCS5Padding</i>.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @param isEncrypt      True to encrypt, false otherwise.
     * @return the bytes of symmetric encryption or decryption
     */
    @Suppress("SameParameterValue")
    @Throws(Exception::class)
    private fun symmetricTemplate(
        data: ByteArray?,
        key: ByteArray?,
        algorithm: String,
        transformation: String,
        iv: ByteArray?,
        isEncrypt: Boolean
    ): ByteArray? {
        return if (data == null || data.isEmpty() || key == null || key.isEmpty()) null
        else {
            val keySpec = SecretKeySpec(key, algorithm)
            val cipher = Cipher.getInstance(transformation)
            val mode = if (isEncrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE
            if (iv == null || iv.isEmpty()) {
                cipher.init(mode, keySpec)
            } else {
                val params: AlgorithmParameterSpec = IvParameterSpec(iv)
                cipher.init(mode, keySpec, params)
            }
            cipher.doFinal(data)
        }
    }
    //////////DES Start

    /**
     * Return the Base64-encode bytes of DES encryption.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DES/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the Base64-encode bytes of AES encryption
     */
    @Throws(Exception::class)
    fun encryptDES2Base64(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String? = "DES/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return Base64.encode(encryptDES(data, key, transformation, iv), Base64.NO_WRAP)
    }

    /**
     * Return the bytes of DES encryption.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DES/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the bytes of AES encryption
     */
    @Throws(Exception::class)
    fun encryptDES(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String? = "DES/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return symmetricTemplate(data, key, "DES", transformation!!, iv, true)
    }


    /**
     * Return the bytes of DES decryption for Base64-encode bytes.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DES/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the bytes of AES decryption for Base64-encode bytes
     */
    @Throws(Exception::class)
    fun decryptBase64DES(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String = "DES/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return decryptDES(Base64.decode(data, Base64.NO_WRAP), key, transformation, iv)
    }

    /**
     * Return the bytes of DES decryption.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DES/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the bytes of AES decryption
     */
    @Throws(Exception::class)
    fun decryptDES(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String = "DES/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return symmetricTemplate(data, key, "DES", transformation, iv, false)
    }

    //////////DESede Start

    /**
     * Return the Base64-encode bytes of DESede encryption.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DESede/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the Base64-encode bytes of AES encryption
     */
    @Throws(Exception::class)
    fun encryptDESede2Base64(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String? = "DESede/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return Base64.encode(encryptDESede(data, key, transformation, iv), Base64.NO_WRAP)
    }

    /**
     * Return the bytes of DESede encryption.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DESede/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the bytes of AES encryption
     */
    @Throws(Exception::class)
    fun encryptDESede(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String? = "DESede/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return symmetricTemplate(data, key, "DESede", transformation!!, iv, true)
    }


    /**
     * Return the bytes of DESede decryption for Base64-encode bytes.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DESede/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the bytes of AES decryption for Base64-encode bytes
     */
    @Throws(Exception::class)
    fun decryptBase64DESede(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String = "DESede/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return decryptDESede(Base64.decode(data, Base64.NO_WRAP), key, transformation, iv)
    }

    /**
     * Return the bytes of DESede decryption.
     *
     * @param data           The data.
     * @param key            The key.
     * @param transformation The name of the transformation,
     * 加密算法/加密模式/填充类型, *DESede/CBC/PKCS5Padding*.
     * @param iv             The buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @return the bytes of AES decryption
     */
    @Throws(Exception::class)
    fun decryptDESede(
        data: ByteArray?,
        key: ByteArray?,
        transformation: String = "DESede/ECB/PKCS5Padding",
        iv: ByteArray? = null
    ): ByteArray? {
        return symmetricTemplate(data, key, "DESede", transformation, iv, false)
    }

    /**
     * 非对称加密RSA加密和解密
     */
    /**
     * 私钥加密
     */
    fun encryptByPrivateKey(input: String, privateKey: PrivateKey): String {
        /********************非对称加/解密三部曲**********************/
        //1.创建cipher对象
        val cipher = Cipher.getInstance("RSA")
        //2.初始化cipher
        cipher.init(Cipher.ENCRYPT_MODE, privateKey)
        //3.加密/解密
        val encrypt = cipher.doFinal(input.toByteArray())
        return Base64.encodeToString(encrypt, Base64.NO_WRAP)
    }

    /**
     * 公钥解密
     */
    fun decryptByPublicKey(input: String, publicKey: PublicKey): String {
        val decode = Base64.decode(input, Base64.NO_WRAP)
        /********************非对称加/解密三部曲**********************/
        //1.创建cipher对象
        val cipher = Cipher.getInstance("RSA")
        //2.初始化cipher
        cipher.init(Cipher.DECRYPT_MODE, publicKey)
        //3.加密/解密
        val encrypt = cipher.doFinal(decode)
        return String(encrypt)
    }

    /**
     * 公钥加密
     */
    fun encryptByPublicKey(input: String, publicKey: PublicKey): String {
        /********************非对称加/解密三部曲**********************/
        //1.创建cipher对象
        val cipher = Cipher.getInstance("RSA")
        //2.初始化cipher
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        //3.加密/解密
        val encrypt = cipher.doFinal(input.toByteArray())
        return Base64.encodeToString(encrypt, Base64.NO_WRAP)
    }

    /**
     * 私钥解密
     */
    fun decryptByPrivateKey(input: String, privateKey: PrivateKey): String {
        val decode = Base64.decode(input, Base64.NO_WRAP)
        /********************非对称加/解密三部曲**********************/
        //1.创建cipher对象
        val cipher = Cipher.getInstance("RSA")
        //2.初始化cipher
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        //3.加密/解密
        val encrypt = cipher.doFinal(decode)
        return String(encrypt)
    }

    /**
     * 私钥分段加密
     */
    fun encryptSegmentByPrivateKey(input: String, privateKey: PrivateKey, keySize: Int = 2048): String {
        val byteArray = input.toByteArray()

        var temp:ByteArray
        var offset = 0 //当前偏移的位置
        val MAX_ENCRYPT_BLOCK = keySize / 8 - 11

        val bos = ByteArrayOutputStream()

        /********************非对称加/解密三部曲**********************/
        //1.创建cipher对象
        val cipher = Cipher.getInstance("RSA")
        //2.初始化cipher
        cipher.init(Cipher.ENCRYPT_MODE, privateKey)
        //3.加密：分段加密
        while (byteArray.size - offset > 0) { //没有加密完
            //每次最大加密 MAX_ENCRYPT_BLOCK 个字节
            if (byteArray.size - offset >= MAX_ENCRYPT_BLOCK) {
                //剩余部分大于 MAX_ENCRYPT_BLOCK, 加密完整MAX_ENCRYPT_BLOCK
                temp  = cipher.doFinal(byteArray, offset, MAX_ENCRYPT_BLOCK)
                offset += MAX_ENCRYPT_BLOCK
            } else {
                //加密最后一块
                temp  = cipher.doFinal(byteArray, offset, byteArray.size - offset)
                offset = byteArray.size
            }
            //存储到临时缓冲区
            bos.write(temp)
        }
        bos.close()
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * 公钥分段解密
     */
    fun decryptSegmentByPublicKey(input: String, publicKey: PublicKey, keySize: Int = 2048): String? {
        val byteArray = Base64.decode(input, Base64.NO_WRAP)

        var temp:ByteArray
        var offset = 0 //当前偏移的位置
        val MAX_DECRYPT_BLOCK = keySize / 8

        val bos = ByteArrayOutputStream()

        /********************非对称加/解密三部曲**********************/
        //1.创建cipher对象
        val cipher = Cipher.getInstance("RSA")
        //2.初始化cipher
        cipher.init(Cipher.DECRYPT_MODE, publicKey)
        //3.解密：分段解密
        while (byteArray.size - offset > 0) { //没有解密完
            //每次最大解密 MAX_DECRYPT_BLOCK 个字节
            if (byteArray.size - offset >= MAX_DECRYPT_BLOCK) {
                //剩余部分大于 MAX_DECRYPT_BLOCK, 解密完整 MAX_DECRYPT_BLOCK
                temp  = cipher.doFinal(byteArray, offset, MAX_DECRYPT_BLOCK)
                offset += MAX_DECRYPT_BLOCK
            } else {
                //解密最后一块
                temp  = cipher.doFinal(byteArray, offset, byteArray.size - offset)
                offset = byteArray.size
            }
            //存储到临时缓冲区
            bos.write(temp)
        }
        bos.close()

        return String(bos.toByteArray())
    }

    /**
     * 公钥分段加密
     */
    fun encryptSegmentByPublicKey(input: String, publicKey: PublicKey, keySize: Int = 2048): String {
        val byteArray = input.toByteArray()

        var temp:ByteArray
        var offset = 0 //当前偏移的位置
        val MAX_ENCRYPT_BLOCK = keySize / 8 - 11

        val bos = ByteArrayOutputStream()

        /********************非对称加/解密三部曲**********************/
        //1.创建cipher对象
        val cipher = Cipher.getInstance("RSA")
        //2.初始化cipher
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        //3.加密：分段加密
        while (byteArray.size - offset > 0) { //没有加密完
            //每次最大加密 MAX_ENCRYPT_BLOCK 个字节
            if (byteArray.size - offset >= MAX_ENCRYPT_BLOCK) {
                //剩余部分大于 MAX_ENCRYPT_BLOCK, 加密完整MAX_ENCRYPT_BLOCK
                temp  = cipher.doFinal(byteArray, offset, MAX_ENCRYPT_BLOCK)
                offset += MAX_ENCRYPT_BLOCK
            } else {
                //加密最后一块
                temp  = cipher.doFinal(byteArray, offset, byteArray.size - offset)
                offset = byteArray.size
            }
            //存储到临时缓冲区
            bos.write(temp)
        }
        bos.close()

        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * 私钥分段解密
     */
    fun decryptSegmentByPrivateKey(input: String, privateKey: PrivateKey, keySize: Int = 2048): String? {
        val byteArray = Base64.decode(input, Base64.NO_WRAP)

        var temp:ByteArray
        var offset = 0 //当前偏移的位置
        val MAX_DECRYPT_BLOCK = keySize / 8

        val bos = ByteArrayOutputStream()

        /********************非对称加/解密三部曲**********************/
        //1.创建cipher对象
        val cipher = Cipher.getInstance("RSA")
        //2.初始化cipher
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        //3.解密：分段解密
        while (byteArray.size - offset > 0) { //没有解密完
            //每次最大解密 MAX_DECRYPT_BLOCK 个字节
            if (byteArray.size - offset >= MAX_DECRYPT_BLOCK) {
                //剩余部分大于 MAX_DECRYPT_BLOCK, 解密完整 MAX_DECRYPT_BLOCK
                temp  = cipher.doFinal(byteArray, offset, MAX_DECRYPT_BLOCK)
                offset += MAX_DECRYPT_BLOCK
            } else {
                //解密最后一块
                temp  = cipher.doFinal(byteArray, offset, byteArray.size - offset)
                offset = byteArray.size
            }
            //存储到临时缓冲区
            bos.write(temp)
        }
        bos.close()

        return String(bos.toByteArray())
    }

    fun generateKeys(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        return generator.genKeyPair()
    }
}