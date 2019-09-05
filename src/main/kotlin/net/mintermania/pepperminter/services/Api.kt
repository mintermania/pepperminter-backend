package net.mintermania.pepperminter.services

import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mintermania.pepperminter.config
import net.mintermania.pepperminter.db
import net.mintermania.pepperminter.dec
import net.mintermania.pepperminter.models.Minter
import net.mintermania.pepperminter.models.Profiles
import net.mintermania.pepperminter.models.Transactions
import spark.kotlin.Http
import spark.kotlin.ignite
import tel.egram.kuery.*
import tel.egram.kuery.sqlite.SQLiteDialect
import java.util.*

class Api {
    fun run() {

        val http: Http = ignite()

        fun fixdemo(response: spark.Response, request: spark.Request) {
            response.header("Access-Control-Allow-Origin", "*")

            val accessControlRequestHeaders = request
                .headers("Access-Control-Request-Headers")
            if (accessControlRequestHeaders != null) {
                response.header(
                    "Access-Control-Allow-Headers",
                    accessControlRequestHeaders
                )
            }

            val accessControlRequestMethod = request
                .headers("Access-Control-Request-Method")
            if (accessControlRequestMethod != null) {
                response.header(
                    "Access-Control-Allow-Methods",
                    accessControlRequestMethod
                )
            }
        }

        http.port(config!!.node("general").getInt("port", 7777))

        fun comments(from: String, nonce: Int): Int {
            val data =
                db.prepareStatement("SELECT COUNT() FROM transactions WHERE a_to = '$from' AND type = 'comment' AND type_special = '$nonce'")
                    .executeQuery()
            data.next()
            return data.getInt(1)
        }


        http.get("/stop") {
            if (request.host().split(":")[0] != request.ip())
                "ERROR. ACCESS DENIED"
            else {
                db.close()
                http.stop()
                "DONE"
            }
        }

        http.get("/sync") {
            if (request.host().split(":")[0] != request.ip())
                "ERROR. ACCESS DENIED"
            else {
                GlobalScope.launch {
                    Syncer().run()
                }
                "DONE"
            }
        }

        // order = old | new
        http.get("/tweets") {

            fixdemo(response, request)


            response.type("application/json;charset=utf-8")

            // false = old
            val newFirst = request.queryParams("first") != "old"
            val needp: Boolean =
                if (request.queryParams("profiles") != null) request.queryParams("profiles")!!.toBoolean() else true
            //TODO: need make this cool and beautiful :) now it sucks
            val onPage: Int = if (request.queryParams("on_page") != null) request.queryParams("on_page").toInt() else 50
            val page: Int = if (request.queryParams("page") != null) request.queryParams("page").toInt() else 1
            val from: String? = if (request.queryParams("from") != null) request.queryParams("from") else null
            val to: String? = if (request.queryParams("to") != null) request.queryParams("to") else null
            val block: Number =
                if (request.queryParams("block") != null) request.queryParams("block").toInt() else Minter.block()
            val skipAir: Boolean =
                if (request.queryParams("skip_air") != null) request.queryParams("skip_air")!!.toBoolean() else false

            val q = db.prepareStatement(
                from(Transactions)
                    .where { e ->
                        (if (from != null) e.a_from eq from else e.a_from ne "a") and
                                (if (to != null) e.a_to eq to else e.a_to ne "b") and
                                (if (skipAir) e.a_to ne e.a_from else e.a_from ne "c") and
                                (e.block lt block) and
                                (e.type eq "tweet")
                    }
                    .orderBy { e -> (if (newFirst) e.block.desc else e.block.asc) }
                    .limit { onPage + 1 }
                    .offset { onPage * (page - 1) }
                    .select { e -> e.hash..e.block..e.a_from..e.a_to..e.amount..e.payload..e.date..e.nonce }
                    .toString(SQLiteDialect)
            )

            val data = q.executeQuery()

            val arr = ArrayList<TreeMap<String, Any>>()

            val profiles = mutableSetOf<String>()

            var next = false
            while (data.next()) {
                if (data.row == onPage + 1) {
                    next = true
                    continue
                }

                val map = TreeMap<String, Any>()
                map["hash"] = data.getString("hash")
                map["block"] = data.getInt("block")
                map["from"] = data.getString("a_from")
                map["to"] = data.getString("a_to")
                map["amount"] = data.getString("amount")
                map["payload"] = data.getString("payload").dec()
                map["date"] = data.getString("date")
                map["nonce"] = data.getInt("nonce")

                map["comments"] = comments(data.getString("a_from"), data.getInt("nonce"))


                profiles.add(map["from"] as String)
                profiles.add(map["to"] as String)

                arr.add(map)
            }

            val general = TreeMap<String, Any>()

            general["next"] = next
            general["prev"] = page > 1
            general["block"] = block
            general["tweets"] = arr

            if (needp) { // if we want to receive profiles
                val arr2 = TreeMap<String, TreeMap<String, Any?>>()

                val p =
                    db.prepareStatement(
                        "SELECT * FROM profiles WHERE address IN (${profiles.joinToString(
                            separator = "','",
                            prefix = "'",
                            postfix = "'"
                        )})"
                    )
                        .executeQuery()

