package io.github.sevenparadigms.gateway.websocket.support

import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.annotation.Selector
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse
import org.springframework.stereotype.Component

@Component
@Endpoint(id = "websocket")
class WebSocketSession(val websocketFactory: WebSocketFactory) {
    @ReadOperation
    fun features(): WebEndpointResponse<Map<String, Long>> {
        val features: MutableMap<String, Long> = HashMap()
        features["count"] = websocketFactory.size()
        return WebEndpointResponse(features)
    }

    @ReadOperation
    fun feature(@Selector name: String): Long? {
        return features().body[name]
    }
}