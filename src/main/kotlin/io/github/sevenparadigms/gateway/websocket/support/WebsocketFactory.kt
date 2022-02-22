package io.github.sevenparadigms.gateway.websocket.support

import com.fasterxml.jackson.databind.JsonNode
import org.sevenparadigms.kotlin.common.debug
import org.sevenparadigms.kotlin.common.info
import org.sevenparadigms.kotlin.common.parseJson
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.r2dbc.config.Beans
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters.fromValue
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import io.github.sevenparadigms.gateway.websocket.model.MessageWrapper
import io.github.sevenparadigms.gateway.websocket.model.WebsocketEntryPoint
import io.github.sevenparadigms.gateway.websocket.model.WebsocketSessionChain
import io.github.sevenparadigms.gateway.websocket.support.WebsocketSession.Companion.clients
import java.time.LocalDateTime

@Component
@WebsocketEntryPoint("/wsf")
class WebsocketFactory : WebSocketHandler {
    override fun handle(session: WebSocketSession): Mono<Void> {
        return session.handshakeInfo.principal
            .cast(UsernamePasswordAuthenticationToken::class.java)
            .flatMap { authenticationToken: UsernamePasswordAuthenticationToken ->
                val output = session.send(Flux.create {
                    clients[authenticationToken.name] = WebsocketSessionChain(session, it)
                })
                val input = session.receive()
                    .map { obj: WebSocketMessage -> obj.payloadAsText.parseJson(MessageWrapper::class.java) }
                    .doOnNext { handling(it, authenticationToken) }.then()
                Mono.zip(input, output).then().doFinally { signal: SignalType ->
                    clients.remove(authenticationToken.name)
                    info("WebSocket revoke connection with signal[${signal.name}] and user[${authenticationToken.name}]")
                }
            }
    }

    fun handling(message: MessageWrapper, authenticationToken: UsernamePasswordAuthenticationToken) {
        clients[authenticationToken.name]!!.stamp = LocalDateTime.now()
        val webClient = Beans.of(WebClient.Builder::class.java).baseUrl(message.baseUrl).build()
        val response = when (message.type) {
            HttpMethod.GET -> webClient.get().uri(message.uri).retrieve()
            HttpMethod.POST -> webClient.post().uri(message.uri).body(fromValue(message.body)).retrieve()
            HttpMethod.PUT -> webClient.put().uri(message.uri).body(fromValue(message.body)).retrieve()
            HttpMethod.DELETE -> webClient.delete().uri(message.uri).retrieve()
            HttpMethod.PATCH -> webClient.patch().uri(message.uri).body(fromValue(message.body)).retrieve()
            HttpMethod.HEAD -> webClient.head().uri(message.uri).retrieve()
            HttpMethod.OPTIONS -> webClient.options().uri(message.uri).retrieve()
            HttpMethod.TRACE -> webClient.method(HttpMethod.TRACE).uri(message.uri).retrieve()
        }
        response
            .onStatus({ status -> status.isError })
            { clientResponse ->
                clientResponse.bodyToMono(ByteArrayResource::class.java)
                    .map { responseAnswer: ByteArrayResource ->
                        WebClientResponseException(
                            clientResponse.rawStatusCode(),
                            clientResponse.statusCode().name,
                            clientResponse.headers().asHttpHeaders(),
                            responseAnswer.byteArray,
                            Charsets.UTF_8
                        )
                    }
            }
            .bodyToMono(JsonNode::class.java).subscribe {
                info("Request[${message.baseUrl}${message.uri}] by user[${authenticationToken.name}] accepted")
                debug(it.toString())
                clients[authenticationToken.name]!!.sendMessage(message.copy(body = it))
            }
    }
}