                while (p.next()) {
                    val map = TreeMap<String, Any?>()
                    map["description"] = p.getString("description")
                    map["www"] = p.getString("www")
                    map["icon"] = p.getString("icon")
                    map["isVerified"] = p.getBoolean("isVerified")
                    map["title"] = p.getString("title")
                    map["fish"] = p.getString("fish")

                    arr2[p.getString("address")] = map
                }
                general["profiles"] = arr2

            }

            Gson().toJson(general)
        }

        http.get("/profile") {

            fixdemo(response, request)


            response.type("application/json;charset=utf-8")

            val address: String? = if (request.queryParams("address") != null) request.queryParams("address") else null

            if (address == null || address.length != 42)
                "Error."
            else {
                val arr = TreeMap<String, Any?>()

                val pubkey =
                    db.prepareStatement(from(Transactions)
                        .where { e -> (e.a_from eq address) and (e.type eq "publickey") }
                        .limit { 1 }
                        .select { e -> e.payload }
                        .toString(SQLiteDialect)
                    ).executeQuery()

                if (pubkey.next()) {
//                    val keys = twokeys.getString(Transactions.payload.name).split("-----END PUBLIC KEY-----")
//                    val public = keys[0] + "-----END PUBLIC KEY-----"
//                    val private = keys[1]

//                    println(keys)
                    arr["public"] = pubkey.getString("payload").dec()
                }

                val privatekey =
                    db.prepareStatement(from(Transactions)
                        .where { e -> (e.a_from eq address) and (e.type eq "privatekey") }
                        .limit { 1 }
                        .select { e -> e.payload }
                        .toString(SQLiteDialect)
                    ).executeQuery()

                if (privatekey.next()) {
//                    val keys = twokeys.getString(Transactions.payload.name).split("-----END PUBLIC KEY-----")
//                    val public = keys[0] + "-----END PUBLIC KEY-----"
//                    val private = keys[1]

//                    println(keys)
                    arr["private"] = privatekey.getString("payload").dec()
                }

//                else {
//                    val pubkey =
//                        db.prepareStatement(from(Transactions)
//                            .where { e -> (e.a_from eq address) and (e.type eq "publickey") }
//                            .limit { 1 }
//                            .select { e -> e.payload }
//                            .toString(SQLiteDialect)
//                        ).executeQuery()
//
//                    if (pubkey.next()) {
//                        val public = pubkey.getString(Transactions.payload.name)
//                        arr["public"] = public
//                        //TODO: if keys were sent as two transactions
//                    }
//                }

                val q = db.prepareStatement(
                    from(Profiles)
                        .where { e ->
                            e.address eq address
                        }
                        .select { e -> e.description..e.fish..e.isVerified..e.icon..e.title..e.www }
                        .toString(SQLiteDialect)
                )
                val data = q.executeQuery()


                if (data.next()) {
                    arr["description"] = data.getString("description")
                    arr["www"] = data.getString("www")
                    arr["icon"] = data.getString("icon")
                    arr["isVerified"] = data.getBoolean("isVerified")
                    arr["title"] = data.getString("title")
                    arr["fish"] = data.getString("fish")
                }


                val general = TreeMap<String, Any>()

                general["profile"] = arr


                Gson().toJson(general)
            }
        }

        http.get("/comments") {

            fixdemo(response, request)


            response.type("application/json;charset=utf-8")

            // false = old
            val newFirst = request.queryParams("first") != "old"
            val needp: Boolean =
                if (request.queryParams("profiles") != null) request.queryParams("profiles")!!.toBoolean() else true
            //TODO: need make this cool and beautiful :) now it sucks
            val onPage: Int = if (request.queryParams("on_page") != null) request.queryParams("on_page").toInt() else 50
            val page: Int = if (request.queryParams("page") != null) request.queryParams("page").toInt() else 1
            val block: Number =
                if (request.queryParams("block") != null) request.queryParams("block").toInt() else Minter.block()


            val tweet_from: String? =
                if (request.queryParams("tweet_from") != null) request.queryParams("tweet_from") else null
            val tweet_nonce: Int? =
                if (request.queryParams("tweet_nonce") != null) request.queryParams("tweet_nonce").toInt() else null

            if (tweet_from == null || tweet_nonce == null)
                "Error."
            else {

                val q = db.prepareStatement(
                    from(Transactions)
                        .where { e ->
                            (e.a_to eq tweet_from) and
                                    (e.type eq "comment") and
                                    (e.type_special eq tweet_nonce.toString()) and
                                    (e.block lt block)
                        }
                        .orderBy { e -> (if (newFirst) e.block.desc else e.block.asc) }
                        .limit { onPage + 1 }
                        .offset { onPage * (page - 1) }
                        .select { e -> e.hash..e.block..e.a_from..e.a_to..e.amount..e.payload..e.date..e.nonce }
                        .toString(SQLiteDialect)
                )


                val data = q.executeQuery()

                val arr = ArrayList<TreeMap<String, Any>>()

                val profiles = mutableSetOf<String>()
                var next = false
                while (data.next()) {


                    if (data.row == onPage + 1) {
                        next = true
                        continue
                    }

                    val map = TreeMap<String, Any>()
                    map["hash"] = data.getString("hash")
                    map["block"] = data.getInt("block")
                    map["from"] = data.getString("a_from")
                    map["to"] = data.getString("a_to")
                    map["amount"] = data.getString("amount")
                    map["payload"] = data.getString("payload").dec()
                    map["date"] = data.getString("date")
                    map["nonce"] = data.getInt("nonce")

                    profiles.add(map["from"] as String)
                    profiles.add(map["to"] as String)

                    arr.add(map)
                }

                val general = TreeMap<String, Any>()

                general["next"] = next
                general["prev"] = page > 1
                general["block"] = block
                general["tweets"] = arr

                if (needp) { // if we want to receive profiles
                    val arr2 = TreeMap<String, TreeMap<String, Any?>>()

                    val p =
                        db.prepareStatement(
                            "SELECT * FROM profiles WHERE address IN (${profiles.joinToString(
                                separator = "','",
                                prefix = "'",
                                postfix = "'"
                            )})"
                        )
                            .executeQuery()

                    while (p.next()) {
                        val map = TreeMap<String, Any?>()
                        map["description"] = p.getString("description")
                        map["www"] = p.getString("www")
                        map["icon"] = p.getString("icon")
                        map["isVerified"] = p.getBoolean("isVerified")
                        map["title"] = p.getString("title")
                        map["fish"] = p.getString("fish")

                        arr2[p.getString("address")] = map
                    }
                    general["profiles"] = arr2

                }

                Gson().toJson(general)
            }
        }

    }
}