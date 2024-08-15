package ac.su.suport.livescore.handler;

import ac.su.suport.livescore.domain.User;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

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

                String name = Optional.ofNullable(accessor.getUser())
                        .map(principal -> {
                            if (principal instanceof Authentication) {
                                Authentication auth = (Authentication) principal;
                                Object principalObj = auth.getPrincipal();

                                if (principalObj instanceof User) {
                                    User user = (User) principalObj;
                                    return user.getNickname();
                                } else if (principalObj instanceof UserDetails) {
                                    UserDetails userDetails = (UserDetails) principalObj;
                                    return userDetails.getUsername(); // UserDetails의 username을 nickname으로 사용
                                } else {
                                    log.warn("Principal is not an instance of User or UserDetails. Principal: {}", principalObj);
                                    return "UnknownUser";
                                }
                            } else {
                                log.warn("Principal is not an instance of Authentication. Principal: {}", principal);
                                return "UnknownUser";
                            }
                        })
                        .orElse("UnknownUser");

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
            // DISCONNECT 로직 유지
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
