package ac.su.suport.livescore.handler;

import ac.su.suport.livescore.dto.ChatMessage;
import ac.su.suport.livescore.repository.ChatRoomRepository;
import ac.su.suport.livescore.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.SUBSCRIBE == accessor.getCommand()) {
            String sessionId = (String) message.getHeaders().get("simpSessionId");
            String destination = accessor.getDestination();
            String roomId = chatService.getRoomId(destination);

            if (roomId == null) {
                log.warn("Room ID가 null입니다. 구독 처리 중단: Session ID: {}", sessionId);
                return message;
            }

            if (!chatRoomRepository.isUserAlreadySubscribed(sessionId, roomId)) {
                log.info("SUBSCRIBE: Session ID: {}, Room ID: {}", sessionId, roomId);
                chatRoomRepository.setUserEnterInfo(sessionId, roomId);
                chatRoomRepository.plusUserCount(roomId);

                // 사용자 정보 세션에서 가져오기
                Principal userPrincipal = accessor.getUser();
                String name = "UnknownUser";
                if (userPrincipal != null) {
                    name = userPrincipal.getName();
                } else {
                    // 핸드셰이크 시점에 세션에 저장된 사용자 정보 사용
                    name = (String) accessor.getSessionAttributes().get("username");
                }

                chatService.sendChatMessage(ChatMessage.builder()
                        .type(ChatMessage.MessageType.JOIN)
                        .roomId(roomId)
                        .sender(name)
                        .build());

                log.info("User {} subscribed to room {}. Current user count: {}", name, roomId, chatRoomRepository.getUserCount(roomId));
            } else {
                log.info("Session ID: {} already subscribed to room ID: {}", sessionId, roomId);
            }
        } else if (StompCommand.DISCONNECT == accessor.getCommand()) {
            String sessionId = (String) message.getHeaders().get("simpSessionId");
            String roomId = chatRoomRepository.getUserEnterRoomId(sessionId);
            if (roomId != null) {
                chatRoomRepository.minusUserCount(roomId);
                chatRoomRepository.removeUserEnterInfo(sessionId);
                log.info("User disconnected from room {}. Current user count: {}", roomId, chatRoomRepository.getUserCount(roomId));
            }
        }

        return message;
    }
}
