package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.domain.ChatRoom;
import ac.su.suport.livescore.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController // @Controller 대신 @RestController로 변경
@RequestMapping("/api/chat") // API 경로를 /api/chat으로 변경
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;

    @GetMapping("/user")
    public String getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName(); // JWT 제거로 인해 토큰 대신 사용자 이름만 반환
    }

    // 모든 채팅방 목록 반환
    @GetMapping("/rooms")
    public List<ChatRoom> room() {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllRoom();
        chatRooms.stream().forEach(room -> room.setUserCount(chatRoomRepository.getUserCount(room.getRoomId())));
        return chatRooms;
    }

    // 채팅방 생성
    @PostMapping("/room")
    public ChatRoom createRoom(@RequestParam String name) {
        return chatRoomRepository.createChatRoom(name);
    }

    // 특정 채팅방 조회
    @GetMapping("/room/{roomId}")
    public ChatRoom roomInfo(@PathVariable String roomId) {
        return chatRoomRepository.findRoomById(roomId);
    }

    // 채팅방 입장 화면을 JSON 데이터로 반환
    @GetMapping("/room/enter/{roomId}")
    public ChatRoom roomDetail(@PathVariable String roomId) {
        return chatRoomRepository.findRoomById(roomId);
    }
}
