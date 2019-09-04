package net.mintermania.pepperminter.services

import net.mintermania.pepperminter.config
import net.mintermania.pepperminter.db
import org.ini4j.Ini
import org.ini4j.IniPreferences
import java.io.File

class Config {

    val file = "${System.getProperty("user.home")}/.PepperMinter.ini"

    fun init() {
        config = IniPreferences(Ini(File(file)))
    }

    fun create() {

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
    type        TEXT default 'tweet',
    subtype        TEXT default null,
    type_special        TEXT default null,
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

        println("Database file was created successfully on ${System.getProperty("user.home")}/.PepperMinter.sqlite")

        File(file).writeText(
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


        println("Config file was created successfully on ${System.getProperty("user.home")}/.PepperMinter.ini")
        println("")
        println("In order to get this app working, change config file and run this program again")

    }
}