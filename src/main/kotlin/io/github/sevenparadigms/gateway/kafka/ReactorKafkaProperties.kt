package io.github.sevenparadigms.gateway.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "kafka")
data class ReactorKafkaProperties(
    val webSocketTopic: String,
    val userConnectTopic: String,
    val userDisconnectTopic: String,
    val broker: String,
    val groupId: String,
    val serializer: String,
    val deserializer: String,
    val schemaRegistryUrl: String
)
