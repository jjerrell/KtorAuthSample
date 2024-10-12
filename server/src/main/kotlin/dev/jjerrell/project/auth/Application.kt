package dev.jjerrell.project.auth

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    setupRouting()
    setupPlugins()
}

private fun Application.setupRouting() {
    val httpClient = HttpClient(CIO) { install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json() } }
    setupAuthorization(httpClient)
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        route("debug") {
            get("session") {
                val sessionData = call.getSession()
                sessionData?.let { call.respond("State: ${it.state};\nToken: ${it.token}\nID: ${it.userInfo.id}") }
            }
            get("profile") {
                val sessionData = call.getSession()
                sessionData?.let {
                    val profile = getProfile(httpClient = httpClient, token = it.token)
                    call.respond(profile)
                }
            }
        }
    }
}

private fun Application.setupPlugins() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
            }
        )
    }

    install(IgnoreTrailingSlash)
}