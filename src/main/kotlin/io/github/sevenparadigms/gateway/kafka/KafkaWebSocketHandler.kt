package io.github.sevenparadigms.gateway.kafka

import io.github.sevenparadigms.gateway.kafka.model.EventWrapper
import io.github.sevenparadigms.gateway.kafka.model.WebSocketEvent
import org.sevenparadigms.kotlin.common.jsonToObject
import org.sevenparadigms.kotlin.common.severe
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

@Component
class KafkaHandler(private val kafkaPublisher: EventDrivenPublisher) {
    fun publish(request: ServerRequest) = request.bodyToMono(EventWrapper::class.java)
        .flatMap { kafkaPublisher.publish(it.topic, it.body.jsonToObject(WebSocketEvent::class.java)) }
        .flatMap { ServerResponse.ok().build() }
        .doOnError { severe { "Exception while trying to process event: " + it.message } }
}
