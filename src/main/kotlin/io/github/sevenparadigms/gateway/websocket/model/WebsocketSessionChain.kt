package io.github.sevenparadigms.gateway.websocket.model

import org.sevenparadigms.kotlin.common.objectToJson
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.FluxSink
import java.time.LocalDateTime

class WebsocketSessionChain(
    val session: WebSocketSession,
    private val chain: FluxSink<WebSocketMessage>,
    var stamp: LocalDateTime = LocalDateTime.now()
) {
    fun sendMessage(message: MessageWrapper) = chain.next(session.textMessage(message.objectToJson().toString()))
}