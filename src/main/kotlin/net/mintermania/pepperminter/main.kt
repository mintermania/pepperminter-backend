package net.mintermania.pepperminter

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mintermania.pepperminter.services.Api
import net.mintermania.pepperminter.services.Config
import net.mintermania.pepperminter.services.Worker
import org.ini4j.IniPreferences
import java.io.File
import java.lang.System.getProperty
import java.sql.Connection
import java.sql.DriverManager
import java.util.*


// @backend based on Kotlin & SparkJava was powered by MinterMania (mpay.ms) for Minter Twitter challenge 2019
//TODO: types and just more cool features


val db: Connection = DriverManager.getConnection("jdbc:sqlite:${getProperty("user.home")}/.PepperMinter.sqlite")
fun String.dec(): String = String(Base64.getDecoder().decode(this))
fun String.enc(): String = Base64.getEncoder().encodeToString(this.toByteArray())


var config: IniPreferences? = null

fun main() {

    if (!File(Config().file).isFile)
        Config().create()
    else {
        Config().init()

        Api().run()

        GlobalScope.launch {
            Worker().run()
        }
    }

//
//    val hashedStr = String.format(
//        "%032x",
//        BigInteger(
//            1,
//            MessageDigest.getInstance("MD5").digest(
//                "-----BEGIN PUBLIC KEY-----\nMIGJAoGBALCx9Zk4upPnwHQfqpIlkWFBdqLuWLt3eH9Nd3+k5hRRWceBmzh4NCxeQvkDAdrVjkpqAmGpu0kDslEFwEQ/cffeL9+2SjdUMc8Epo/5d+CbcHYYx5VStjAL0tChYqDJMBxo1XY8xoD1PPSx2/+o/WK6yKj1+rJNrzgPYF0K8ODlAgMBAAE=\n-----END PUBLIC KEY-----".toByteArray(
//                    Charsets.UTF_8
//                )
//            )
//        )
//    )
//
//    val md5Hex = DigestUtils
//        .md5Hex("-----BEGIN PUBLIC KEY-----\nMIGJAoGBALCx9Zk4upPnwHQfqpIlkWFBdqLuWLt3eH9Nd3+k5hRRWceBmzh4NCxeQvkDAdrVjkpqAmGpu0kDslEFwEQ/cffeL9+2SjdUMc8Epo/5d+CbcHYYx5VStjAL0tChYqDJMBxo1XY8xoD1PPSx2/+o/WK6yKj1+rJNrzgPYF0K8ODlAgMBAAE=\n-----END PUBLIC KEY-----")
//
//
//    print(hashedStr)
//    print("\n")
//    print(md5Hex)
//
//    fun getMd5Base64(encTarget: ByteArray): String? {
//        val mdEnc: MessageDigest?
//        try {
//            mdEnc = MessageDigest.getInstance("MD5")
//            // Encryption algorithmy
//            val md5Base16 = BigInteger(1, mdEnc.digest(encTarget))     // calculate md5 hash
//            return Base64.getEncoder().encodeToString(md5Base16.toByteArray())
//                .trim()     // convert from base16 to base64 and remove the new line character
//        } catch (e: NoSuchAlgorithmException) {
//            e.printStackTrace()
//            return null
//        }
//    }
//    print("\n")
//    print(
//        getMd5Base64(
//            "-----BEGIN PUBLIC KEY-----\nMIGJAoGBALCx9Zk4upPnwHQfqpIlkWFBdqLuWLt3eH9Nd3+k5hRRWceBmzh4NCxeQvkDAdrVjkpqAmGpu0kDslEFwEQ/cffeL9+2SjdUMc8Epo/5d+CbcHYYx5VStjAL0tChYqDJMBxo1XY8xoD1PPSx2/+o/WK6yKj1+rJNrzgPYF0K8ODlAgMBAAE=\n-----END PUBLIC KEY-----".toByteArray(
//                Charsets.UTF_8
//            )
//        )
//    )

//    //print(Base64.getDecoder().decode("SuD/8poH5f2bThSiFfKKxQ=="))
//
//    val md = MessageDigest.getInstance("MD5")
//    val a = BigInteger(1, md.digest(Base64.getDecoder().decode("SuD/8poH5f2bThSiFfKKxQ=="))).toString(16).padStart(32, '0')
//
//    print(a)
//    print(" ")
//    val b = BigInteger(1, md.digest("0\u0081\u0089\u0002\u0081\u0081".toByteArray())).toString(16).padStart(32, '0')
//
//    print(b)
}