package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.dto.ChatMessage;
import ac.su.suport.livescore.repository.ChatRoomRepository;
import ac.su.suport.livescore.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class ChatController {

    private final ChatRoomRepository chatRoomRepository; // 채팅방 저장소
    private final ChatService chatService; // 채팅 서비스

    @MessageMapping("/chat/message/{matchId}")
    public void message(@DestinationVariable String matchId, ChatMessage message) {
        // 사용자의 닉네임 설정
        String nickname = message.getNickname();
        message.setNickname(nickname);

        // matchId를 사용하여 채팅방을 구분
        message.setRoomId(matchId);

        // 채팅방 인원수 세팅
        message.setUserCount(chatRoomRepository.getUserCount(matchId));

        // Websocket 에 발행된 메시지를 redis 로 발행 (publish)
        chatService.sendChatMessage(message);
    }
}

