package io.github.sevenparadigms.gateway.configuration

import io.github.sevenparadigms.gateway.kafka.KafkaHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class RoutesConfiguration(private val kafkaHandler: KafkaHandler) {
    @Bean
    fun route(): RouterFunction<ServerResponse> = router {
        ("/kafka-websocket").nest {
            accept(MediaType.APPLICATION_JSON).nest {
                POST("", kafkaHandler::publish)
            }
        }
    }
}
