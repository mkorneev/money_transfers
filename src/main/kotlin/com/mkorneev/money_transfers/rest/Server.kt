package com.mkorneev.money_transfers.rest

import spark.Spark.awaitInitialization
import spark.Spark.path
import spark.kotlin.get
import spark.kotlin.port
import spark.kotlin.post
import kotlin.system.exitProcess

const val docsURL = "https://github.com/mkorneev/money_transfers/blob/master/docs/api/index.md"

fun main(args: Array<String>) {
    val requestedPort = parsePort(args)
    port(requestedPort)

    get("/") { redirect("/docs") }

    get("/docs") { """<html><body>
        |<h3>RESTful API for money transfers between accounts</h3>
        |<p>Please refer to API documentation at <a href="$docsURL">$docsURL</a></p>
        |</body></html>""".trimMargin() }

    path("/api") {
        path("/accounts") {
            get("/:account", function = AccountApi.show)
            post("", function = AccountApi.open)

            get("/:account/balance", function = AccountApi.balance)
            post("/:account/deposit", function = AccountApi.deposit)
            post("/:account/withdraw", function = AccountApi.withdraw)
        }

        post("/transfer", function = AccountApi.transfer)
    }

    awaitInitialization()  // Wait for server to be initialized

    println("\nReady to serve requests at http://localhost:$requestedPort\n")
}

private fun parsePort(args: Array<String>): Int {
    if (args.size != 1) printUsageAndExit()
    val port = Integer.valueOf(args[0])
    if (port == null) printUsageAndExit()
    return port
}

private fun printUsageAndExit() {
    println("""Usage:
        | ./gradlew run --args <port>
    """.trimMargin())
    exitProcess(1)
}