package ru.doronin.spring.trading.core.configuration.websocket

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy

@Configuration
class WebsocketConfiguration {

    @Bean
    fun handlerAdapter(socketService: WebSocketService): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter(socketService)
    }

    @Bean
    fun webSocketHandler(mainHandler: WebSocketHandler): HandlerMapping {
        val urlMap = hashMapOf("/streaming" to mainHandler)
        return SimpleUrlHandlerMapping(urlMap, 0)
    }

    @Bean
    fun webSocketService(): WebSocketService {
        val nettyRequestUpgradeStrategy = ReactorNettyRequestUpgradeStrategy()
        return HandshakeWebSocketService(nettyRequestUpgradeStrategy)
    }
}