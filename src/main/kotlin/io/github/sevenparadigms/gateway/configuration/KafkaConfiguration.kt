package io.github.sevenparadigms.gateway.configuration

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.github.sevenparadigms.gateway.kafka.ReactorKafkaProperties
import io.github.sevenparadigms.gateway.kafka.defaultOffsetPolicy
import io.github.sevenparadigms.gateway.kafka.model.WebsocketEvent
import io.github.sevenparadigms.gateway.kafka.schemaRegistryUrl
import io.github.sevenparadigms.gateway.websocket.model.MessageWrapper
import io.github.sevenparadigms.gateway.websocket.support.WebsocketSession.Companion.clients
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.sevenparadigms.kotlin.common.objectToJson
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.Disposable
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.time.Duration

@Configuration
class KafkaConfiguration(val kafkaProperties: ReactorKafkaProperties) {

    @Bean
    private fun listenWebsocketEvent(): Disposable {
        val receiverOptions = ReceiverOptions.create<String, WebsocketEvent>(
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.broker,
                ConsumerConfig.GROUP_ID_CONFIG to kafkaProperties.groupId,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to kafkaProperties.deserializer,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to kafkaProperties.deserializer,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to defaultOffsetPolicy,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to true,
                schemaRegistryUrl to kafkaProperties.schemaRegistryUrl,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true
            )
        ).commitInterval(Duration.ZERO)
            .commitBatchSize(0)
            .subscription(setOf(kafkaProperties.incomeWebsocketTopic))
        return KafkaReceiver.create(receiverOptions)
            .receive().map { it.value() }
            .doOnNext {
                clients[it.username]?.sendMessage(
                    MessageWrapper(
                        baseUrl = it.baseUrl,
                        uri = it.uri,
                        body = it.body.objectToJson()
                    )
                )
            }.subscribe()
    }
}
