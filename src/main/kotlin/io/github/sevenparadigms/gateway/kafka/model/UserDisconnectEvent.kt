package io.github.sevenparadigms.gateway.kafka.model

import java.io.Serializable

data class UserDisconnectEvent(
    val username: String? = null,
    val isTimeOut: Boolean? = null
) : Serializable