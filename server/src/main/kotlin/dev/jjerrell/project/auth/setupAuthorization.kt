package dev.jjerrell.project.auth

import dev.jjerrell.project.auth.model.UserInfo
import dev.jjerrell.project.auth.model.UserSession
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

internal fun Application.setupAuthorization(httpClient: HttpClient) {
    install(Sessions) { cookie<UserSession>("user_session") }

    val redirects = mutableMapOf<String, String>()
    install(Authentication) {
        oauth("auth-oauth-google") {
            // Configure oauth authentication
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = System.getenv("GOOGLE_CLIENT_ID"),
                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile"),
                    extraAuthParameters = listOf("access_type" to "offline"),
                    onStateCreated = { call, state ->
                        // saves new state with redirect url value
                        call.request.queryParameters["redirectUrl"]?.let {
                            redirects[state] = it
                        }
                    }
                )
            }
            client = httpClient
        }
    }
    routing {
        authroute(httpClient, redirects)
        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }
    }
}

private fun Route.authroute(client: HttpClient, redirects: Map<String, String>): Route {
    return authenticate("auth-oauth-google") {
        get("/login") {
            // Redirects to 'authorizeUrl' automatically
        }

        get("/callback") {
            val currentPrincipal: OAuthAccessTokenResponse.OAuth2? = call.principal()
            // redirects home if the url is not found before authorization
            currentPrincipal?.let { principal ->
                principal.state?.let { state ->
                    val profile = getProfile(httpClient = client, token = principal.accessToken)
                    call.sessions.set(
                        UserSession(
                            state = state,
                            token = principal.accessToken,
                            userInfo = profile
                        )
                    )
                    redirects[state]?.let { redirect ->
                        call.respondRedirect(redirect)
                        return@get
                    }
                }
            }
            call.respondRedirect("/debug/session")
        }
    }
}

suspend fun ApplicationCall.getSession(): UserSession? =
    sessions.get()
        ?: run {
            val redirectUrl =
                URLBuilder("http://0.0.0.0:8080/login").run {
                    parameters.append("redirectUrl", request.uri)
                    build()
                }
            respondRedirect(redirectUrl)
            null
        }

suspend fun getProfile(httpClient: HttpClient, token: String): UserInfo =
    httpClient
        .get("https://www.googleapis.com/oauth2/v2/userinfo") {
            headers { append(HttpHeaders.Authorization, "Bearer $token") }
        }
        .body()
