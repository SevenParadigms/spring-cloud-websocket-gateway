import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

@ContextConfiguration(initializers = [KafkaProperties.Initializer::class])
open class KafkaProperties {
    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.kafka.bootstrap-servers=" + kafkaContainer.getBootstrapServers(),
                "spring.kafka.client-id=kafkatest",
                "spring.kafka.consumer.group-id=kafkatest",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "messaging.enabled=true"
            )
                .applyTo(configurableApplicationContext.environment)
        }
    }

    companion object {
        protected var network: Network = Network.newNetwork()
        @JvmStatic
        protected val kafkaContainer: KafkaContainer =
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.1.1"))
                .withEmbeddedZookeeper()
                .withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9093 ,BROKER://0.0.0.0:9092")
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT")
                .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER")
                .withEnv("KAFKA_BROKER_ID", "1")
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
                .withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE.toString() + "")
                .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0")
                .withNetwork(network)

        init {
            kafkaContainer.start()
        }

        fun createProducer(): KafkaProducer<Any, Any> {
            val props = Properties()
            props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaContainer.getBootstrapServers()
            props[ProducerConfig.ACKS_CONFIG] = "all"
            props[ProducerConfig.RETRIES_CONFIG] = 0
            props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
            props[AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://testUrl"
            props[ProducerConfig.CLIENT_ID_CONFIG] = "kafkatest"
            return KafkaProducer(props)
        }

        fun createEventConsumer(): KafkaConsumer<String, Any> {
            val props = Properties()
            props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaContainer.getBootstrapServers()
            props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
            props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
            props[AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://testUrl"
            props[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = "true"
            props[ConsumerConfig.GROUP_ID_CONFIG] = "kafkatest"
            return KafkaConsumer(props)
        }

        @Throws(ExecutionException::class, InterruptedException::class)
        fun sendEvent(producer: KafkaProducer<Any, Any>, record: ProducerRecord<Any, Any>?) {
            val sendFuture: Future<*> = producer.send(record)
            val metadata = sendFuture.get() as RecordMetadata
        }
    }
}