package io.github.sevenparadigms.gateway.websocket.support

import io.github.sevenparadigms.gateway.websocket.model.WebSocketEntryPoint
import org.sevenparadigms.kotlin.common.debug
import org.sevenparadigms.kotlin.common.objectToJson
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import java.util.function.Consumer

class WebSocketHandler : SimpleUrlHandlerMapping() {
    override fun initApplicationContext() {
        val beans = obtainApplicationContext().getBeansWithAnnotation(WebSocketEntryPoint::class.java)
        val handlerMap: MutableMap<String, WebSocketFactory> = LinkedHashMap()
        beans.values.forEach(Consumer { bean: Any ->
            val annotation = AnnotationUtils.getAnnotation(bean.javaClass, WebSocketEntryPoint::class.java)!!
            handlerMap[annotation.value] = bean as WebSocketFactory
        })
        super.setOrder(HIGHEST_PRECEDENCE)
        super.setUrlMap(handlerMap)
        super.initApplicationContext()
        debug { "WebSocket`s endpoint defined: ${handlerMap.objectToJson()}" }
    }
}