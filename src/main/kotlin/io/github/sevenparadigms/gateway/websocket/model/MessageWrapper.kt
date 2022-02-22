package io.github.sevenparadigms.gateway.websocket.model

import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.lang3.StringUtils
import org.springframework.data.r2dbc.support.JsonUtils
import org.springframework.http.HttpMethod

data class MessageWrapper(
    val username: String? = null,
    val type: HttpMethod = HttpMethod.GET,
    val baseUrl: String = StringUtils.EMPTY,
    val uri: String = StringUtils.EMPTY,
    val body: JsonNode = JsonUtils.objectNode()
)
