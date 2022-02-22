package io.github.sevenparadigms.gateway.websocket.support

import org.sevenparadigms.kotlin.common.clone
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.annotation.Selector
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import io.github.sevenparadigms.gateway.websocket.model.WebsocketSessionChain
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Component
@Endpoint(id = "websocket")
class WebsocketSession {
    @ReadOperation
    fun features(): WebEndpointResponse<Map<String, Int>> {
        val features: MutableMap<String, Int> = HashMap()
        features["count"] = clients.size
        return WebEndpointResponse(features)
    }

    @ReadOperation
    fun feature(@Selector name: String): Int? {
        return features().body[name]
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    fun disconnectForgottenWebSessions() {
        clients.clone().keys.parallelStream().forEach { key ->
            val value = clients[key]
            if (value != null && Duration.between(value.stamp, LocalDateTime.now()).toMinutes() > 60) {
                value.session.close()
                clients.remove(key)
            }
        }
    }

    companion object {
        val clients = ConcurrentHashMap<String, WebsocketSessionChain>()
    }
}