package io.github.sevenparadigms.gateway.websocket.model

import org.apache.commons.lang3.StringUtils

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebSocketEntryPoint(val value: String = StringUtils.EMPTY)