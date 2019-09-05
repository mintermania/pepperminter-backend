package net.mintermania.pepperminter.services

import net.mintermania.pepperminter.models.Minter
import net.mintermania.pepperminter.models.Transactions

class Syncer {
    suspend fun run() {
        val data = Minter.all()

        for (d in data) {
            try {
                Transactions.add(d.asJsonObject)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}