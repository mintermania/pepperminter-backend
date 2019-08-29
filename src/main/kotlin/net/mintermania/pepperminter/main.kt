package net.mintermania.pepperminter

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ini4j.Ini
import org.ini4j.IniPreferences
import spark.kotlin.Http
import spark.kotlin.ignite
import tel.egram.kuery.*
import tel.egram.kuery.sqlite.SQLiteDialect
import java.io.File
import java.lang.System.getProperty
import java.math.BigInteger
import java.net.URL
import java.net.URLEncoder
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import kotlin.text.Charsets.UTF_8

// @backend based on Kotlin & SparkJava was powered by MinterMania (mpay.ms) for Minter Twitter challenge 2019
// v: 1.7
//TODO: types and just more cool features


val db: Connection = DriverManager.getConnection("jdbc:sqlite:${getProperty("user.home")}/.PepperMinter.sqlite")
private fun String.dec(): String = String(Base64.getDecoder().decode(this))


var config: IniPreferences? = null

fun main() {
    if (!File("${getProperty("user.home")}/.PepperMinter.ini").isFile) {

        val sqls = """
            create table profiles
(
    address     VARCHAR(42)
        constraint profiles_pk
            primary key,
    description TEXT,
    www         TEXT,
    icon        TEXT,
    isVerified  boolean,
    title       text,
    fish        text
);

create table transactions
(
    hash    VARCHAR(64) not null
        constraint transactions_pk
            primary key,
    block   INT         not null,
    a_from  VARCHAR(42) not null,
    a_to    VARCHAR(42) not null,
    amount  NUMERIC(18) default 0 not null,
    payload INT,
    date    DATETIME,
    nonce   INT         default 0 not null
);
        """.trimIndent().split("\n\n")

        for (sql in sqls)
            db.prepareStatement(sql).execute()

        println("Database file was created successfully on ${getProperty("user.home")}/.PepperMinter.sqlite")

        File("${getProperty("user.home")}/.PepperMinter.ini").writeText(
            """
            [general]
            node_api = https://api.minter.stakeholder.space/
            minterscan_api = https://minterscan.pro/
            port = 7777
            
            [telegram]
            poster = false
            bot_token = 1:A
            channel_id = @abc
        """.trimIndent()
        )


        println("Config file was created successfully on ${getProperty("user.home")}/.PepperMinter.ini")
        println("")
        println("In order to get this app working, change config file and run this program again")

    } else {
        config = IniPreferences(Ini(File("${getProperty("user.home")}/.PepperMinter.ini")))
//        println("grumpy/homePage: " + prefs.node("grumpy").get("homePage", null))

        val http: Http = ignite()
        http.port(config!!.node("general").getInt("port", 7777))

        http.get("/stop") {
            if (request.host().split(":")[0] != request.ip())
                "ERROR. ACCESS DENIED"
            else {
                db.close()
                http.stop()
            }
        }

        // order = old | new
        http.get("/tweets") {
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
                map["payload"] = data.getString("payload")
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

        http.get("/profile") {
            response.type("application/json;charset=utf-8")

            val address: String? = if (request.queryParams("address") != null) request.queryParams("address") else null

            if (address == null || address.length != 42) {
                "Error."
            } else {

                val q = db.prepareStatement(
                    from(Profiles)
                        .where { e ->
                            e.address eq address
                        }
                        .select { e -> e.description..e.fish..e.isVerified..e.icon..e.title..e.www }
                        .toString(SQLiteDialect)
                )
                val data = q.executeQuery()

                val arr = TreeMap<String, Any?>()

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


        GlobalScope.launch {
            worker()
        }
    }


}

// posting to telegram (thanx for the idea, biplive)
fun telegramPoster(data: JsonObject) {

    val from = data["from"].asString
    val to = data["data"].asJsonObject["to"].asString
    val res =
        db.prepareStatement(
            from((Profiles)).where { e -> (e.address eq from) or (e.address eq to) }.select { e -> e.title..e.fish..e.www..e.isVerified }.toString(
                SQLiteDialect
            )
        )
            .executeQuery()

    val pfrom: Map<String, Any>?
    val pto: Map<String, Any>?

    res.next()
    pfrom = mapOf(
        "title" to res.getString("title"),
        "fish" to res.getString("fish"),
        "www" to res.getString("www"),
        "isVerified" to res.getBoolean("isVerified")
    ) //TODO: just to make code better for reading / working with ide need to make it via Profiles/Transactions.*

    pto = if (res.next())
        mapOf(
            "title" to res.getString("title"),
            "fish" to res.getString("fish"),
            "www" to res.getString("www"),
            "isVerified" to res.getBoolean("isVerified")
        )
    else
        pfrom

    val m = StringBuilder()
    if (pfrom["www"] != null)
        m.append("<a href='${(pfrom["www"] as String)}'>" + pfrom["fish"] + "</a> ")
    else
        m.append("<a href='https://minterscan.net/address/$from'>" + pfrom["fish"] + "</a> ")

    fun app(p: Map<String, Any>?, addr: String) {
        if (!(p!!["isVerified"] as Boolean))
            m.append("<a href='https://minterscan.net/address/$addr'>" + addr.slice(0..5) + "…" + addr.slice(38..41) + "</a>")
        else
            m.append(
                (p["title"] as String) + " (<a href='https://minterscan.net/address/$addr'>" + addr.slice(0..3) + "…" + addr.slice(
                    40..41
                ) + "</a>)"
            )
    }

    app(pfrom, from)

    if (from != to) {

        if (pto["www"] != null)
            m.append("  ⇒ <a href='${(pto["www"] as String)}'>" + pto["fish"] + "</a> ")
        else
            m.append("  ⇒ <a href='https://minterscan.net/address/$to'>" + pto["fish"] + "</a> ")

        app(pto, to)

    }
    m.append("\n\n" + data["payload"].asString.dec())

    m.append(
        "\n\n<a href='https://minterscan.net/tx/Mt${(data["hash"].asString).toLowerCase()}'>" + data["data"].asJsonObject["value"].asBigInteger.divide(
            BigInteger.valueOf(1000000000000000000)
        )
                + " ping</a>"
    )
//    m.append("   ") formatting below is shit, sorry
    URL(
        "https://api.telegram.org/bot" + config!!.node("telegram").get(
            "bot_token",
            ""
        ) + "/sendMessage?chat_id=" + config!!.node("telegram").get(
            "channel_id",
            ""
        ) + "&parse_mode=HTML&disable_web_page_preview=true&text=" + URLEncoder.encode(
            m.toString(), UTF_8
        )
    ).readText()
}

// biplive/coinfeed implementation
suspend fun worker() {
    GlobalScope.launch {
        sync()
    }

    var last = 0
    while (true) {

        val block = Minter.block()

        if (block == last) {
            delay(1000)
            continue
        }

        last = block

        val transactions = Minter.transactions(block)

        for (t in transactions)
            add(t as JsonObject, block)

        println("[block] $block")
        delay(1000)
    }
}

// add profile of address to db (thanx, minterscan.net)
fun add(address: String) {

    val minterscan = config!!.node("general").get("minterscan_api", "https://minterscan.pro/")

    var data: JsonObject? = null
    var fish: String? = null
    try {
        fish = URL(minterscan + "addresses/$address/icon").readText()

        data =
            JsonParser().parse(URL(minterscan + "profiles/$address").readText()).asJsonObject
    } catch (e: Exception) {
//        e.printStackTrace()
    }

    val upd: (Profiles) -> Iterable<tel.egram.kuery.dml.Assignment>

    if (fish == null && data == null) return
    else if (fish != null && data != null) {
        upd = { e: Profiles ->
            e.address(address)..
                    e.description(data["description"].asString)..
                    e.www(data["www"].asString)..
                    e.icon(data["icons"].asJsonObject["jpg"].asString)..
                    e.isVerified(data["isVerified"].asBoolean)..
                    e.title(data["title"].asString)..
                    e.fish(fish)
        }
    } else if (fish == null && data != null) {
        upd = { e: Profiles ->
            e.address(address)..
                    e.description(data["description"].asString)..
                    e.www(data["www"].asString)..
                    e.icon(data["icons"].asJsonObject["jpg"].asString)..
                    e.isVerified(data["isVerified"].asBoolean)..
                    e.title(data["title"].asString)
        }

    } else {
        upd = { e: Profiles ->
            e.address(address)..
                    e.fish(fish)
        }
    }

    val q = from(Profiles)
        .where { e -> e.address eq address }
        .select { e -> e.title }.toString(SQLiteDialect)

    val request = if (db.prepareStatement(q).executeQuery().next())
        from(Profiles)
            .where { e -> e.address eq address }
            .update(upd).toString(SQLiteDialect)
    else
        into(Profiles)
            .insert(upd).toString(SQLiteDialect)


    db.prepareStatement(request).execute()
}

// add transaction to db
fun add(data: JsonObject, height: Int = 0) {
    if (data["type"].asInt != 1 || data["data"].asJsonObject["coin"].asString != "PING" || data["payload"] == null || data["payload"].asString.isEmpty()) return

    //println(data)

    val q = from(Transactions)
        .where { e -> e.hash eq data["hash"].asString }
        .select { e -> e.payload }.toString(SQLiteDialect)

    if (db.prepareStatement(q).executeQuery().next()) return


    val query = into(Transactions).insert { e ->
        e.hash(data["hash"].asString)..
                e.block(if (height == 0) data["height"].asInt else height)..
                e.a_from(data["from"].asString)..
                e.a_to(data["data"].asJsonObject["to"].asString)..
                e.amount(data["data"].asJsonObject["value"].asBigInteger)..
                e.payload(data["payload"].asString.dec())..
                e.date(Minter.time(if (height == 0) data["height"].asInt else height))..
                e.nonce(data["nonce"].asInt)
    }.toString(SQLiteDialect)

    db.prepareStatement(query).execute()

    add(data["from"].asString)
    if (data["data"].asJsonObject["to"].asString != data["from"].asString)
        add(data["data"].asJsonObject["to"].asString)

    if (config!!.node("telegram").getBoolean("poster", false))
        telegramPoster(data)
}

// sync with blockchain
suspend fun sync() {
    val data = Minter.all()

    for (d in data) add(d.asJsonObject)
}


// working with minter api
object Minter {
    val NODE: String = config!!.node("general").get("node_api", "https://api.minter.stakeholder.space/")
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

// table for db
object Transactions : Table("transactions") {
    var hash = Column("hash")
    val block = Column("block")
    val a_from = Column("a_from")
    val a_to = Column("a_to")
    val amount = Column("amount")
    val payload = Column("payload")
    val date = Column("date")
    val nonce = Column("nonce")
}

// table for db
object Profiles : Table("profiles") {
    var address = Column("address")
    val description = Column("description")
    val www = Column("www")
    val icon = Column("icon")
    val isVerified = Column("isVerified")
    val title = Column("title")
    val fish = Column("fish")
}