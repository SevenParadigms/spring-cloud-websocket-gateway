package io.github.sevenparadigms.gateway.websocket.support

import com.fasterxml.jackson.databind.JsonNode
import io.github.sevenparadigms.gateway.kafka.EventDrivenPublisher
import io.github.sevenparadigms.gateway.kafka.model.UserConnectEvent
import io.github.sevenparadigms.gateway.kafka.model.UserDisconnectEvent
import io.github.sevenparadigms.gateway.websocket.model.MessageWrapper
import io.github.sevenparadigms.gateway.websocket.model.WebsocketEntryPoint
import io.github.sevenparadigms.gateway.websocket.model.WebsocketSessionChain
import org.sevenparadigms.kotlin.common.copy
import org.sevenparadigms.kotlin.common.debug
import org.sevenparadigms.kotlin.common.info
import org.sevenparadigms.kotlin.common.parseJson
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.r2dbc.config.Beans
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Scheduled
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
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Component
@WebsocketEntryPoint("/wsf")
class WebsocketFactory(val kafkaPublisher: EventDrivenPublisher) : WebSocketHandler {
    private val clients = ConcurrentHashMap<String, WebsocketSessionChain>()

    fun get(username: String): WebsocketSessionChain? = clients[username]

    fun size() = clients.size

    override fun handle(session: WebSocketSession): Mono<Void> {
        return session.handshakeInfo.principal
            .cast(UsernamePasswordAuthenticationToken::class.java)
            .flatMap { authenticationToken: UsernamePasswordAuthenticationToken ->
                kafkaPublisher.publishConnect(
                    UserConnectEvent.newBuilder().setUsername(authenticationToken.name)
                        .setRoles(authenticationToken.authorities.map { it.authority }).build()
                ).subscribe()
                val output = session.send(Flux.create {
                    clients[authenticationToken.name] = WebsocketSessionChain(session, it)
                })
                val input = session.receive()
                    .map { obj: WebSocketMessage -> obj.payloadAsText.parseJson(MessageWrapper::class.java) }
                    .doOnNext { handling(it, authenticationToken.name) }.then()
                Mono.zip(input, output).then().doFinally { signal: SignalType ->
                    kafkaPublisher.publishDisconnect(
                        UserDisconnectEvent.newBuilder().setUsername(authenticationToken.name).build()
                    ).subscribe {
                        val sessionChain = clients[authenticationToken.name]
                        clients.remove(authenticationToken.name)
                        info("WebSocket revoke connection with signal[${signal.name}] and user[${authenticationToken.name}]")
                        sessionChain?.session?.close()
                    }
                }
            }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 10)
    fun disconnectForgottenWebSessions() {
        clients.copy().keys.parallelStream().forEach { key ->
            val value = clients[key]
            if (value != null && Duration.between(value.stamp, LocalDateTime.now()).toMinutes() > 60) {
                kafkaPublisher.publishDisconnect(
                    UserDisconnectEvent.newBuilder().setUsername(key).setIsTimeOut(true).build()
                ).subscribe {
                    clients.remove(key)
                    value.session.close()
                    info("WebSocket disconnect by timeout and user[$key]")
                }
            }
        }
    }

    fun handling(message: MessageWrapper, username: String) {
        clients[username]!!.stamp = LocalDateTime.now()
        val webClient = Beans.of(WebClient.Builder::class.java).baseUrl(message.baseUrl).build()
        when (message.type) {
            HttpMethod.GET -> webClient.get().uri(message.uri).retrieve()
            HttpMethod.POST -> webClient.post().uri(message.uri).body(BodyInserters.fromValue(message.body)).retrieve()
            HttpMethod.PUT -> webClient.put().uri(message.uri).body(BodyInserters.fromValue(message.body)).retrieve()
            HttpMethod.DELETE -> webClient.delete().uri(message.uri).retrieve()
            HttpMethod.PATCH -> webClient.patch().uri(message.uri).body(BodyInserters.fromValue(message.body)).retrieve()
            HttpMethod.HEAD -> webClient.head().uri(message.uri).retrieve()
            HttpMethod.OPTIONS -> webClient.options().uri(message.uri).retrieve()
            HttpMethod.TRACE -> webClient.method(HttpMethod.TRACE).uri(message.uri).retrieve()
        }
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
                debug("Request[${message.baseUrl}${message.uri}] by user[$username] accepted\r\n$it")
                clients[username]!!.sendMessage(message.copy(body = it))
            }
    }
}