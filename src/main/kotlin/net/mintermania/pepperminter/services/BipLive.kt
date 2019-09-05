package net.mintermania.pepperminter.services

import com.google.gson.JsonObject
import net.mintermania.pepperminter.config
import net.mintermania.pepperminter.db
import net.mintermania.pepperminter.dec
import net.mintermania.pepperminter.models.Profiles
import tel.egram.kuery.eq
import tel.egram.kuery.rangeTo
import tel.egram.kuery.sqlite.SQLiteDialect
import java.math.BigInteger
import java.net.URL
import java.net.URLEncoder

class BipLive(val data: JsonObject) {

    fun run() {
        val payload = data["payload"].asString.dec()

        if (payload.startsWith("-PEPPER-COMMENT-")) {
            return
        } else if (payload.startsWith("-PEPPER-DIRECT-")) {
            return
        } else if (payload.startsWith("-----BEGIN PUBLIC KEY-----") && payload.endsWith("-----END PUBLIC KEY-----")) {
            return
        } else if (payload.startsWith("-PEPPER-")) {
            return
        }

        val from = data["from"].asString
        val to = data["data"].asJsonObject["to"].asString
        val res1 =
            db.prepareStatement(
                tel.egram.kuery.from((Profiles)).where { e -> (e.address eq from) }.select { e -> e.title..e.fish..e.www..e.isVerified }.toString(
                    SQLiteDialect
                )
            )
                .executeQuery()
        val res2 =
            db.prepareStatement(
                tel.egram.kuery.from((Profiles)).where { e -> (e.address eq to) }.select { e -> e.title..e.fish..e.www..e.isVerified }.toString(
                    SQLiteDialect
                )
            )
                .executeQuery()

        val pfrom: Map<String, Any>?
        val pto: Map<String, Any>?

        res1.next()
        pfrom = mapOf(
            "title" to res1.getString("title"),
            "fish" to res1.getString("fish"),
            "www" to res1.getString("www"),
            "isVerified" to res1.getBoolean("isVerified")
        ) //TODO: just to make code better for reading / working with ide need to make it via Profiles/Transactions.*

        pto = if (res2.next())
            mapOf(
                "title" to res2.getString("title"),
                "fish" to res2.getString("fish"),
                "www" to res2.getString("www"),
                "isVerified" to res2.getBoolean("isVerified")
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

        try {
            URL(
                "https://api.telegram.org/bot" + config!!.node("telegram").get(
                    "bot_token",
                    ""
                ) + "/sendMessage?chat_id=" + config!!.node("telegram").get(
                    "channel_id",
                    ""
                ) + "&parse_mode=HTML&disable_web_page_preview=true&text=" + URLEncoder.encode(
                    m.toString(), Charsets.UTF_8
                )
            ).readText()
        } catch (e: Exception) {
            println("biplive exception")
//        e.printStackTrace()
        }

    }

}