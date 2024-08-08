package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.dto.ChatMessage;
import ac.su.suport.livescore.repository.ChatRoomRepository;
import ac.su.suport.livescore.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class ChatController {

    private final ChatRoomRepository chatRoomRepository;
    private final ChannelTopic channelTopic;
    private final ChatService chatService;

    /**
     * websocket "/pub/chat/message"로 들어오는 메시징을 처리한다.
     */
    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {

        // 로그인 회원 정보로 대화명 설정
        String nickname = message.getSender(); // or other method to get the sender name
        message.setSender(nickname);
        // 채팅방 인원수 세팅
        message.setUserCount(chatRoomRepository.getUserCount(message.getRoomId()));
        // Websocket 에 발행된 메시지를 redis 로 발행 (publish)
        chatService.sendChatMessage(message);
    }
}
