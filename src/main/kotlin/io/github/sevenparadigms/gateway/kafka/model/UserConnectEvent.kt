package io.github.sevenparadigms.gateway.kafka.model

import java.io.Serializable

data class UserConnectEvent(
    val username: String? = null,
    val roles: List<String>? = null
) : Serializable