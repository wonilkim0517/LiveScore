package ac.su.suport.livescore.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessage {

    @Builder
    public ChatMessage(MessageType type, String roomId, String sender, String message, long userCount) {
        this.type = type;
        this.roomId = roomId;
        this.sender = sender;
        this.message = message;
        this.userCount = userCount;
    }

    public enum MessageType {
        // 메세지 타입 : 입장, 채팅, 퇴장
        JOIN, TALK, QUIT
    }

    private MessageType type; // 메세지 타입
    private String roomId; // 방번호
    private String sender; // 메세지 보낸 사람
    private String message; // 메세지
    private long userCount; // 현재 접속자 수
}
