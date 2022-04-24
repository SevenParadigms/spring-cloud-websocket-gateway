package io.github.sevenparadigms.gateway.kafka

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.*
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import io.github.sevenparadigms.abac.security.auth.data.RevokeTokenEvent
import io.github.sevenparadigms.gateway.kafka.model.UserConnectEvent
import io.github.sevenparadigms.gateway.kafka.model.UserDisconnectEvent
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.sevenparadigms.kotlin.common.info
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import java.util.*

@Component
class EventDrivenPublisher(private val kafkaProperties: ReactorKafkaProperties,
                           private val eventPublisher: ApplicationEventPublisher
) {
    private val producerProps: Map<String, Any> = mapOf(
        BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.broker,
        KEY_SERIALIZER_CLASS_CONFIG to kafkaProperties.serializer,
        VALUE_SERIALIZER_CLASS_CONFIG to kafkaProperties.serializer,
        SCHEMA_REGISTRY_URL_CONFIG to kafkaProperties.schemaRegistryUrl,
        VALUE_SUBJECT_NAME_STRATEGY to RecordNameStrategy::class.java,
        AUTO_REGISTER_SCHEMAS to true
    )

    fun <T> publish(topic: String, event: T, key: String = UUID.randomUUID().toString()) =
        KafkaSender.create<String, T>(SenderOptions.create(producerProps)).createOutbound()
            .send(Mono.just(ProducerRecord(topic, key, event)))
            .then()
            .doOnSuccess { info { "Successfully sent to topic[$topic]: $event with id=$key" }  }


    fun publishConnect(event: UserConnectEvent) = publish(kafkaProperties.userConnectTopic, event)
    fun publishDisconnect(event: UserDisconnectEvent): Mono<Void> {
        eventPublisher.publishEvent(RevokeTokenEvent(hash = event.hash, source = event.username))
        return publish(kafkaProperties.userDisconnectTopic, event)
    }
}
