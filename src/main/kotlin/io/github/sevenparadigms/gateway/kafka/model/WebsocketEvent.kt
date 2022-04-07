package io.github.sevenparadigms.gateway.kafka.model

import java.io.Serializable

data class WebsocketEvent(
    val username: String? = null,
    val baseUrl: String? = null,
    val uri: String? = null,
    val body: String? = null
) : Serializable