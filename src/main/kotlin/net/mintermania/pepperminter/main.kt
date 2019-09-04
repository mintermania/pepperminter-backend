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