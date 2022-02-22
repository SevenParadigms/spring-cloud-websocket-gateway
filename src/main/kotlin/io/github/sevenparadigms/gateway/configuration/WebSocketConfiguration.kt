package io.github.sevenparadigms.gateway.configuration

import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.Beans
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy
import io.github.sevenparadigms.gateway.websocket.support.WebsocketHandler

@Configuration
@ImportAutoConfiguration(Beans::class)
class WebSocketConfiguration {
    @Bean
    fun handlerMapping(): HandlerMapping = WebsocketHandler()

    @Bean
    fun webSocketService(): WebSocketService = HandshakeWebSocketService(ReactorNettyRequestUpgradeStrategy())

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter = WebSocketHandlerAdapter(webSocketService())
}