package net.mintermania.pepperminter.models

import com.google.gson.JsonObject
import net.mintermania.pepperminter.config
import net.mintermania.pepperminter.db
import net.mintermania.pepperminter.dec
import net.mintermania.pepperminter.enc
import net.mintermania.pepperminter.services.BipLive
import tel.egram.kuery.*
import tel.egram.kuery.sqlite.SQLiteDialect

object Transactions : Table("transactions") {
    var hash = Column("hash")
    val block = Column("block")
    val type = Column("type")
    val type_special = Column("type_special")
    val subtype = Column("subtype")
    val a_from = Column("a_from")
    val a_to = Column("a_to")
    val amount = Column("amount")
    val payload = Column("payload")
    val date = Column("date")
    val nonce = Column("nonce")

    fun add(data: JsonObject, height: Int = 0) {
        if (data["type"].asInt != 1 || data["data"].asJsonObject["coin"].asString != "PING" || data["payload"] == null || data["payload"].asString.isEmpty()) return

        //println(data)

        val q = from(Transactions)
            .where { e -> e.hash eq data["hash"].asString }
            .select { e -> e.payload }.toString(SQLiteDialect)

        if (db.prepareStatement(q).executeQuery().next()) return

        val payload = data["payload"].asString.dec()
        var type = "tweet"
        var type_special: String? = null

        if (payload.startsWith("-PEPPER-COMMENT-")) {
            type = "comment"
            type_special = payload.split(" ")[1]
        } else if (payload.startsWith("-PEPPER-DIRECT-")) {
            type = "direct"
//            type_special = payload.split(" ")[1]
        } else if (payload.startsWith("-----BEGIN PUBLIC KEY-----") && payload.endsWith("-----END PUBLIC KEY-----")) {
            type = "publickey"
//            if (payload.contains("-----BEGIN PRIVATE ENCRYPTED KEY-----") && payload.contains("-----END PRIVATE ENCRYPTED KEY-----"))
//                type = "keys"
        } else if (payload.startsWith("-PEPPER-")) {
            type = "privatekey"
//            type_special = payload.split("\n")[1].dec()
        }
//    else if(payload.startsWith("sca")) //TODO: finish this stuff

        val query = into(Transactions).insert { e ->
            e.hash(data["hash"].asString.replace("Mt", "").toUpperCase())..
                    e.block(if (height == 0) data["height"].asInt else height)..
                    e.type(type)..
                    e.type_special(type_special)..
                    e.a_from(data["from"].asString)..
                    e.a_to(data["data"].asJsonObject["to"].asString)..
                    e.amount(data["data"].asJsonObject["value"].asBigInteger)..
                    e.payload(payload.enc())..
                    e.date(Minter.time(if (height == 0) data["height"].asInt else height))..
                    e.nonce(data["nonce"].asInt)
        }.toString(SQLiteDialect)

        db.prepareStatement(query).execute()

        Profiles.add(data["from"].asString)
        if (data["data"].asJsonObject["to"].asString != data["from"].asString)
            Profiles.add(data["data"].asJsonObject["to"].asString)

        if (config!!.node("telegram").getBoolean("poster", false))
            BipLive(data)
    }
}