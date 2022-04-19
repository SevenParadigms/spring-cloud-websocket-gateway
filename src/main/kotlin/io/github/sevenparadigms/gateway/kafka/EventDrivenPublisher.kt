package io.github.sevenparadigms.gateway.kafka

import io.github.sevenparadigms.gateway.kafka.model.UserConnectEvent
import io.github.sevenparadigms.gateway.kafka.model.UserDisconnectEvent
import io.github.sevenparadigms.gateway.support.schemaRegistryUrl
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.sevenparadigms.kotlin.common.debug
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import java.util.*

@Component
class EventDrivenPublisher(private val kafkaProperties: ReactorKafkaProperties) {
    private val producerProps: Map<String, String> = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.broker,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to kafkaProperties.serializer,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to kafkaProperties.serializer,
        schemaRegistryUrl to kafkaProperties.schemaRegistryUrl
    )

    fun <T> publish(topic: String, event: T, key: String = UUID.randomUUID().toString()) =
        KafkaSender.create<String, T>(SenderOptions.create(producerProps)).createOutbound()
            .send(Mono.just(ProducerRecord(topic, key, event)))
            .then()
            .doOnSuccess { debug { "Successfully sent $topic[$event] with id[$key]" } }

    fun publishConnect(event: UserConnectEvent) = publish(kafkaProperties.userConnectTopic, event)
    fun publishDisconnect(event: UserDisconnectEvent) = publish(kafkaProperties.userDisconnectTopic, event)
}
