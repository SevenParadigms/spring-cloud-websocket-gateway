package io.github.sevenparadigms.gateway.websocket.support

import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.annotation.Selector
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse
import org.springframework.stereotype.Component

@Component
@Endpoint(id = "websocket")
class WebsocketSession(val websocketFactory: WebsocketFactory) {
    @ReadOperation
    fun features(): WebEndpointResponse<Map<String, Int>> {
        val features: MutableMap<String, Int> = HashMap()
        features["count"] = websocketFactory.size()
        return WebEndpointResponse(features)
    }

    @ReadOperation
    fun feature(@Selector name: String): Int? {
        return features().body[name]
    }
}