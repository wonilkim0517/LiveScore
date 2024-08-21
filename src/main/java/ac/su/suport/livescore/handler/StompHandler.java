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

@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT == accessor.getCommand()) {
            log.info("CONNECT");
        } else if (StompCommand.SUBSCRIBE == accessor.getCommand()) {
            String sessionId = (String) message.getHeaders().get("simpSessionId");
            String destination = accessor.getDestination();
            String roomId = chatService.getRoomId(destination);

            log.info("SUBSCRIBE: SessionId: {}, RoomId: {}", sessionId, roomId);

            if (!chatRoomRepository.isUserAlreadySubscribed(sessionId, roomId)) {
                chatRoomRepository.setUserEnterInfo(sessionId, roomId);
                long userCount = chatRoomRepository.plusUserCount(roomId);

                String nickname = (String) accessor.getSessionAttributes().get("nickname");
                if (nickname == null || nickname.trim().isEmpty()) {
                    nickname = "Anonymous";
                }

                chatService.sendChatMessage(ChatMessage.builder()
                        .type(ChatMessage.MessageType.JOIN)
                        .roomId(roomId)
                        .sender(nickname)
                        .nickname(nickname)
                        .userCount(userCount)
                        .build());

                log.info("User {} subscribed to room {}. Current user count: {}", nickname, roomId, userCount);
            }
        } else if (StompCommand.DISCONNECT == accessor.getCommand()) {
            String sessionId = (String) message.getHeaders().get("simpSessionId");
            String roomId = chatRoomRepository.getUserEnterRoomId(sessionId);

            if (roomId != null) {
                long userCount = chatRoomRepository.minusUserCount(roomId);
                String nickname = (String) accessor.getSessionAttributes().get("nickname");
                if (nickname == null || nickname.trim().isEmpty()) {
                    nickname = "Anonymous";
                }

                chatService.sendChatMessage(ChatMessage.builder()
                        .type(ChatMessage.MessageType.QUIT)
                        .roomId(roomId)
                        .sender(nickname)
                        .nickname(nickname)
                        .userCount(userCount)
                        .build());

                chatRoomRepository.removeUserEnterInfo(sessionId);
                log.info("User {} disconnected from room {}. Current user count: {}", nickname, roomId, userCount);
            }
        }

        return message;
    }
}