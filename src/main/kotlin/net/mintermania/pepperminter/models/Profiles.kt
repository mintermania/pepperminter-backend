package net.mintermania.pepperminter.models

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.mintermania.pepperminter.config
import net.mintermania.pepperminter.db
import tel.egram.kuery.*
import tel.egram.kuery.sqlite.SQLiteDialect
import java.net.URL

object Profiles : Table("profiles") {
    var address = Column("address")
    val description = Column("description")
    val www = Column("www")
    val icon = Column("icon")
    val isVerified = Column("isVerified")
    val title = Column("title")
    val fish = Column("fish")

    fun add(address: String){

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
}