package io.github.sevenparadigms.gateway.websocket.support

import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import io.github.sevenparadigms.gateway.websocket.model.WebsocketEntryPoint
import java.util.function.Consumer

class WebsocketHandler : SimpleUrlHandlerMapping() {
    override fun initApplicationContext() {
        val beans = obtainApplicationContext().getBeansWithAnnotation(WebsocketEntryPoint::class.java)
        val handlerMap: MutableMap<String, WebsocketFactory> = LinkedHashMap()
        beans.values.forEach(Consumer { bean: Any ->
            val annotation = AnnotationUtils.getAnnotation(bean.javaClass, WebsocketEntryPoint::class.java)!!
            handlerMap[annotation.value] = bean as WebsocketFactory
        })
        super.setOrder(HIGHEST_PRECEDENCE)
        super.setUrlMap(handlerMap)
        super.initApplicationContext()
    }
}