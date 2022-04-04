package io.github.sevenparadigms.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication(exclude = [R2dbcAutoConfiguration::class])
class GatewayApplication

fun main(args: Array<String>) {
	runApplication<GatewayApplication>(*args)
}