package io.github.sevenparadigms.gateway.configuration

import io.github.sevenparadigms.abac.configuration.SecurityConfig
import io.github.sevenparadigms.gateway.websocket.support.WebsocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy

@Configuration
@Import(SecurityConfig::class)
class WebSocketConfiguration {
    @Bean
    fun handlerMapping(): HandlerMapping = WebsocketHandler()

    @Bean
    fun webSocketService(): WebSocketService = HandshakeWebSocketService(ReactorNettyRequestUpgradeStrategy())

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter = WebSocketHandlerAdapter(webSocketService())
}