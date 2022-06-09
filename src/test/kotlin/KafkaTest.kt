import io.github.sevenparadigms.gateway.kafka.model.UserConnectEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.util.concurrent.ExecutionException

@ContextConfiguration
internal class KafkaTest : KafkaProperties() {
    var consumer: KafkaConsumer<*, *>? = null
    var producer: KafkaProducer<Any, Any>? = null
    var singleRecord: ConsumerRecord<String, UserConnectEvent>? = null

    @BeforeEach
    fun prepare() {
        consumer = createEventConsumer()
        consumer!!.subscribe(setOf(TOPIC))
        val duration = Duration.ofSeconds(5)
        consumer!!.poll(duration)
        producer = createProducer()
    }

    @AfterEach
    fun cleanup() {
        producer!!.close()
        consumer!!.close()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun shouldReturnRecordWhenProducerSendEvent() {
        assumeTrue(kafkaContainer.isRunning())
        val userName = "testName"
        val roles: MutableList<String> = ArrayList()
        roles.add("user")
        val event = UserConnectEvent.newBuilder().setRoles(roles).setUsername(userName).build()
        val headers = listOf<Header>(RecordHeader(EVENT_TYPE_KEY, EVENT_UPDATE.toByteArray()))
        val record: ProducerRecord<Any, Any> = ProducerRecord(TOPIC, null, event.username, event, headers)
        sendEvent(producer!!, record)
        singleRecord = KafkaTestUtils.getSingleRecord(consumer, TOPIC) as ConsumerRecord<String, UserConnectEvent>?
        MatcherAssert.assertThat(singleRecord!!.topic().contains(TOPIC), `is`(true))
        MatcherAssert.assertThat(singleRecord!!.key(), `is`(event.username))
        MatcherAssert.assertThat(singleRecord!!.value(), `is`(event))
    }

    companion object {
        private const val EVENT_TYPE_KEY = "eventType"
        private const val EVENT_UPDATE = "UPDATE"
        private const val TOPIC = "test_topic"
    }
}