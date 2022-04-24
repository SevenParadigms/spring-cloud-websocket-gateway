package io.github.sevenparadigms.gateway.websocket.support

import com.fasterxml.jackson.databind.JsonNode
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import io.github.sevenparadigms.gateway.kafka.EventDrivenPublisher
import io.github.sevenparadigms.gateway.kafka.model.UserConnectEvent
import io.github.sevenparadigms.gateway.kafka.model.UserDisconnectEvent
import io.github.sevenparadigms.gateway.websocket.model.MessageWrapper
import io.github.sevenparadigms.gateway.websocket.model.WebSocketEntryPoint
import io.github.sevenparadigms.gateway.websocket.model.WebSocketSessionChain
import org.apache.commons.lang3.ObjectUtils
import org.sevenparadigms.kotlin.common.debug
import org.sevenparadigms.kotlin.common.info
import org.sevenparadigms.kotlin.common.parseJson
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.r2dbc.support.Beans
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import java.util.concurrent.TimeUnit

@Component
@WebSocketEntryPoint("/wsf")
class WebSocketFactory(val kafkaPublisher: EventDrivenPublisher) : WebSocketHandler {
    private val clients = Caffeine.newBuilder()
        .maximumSize(Beans.getProperty(Constants.GATEWAY_CACHE_SIZE, Long::class.java, 10000))
        .expireAfterAccess(
            Beans.getProperty(Constants.GATEWAY_CACHE_ACCESS, Long::class.java, 1800000),
            TimeUnit.MILLISECONDS
        )
        .removalListener { key: String?, value: WebSocketSessionChain?, cause: RemovalCause ->
            if (cause.wasEvicted() && ObjectUtils.isNotEmpty(key)) {
                kafkaPublisher.publishDisconnect(
                    UserDisconnectEvent(key, value!!.tokenHash, true)
                ).subscribe {
                    value.session.close()
                    info { "WebSocket disconnected by timeout with user[$key]" }
                }
            }
        }.build<String, WebSocketSessionChain>()

    override fun handle(session: WebSocketSession) = session.handshakeInfo.principal
        .cast(UsernamePasswordAuthenticationToken::class.java)
        .flatMap { authToken: UsernamePasswordAuthenticationToken ->
            val output = session.send(Flux.create {
                authToken.credentials
                clients.put(authToken.name, WebSocketSessionChain(
                    session = session, tokenHash = authToken.credentials as Long, chain = it))
            })
            val input = session.receive()
                .map { obj: WebSocketMessage -> obj.payloadAsText.parseJson(MessageWrapper::class.java) }
                .doOnNext { handling(it, authToken.name) }.then()
            Mono.zip(input, output).then().doFinally { signal: SignalType ->
                val sessionChain = clients.getIfPresent(authToken.name)!!
                kafkaPublisher.publishDisconnect(
                    UserDisconnectEvent(authToken.name, sessionChain.tokenHash, false)
                ).subscribe {
                    clients.invalidate(authToken.name)
                    sessionChain.session.close()
                    info { "Connection close with signal[${signal.name}] and user[${authToken.name}]" }
                }
            }
            kafkaPublisher.publishConnect(
                UserConnectEvent(authToken.name, authToken.authorities.map { it.authority })
            )
        }

    fun handling(message: MessageWrapper, username: String) {
        val webClient = Beans.of(WebClient.Builder::class.java).baseUrl(message.baseUrl).build()
        val response = when (message.type) {
            HttpMethod.GET -> webClient.get().uri(message.uri).retrieve()
            HttpMethod.POST -> webClient.post().uri(message.uri).body(BodyInserters.fromValue(message.body!!)).retrieve()
            HttpMethod.PUT -> webClient.put().uri(message.uri).body(BodyInserters.fromValue(message.body!!)).retrieve()
            HttpMethod.DELETE -> webClient.delete().uri(message.uri).retrieve()
            HttpMethod.PATCH -> webClient.patch().uri(message.uri).body(BodyInserters.fromValue(message.body!!))
                .retrieve()
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
                info { "Request[${message.baseUrl}${message.uri}] by user[$username] accepted" }
                debug { it.toString() }
                val sessionChain = clients.getIfPresent(username)
                sessionChain?.sendMessage(message.copy(body = it))
            }
    }

    fun get(username: String): WebSocketSessionChain? = clients.getIfPresent(username)

    fun size() = clients.estimatedSize()
}