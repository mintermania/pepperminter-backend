package net.mintermania.pepperminter.services

import com.google.gson.JsonObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mintermania.pepperminter.models.Minter
import net.mintermania.pepperminter.models.Transactions

class Worker {
    suspend fun run() {
        GlobalScope.launch {
            Syncer().run()
        }


        var last = 0
        while (true) {
            try {
                val block = Minter.block()

                if (block == last) {
                    delay(1000)
                    continue
                }

                last = block

                val transactions = Minter.transactions(block)

                for (t in transactions)
                    Transactions.add(t as JsonObject, block)

                println("[block] $block")
                delay(1000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}