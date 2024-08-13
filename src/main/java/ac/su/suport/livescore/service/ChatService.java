package ac.su.suport.livescore.service;

import ac.su.suport.livescore.dto.ChatMessage;
import ac.su.suport.livescore.repository.ChatRoomRepository;
import ac.su.suport.livescore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ChatService {

    private final ChannelTopic channelTopic;
    private final RedisTemplate redisTemplate;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    public String getRoomId(String destination) {
        int lastIndex = destination.lastIndexOf('/');
        if (lastIndex != -1) {
            return destination.substring(lastIndex + 1);
        } else {
            return "";
        }
    }

    public void sendChatMessage(ChatMessage chatMessage) {
        // 닉네임 설정 부분과 메시지 발행 부분을 점검
        chatMessage.setUserCount(chatRoomRepository.getUserCount(chatMessage.getRoomId()));

        if (ChatMessage.MessageType.JOIN.equals(chatMessage.getType())) {
            chatMessage.setMessage(chatMessage.getNickname() + "님이 방에 입장했습니다.");
            chatMessage.setSender("[알림]");
        } else if (ChatMessage.MessageType.TALK.equals(chatMessage.getType())) {
            chatMessage.setSender(chatMessage.getNickname());
        } else if (ChatMessage.MessageType.QUIT.equals(chatMessage.getType())) {
            chatMessage.setMessage(chatMessage.getNickname() + "님이 방에서 나갔습니다.");
            chatMessage.setSender("[알림]");
        }

        redisTemplate.convertAndSend(channelTopic.getTopic(), chatMessage);
    }


}

