package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.domain.ChatRoom;
import ac.su.suport.livescore.logger.AdminLogger;
import ac.su.suport.livescore.logger.UserLogger;  // UserLogger 추가
import ac.su.suport.livescore.repository.ChatRoomRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chat")
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;

    @GetMapping("/user")
    public String getUserInfo(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 사용자 로깅 추가: 사용자 정보 조회
        UserLogger.logRequest("i", "사용자 정보 조회", "/api/chat/user", "GET", auth.getName(), "User info retrieved", request);

        return auth.getName();
    }

    // 모든 채팅방 목록 반환
    @GetMapping("/rooms")
    public List<ChatRoom> room(HttpServletRequest request) {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllRoom();
        chatRooms.forEach(room -> room.setUserCount(chatRoomRepository.getUserCount(room.getRoomId())));

        // 사용자 로깅 추가: 모든 채팅방 목록 조회
        UserLogger.logRequest("i", "모든 채팅방 목록 조회", "/api/chat/rooms", "GET", "user", "All chat rooms retrieved", request);

        return chatRooms;
    }

    // 채팅방 생성
    @PostMapping("/room")
    public ChatRoom createRoom(@RequestParam String name, HttpServletRequest request) {
        ChatRoom newRoom = chatRoomRepository.createChatRoom(name);

        // 관리자 행동 로깅
        AdminLogger.logRequest("i", "채팅방 생성", "/api/chat/room", "POST", "admin", newRoom.toString(), request);

        return newRoom;
    }

    // 특정 채팅방 조회
    @GetMapping("/room/{roomId}")
    public ChatRoom roomInfo(@PathVariable String roomId, HttpServletRequest request) {
        ChatRoom room = chatRoomRepository.findRoomById(roomId);

        // 사용자 로깅 추가: 특정 채팅방 조회
        UserLogger.logRequest("i", "특정 채팅방 조회", "/api/chat/room/" + roomId, "GET", "user", "Chat room info retrieved for roomId: " + roomId, request);

        return room;
    }

    // 채팅방 입장 화면을 JSON 데이터로 반환
    @GetMapping("/room/enter/{roomId}")
    public ChatRoom roomDetail(@PathVariable String roomId, HttpServletRequest request) {
        ChatRoom room = chatRoomRepository.findRoomById(roomId);

        // 사용자 로깅 추가: 채팅방 입장
        UserLogger.logRequest("i", "채팅방 입장", "/api/chat/room/enter/" + roomId, "GET", "user", "Entered chat room for roomId: " + roomId, request);

        return room;
    }
}
