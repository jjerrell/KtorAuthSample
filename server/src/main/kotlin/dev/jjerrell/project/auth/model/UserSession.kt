package dev.jjerrell.project.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class UserSession(val state: String, val token: String, val userInfo: UserInfo)

@Serializable
data class UserInfo(
    val id: String,
    val name: String? = null,
    @SerialName("given_name") val givenName: String? = null,
    @SerialName("family_name") val familyName: String? = null,
    val picture: String? = null,
    val locale: String? = null
)