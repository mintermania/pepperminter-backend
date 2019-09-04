package net.mintermania.pepperminter.models

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import net.mintermania.pepperminter.config
import java.net.URL

object Minter {
    private val NODE = config!!.node("general").get("node_api", "https://api.minter.stakeholder.space/")

    fun block() = send("status")["result"].asJsonObject["latest_block_height"].asInt

    fun time(block: Int) = send("block?height=$block")["result"].asJsonObject["time"].asString!!

    suspend fun all(): JsonArray {
        val arr = JsonArray()
        var page = 1
        var last = 0

        while (true) {
            val t = URL(NODE + "transactions?query=\"tags.tx.coin='PING'\"&page=$page").readText()

            if (last == t.length) // pretty dangerous stuff here, not really reliable, but it works :D
                break

            last = t.length
            page++

            val current = JsonParser().parse(t).asJsonObject["result"].asJsonArray

            arr.addAll(current)
            delay(200)
        }

//        println("DONE!!!")
//        println(page)
//        println(arr)

        return arr

    }

    fun transactions(block: Int) =
        send("block?height=$block")["result"].asJsonObject["transactions"].asJsonArray!!

    private fun send(method: String) =
        JsonParser().parse(URL(NODE + method).readText()).asJsonObject
}