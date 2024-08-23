package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.websocketmessage.ChatMessage;
import ac.su.suport.livescore.websocketmessage.StreamMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/offer")
    @SendTo("/topic/offer")
    public Message sendOffer(Message message) {
        return message; // 스트리밍 offer 메시지 전송
    }

    @MessageMapping("/answer")
    @SendTo("/topic/answer")
    public Message sendAnswer(Message message) {
        return message; // 스트리밍 answer 메시지 전송
    }

    @MessageMapping("/candidate")
    @SendTo("/topic/candidate")
    public Message sendCandidate(Message message) {
        return message; // 스트리밍 candidate 메시지 전송
    }

    @MessageMapping("/stream/{matchId}")
    @SendTo("/topic/stream/{matchId}")
    public StreamMessage handleStreamMessage(StreamMessage message) {
        // 스트리밍 관련 로직 구현
        return message; // 특정 매치의 스트리밍 메시지 전송
    }

    @MessageMapping("/chat/{matchId}")
    @SendTo("/topic/chat/{matchId}")
    public ChatMessage handleChatMessage(ChatMessage message) {
        // 채팅 메시지 처리 로직 구현
        return message; // 특정 매치의 채팅 메시지 전송
    }
}
