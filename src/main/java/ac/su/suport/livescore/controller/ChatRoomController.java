package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.constant.UserRole;  // UserRole 상수를 사용하기 위해 import
import ac.su.suport.livescore.domain.ChatRoom;    // ChatRoom 객체를 사용하기 위해 import
import ac.su.suport.livescore.repository.ChatRoomRepository;  // ChatRoomRepository 인터페이스를 사용하기 위해 import
import ac.su.suport.livescore.service.BanService; // BanService를 사용하기 위해 import
import lombok.RequiredArgsConstructor;  // final 변수에 대한 의존성 주입을 자동으로 해주는 Lombok 어노테이션
import org.springframework.http.HttpStatus;  // HTTP 상태 코드 정의를 위해 import
import org.springframework.http.ResponseEntity;  // HTTP 응답을 캡슐화하기 위해 import
import org.springframework.security.core.Authentication;  // 인증된 사용자 정보를 얻기 위해 import
import org.springframework.security.core.context.SecurityContextHolder;  // 현재 보안 컨텍스트에서 인증 객체를 얻기 위해 import
import org.springframework.web.bind.annotation.*;  // REST 컨트롤러 및 요청 매핑을 위해 import

import java.util.List;  // List 타입을 사용하기 위해 import

@RequiredArgsConstructor  // final 필드에 대한 생성자를 자동으로 생성해주는 Lombok 어노테이션
@RestController  // RESTful 웹 서비스를 처리하는 Spring MVC 컨트롤러를 선언
@RequestMapping("/api/chat")  // 이 컨트롤러가 처리하는 기본 URL 패턴을 설정
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;  // 채팅방 데이터를 관리하는 저장소 객체
    private final BanService banService;  // 강퇴 관련 로직을 처리하는 서비스 객체

    // 현재 인증된 사용자의 정보를 반환하는 엔드포인트
    @GetMapping("/user")
    public ResponseEntity<String> getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();  // 현재 인증된 사용자 정보 가져오기
        if (auth == null || auth.getName() == null) {  // 사용자가 인증되지 않은 경우
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);  // 인증되지 않은 상태 코드 반환
        }
        return new ResponseEntity<>(auth.getName(), HttpStatus.OK);  // 인증된 사용자의 이름 반환
    }

    // 모든 채팅방 목록을 반환하는 엔드포인트
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getRooms() {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllRoom();  // 모든 채팅방 정보 가져오기
        chatRooms.forEach(room -> room.setUserCount(chatRoomRepository.getUserCount(room.getRoomId())));  // 각 채팅방의 현재 사용자 수 설정
        return new ResponseEntity<>(chatRooms, HttpStatus.OK);  // 채팅방 목록과 함께 OK 상태 코드 반환
    }

    // 새로운 채팅방을 생성하는 엔드포인트
    @PostMapping("/room")
    public ResponseEntity<ChatRoom> createRoom(@RequestParam String matchId, @RequestParam String name) {
        ChatRoom newRoom = chatRoomRepository.createChatRoom(matchId, name);  // 새로운 채팅방 생성
        return new ResponseEntity<>(newRoom, HttpStatus.CREATED);  // 생성된 채팅방과 함께 CREATED 상태 코드 반환
    }

    // 특정 채팅방의 정보를 조회하는 엔드포인트
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ChatRoom> getRoomInfo(@PathVariable String roomId) {
        ChatRoom chatRoom = chatRoomRepository.findRoomById(roomId);  // 특정 채팅방 정보 가져오기
        if (chatRoom == null) {  // 채팅방이 존재하지 않는 경우
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);  // NOT_FOUND 상태 코드 반환
        }
        return new ResponseEntity<>(chatRoom, HttpStatus.OK);  // 채팅방 정보와 함께 OK 상태 코드 반환
    }

    // 특정 채팅방에 대한 상세 정보를 반환하는 엔드포인트
    @GetMapping("/room/enter/{roomId}")
    public ResponseEntity<ChatRoom> getRoomDetail(@PathVariable String roomId) {
        ChatRoom chatRoom = chatRoomRepository.findRoomById(roomId);  // 특정 채팅방 정보 가져오기
        if (chatRoom == null) {  // 채팅방이 존재하지 않는 경우
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);  // NOT_FOUND 상태 코드 반환
        }
        return new ResponseEntity<>(chatRoom, HttpStatus.OK);  // 채팅방 정보와 함께 OK 상태 코드 반환
    }

    // ADMIN이 특정 사용자를 채팅방에서 강퇴하는 엔드포인트
    @PostMapping("/room/{roomId}/ban/{userId}")
    public ResponseEntity<String> banUser(@PathVariable String roomId, @PathVariable String userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();  // 현재 인증된 사용자 정보 가져오기
        if (auth == null || auth.getAuthorities() == null) {  // 사용자가 인증되지 않은 경우
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);  // 인증되지 않은 상태 코드 반환
        }

        // 유저의 Role 확인 (ADMIN만 강퇴 가능)
        UserRole role = UserRole.valueOf(auth.getAuthorities().iterator().next().getAuthority());

        try {
            // 강퇴 로직 호출
            banService.banUser(roomId, userId, role);
            return new ResponseEntity<>("User banned successfully", HttpStatus.OK);  // 성공적으로 강퇴된 경우 OK 상태 코드 반환
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);  // 강퇴 권한이 없는 경우 FORBIDDEN 상태 코드 반환
        }
    }

    // 특정 사용자가 해당 채팅방에서 강퇴되었는지 확인하는 엔드포인트
    @GetMapping("/room/{roomId}/isBanned/{userId}")
    public ResponseEntity<Boolean> isUserBanned(@PathVariable String roomId, @PathVariable String userId) {
        boolean isBanned = banService.isUserBanned(roomId, userId);  // 강퇴 여부 확인
        return new ResponseEntity<>(isBanned, HttpStatus.OK);  // 강퇴 상태와 함께 OK 상태 코드 반환
    }
}
