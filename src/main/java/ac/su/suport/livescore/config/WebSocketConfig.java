package ac.su.suport.livescore.config;

import ac.su.suport.livescore.handler.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@RequiredArgsConstructor
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompHandler stompHandler; // STOMP 메시지 핸들러

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/sub", "/topic"); // 클라이언트로 메시지를 전달하는 경로 설정
        config.setApplicationDestinationPrefixes("/pub", "/app"); // 클라이언트가 서버로 메시지를 전송할 경로 설정
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*") // CORS 허용
                .withSockJS(); // SockJS 지원
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler); // STOMP 핸들러 설정
    }
}
