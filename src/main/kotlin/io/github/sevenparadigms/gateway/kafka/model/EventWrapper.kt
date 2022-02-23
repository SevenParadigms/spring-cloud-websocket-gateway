package io.github.sevenparadigms.gateway.kafka.model

import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.lang3.StringUtils
import org.springframework.data.r2dbc.support.JsonUtils

data class EventWrapper(
    val topic: String = StringUtils.EMPTY,
    val body: JsonNode = JsonUtils.objectNode()
)
