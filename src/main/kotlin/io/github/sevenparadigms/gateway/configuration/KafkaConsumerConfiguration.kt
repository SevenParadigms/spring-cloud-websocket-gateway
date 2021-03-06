package io.github.sevenparadigms.gateway.configuration

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import io.github.sevenparadigms.gateway.kafka.ReactorKafkaProperties
import io.github.sevenparadigms.gateway.kafka.model.WebSocketEvent
import io.github.sevenparadigms.gateway.websocket.model.MessageWrapper
import io.github.sevenparadigms.gateway.websocket.support.WebSocketFactory
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.sevenparadigms.kotlin.common.copyTo
import org.sevenparadigms.kotlin.common.info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.time.Duration

@Configuration
class KafkaConsumerConfiguration(kafkaProperties: ReactorKafkaProperties) {
    private val receiverOptions = ReceiverOptions.create<String, WebSocketEvent>(
        mapOf(
            BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.broker,
            GROUP_ID_CONFIG to kafkaProperties.groupId,
            KEY_DESERIALIZER_CLASS_CONFIG to kafkaProperties.deserializer,
            VALUE_DESERIALIZER_CLASS_CONFIG to kafkaProperties.deserializer,
            AUTO_OFFSET_RESET_CONFIG to "earliest",
            ENABLE_AUTO_COMMIT_CONFIG to true,
            SCHEMA_REGISTRY_URL_CONFIG to kafkaProperties.schemaRegistryUrl,
            VALUE_SUBJECT_NAME_STRATEGY to RecordNameStrategy::class.java,
            SPECIFIC_AVRO_READER_CONFIG to true
        )
    ).commitInterval(Duration.ZERO)
        .commitBatchSize(0)
        .subscription(setOf(kafkaProperties.webSocketTopic))

    @Bean
    fun listenWebSocketEvent(webSocketFactory: WebSocketFactory) = KafkaReceiver.create(receiverOptions)
        .receive()
        .concatMap { record ->
            Mono.fromRunnable<Void> {
                val it = record.value()
                info { "Transfer kafka message to WebSocket: $it" }
                webSocketFactory.get(it.username!!)?.sendMessage(it.copyTo(MessageWrapper()))
            }
        }.subscribe()
}
