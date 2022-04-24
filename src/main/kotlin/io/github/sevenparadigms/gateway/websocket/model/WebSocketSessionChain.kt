package io.github.sevenparadigms.gateway.websocket.model

import org.sevenparadigms.kotlin.common.objectToJson
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.FluxSink

class WebSocketSessionChain(
    val session: WebSocketSession,
    val tokenHash: Long,
    private val chain: FluxSink<WebSocketMessage>
) {
    fun sendMessage(message: MessageWrapper) = chain.next(session.textMessage(message.objectToJson().toString()))
}