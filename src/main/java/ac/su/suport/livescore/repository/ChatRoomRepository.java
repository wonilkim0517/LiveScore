package ac.su.suport.livescore.repository;

import ac.su.suport.livescore.domain.ChatRoom;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepository {

    private static final String CHAT_ROOMS = "CHAT_ROOMS"; // 채팅방을 저장할 Redis 키
    private static final String ENTER_INFO = "ENTER_INFO"; // 유저가 입장한 채팅방 ID를 저장할 Redis 키
    private static final String USER_COUNT = "USER_COUNT"; // 채팅방에 접속한 유저 수를 저장할 Redis 키

    private final RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, ChatRoom> hashOpsRoom; // 채팅방 정보 관리
    private HashOperations<String, String, String> hashOpsEnterInfo; // 사용자의 입장 정보 관리
    private HashOperations<String, String, Integer> hashOpsUserCount; // 사용자 수 관리

    @PostConstruct
    private void init() {
        hashOpsRoom = redisTemplate.opsForHash();
        hashOpsEnterInfo = redisTemplate.opsForHash();
        hashOpsUserCount = redisTemplate.opsForHash();
    }

    // 모든 채팅방을 반환
    public List<ChatRoom> findAllRoom() {
        return hashOpsRoom.values(CHAT_ROOMS);
    }

    // ID로 채팅방 조회
    public ChatRoom findRoomById(String roomId) {
        return hashOpsRoom.get(CHAT_ROOMS, roomId);
    }

    // 채팅방 생성
    public ChatRoom createChatRoom(String matchId, String name) {
        // matchId를 기반으로 기존에 생성된 채팅방이 있는지 확인
        ChatRoom existingRoom = hashOpsRoom.get(CHAT_ROOMS, matchId);

        if (existingRoom != null) {
            // 이미 해당 matchId로 채팅방이 존재하는 경우 해당 방을 반환
            return existingRoom;
        }

        // 해당 matchId로 채팅방이 없으면 새로 생성
        String roomId = matchId; // matchId를 roomId로 사용
        ChatRoom chatRoom = new ChatRoom(roomId, name);
        hashOpsRoom.put(CHAT_ROOMS, roomId, chatRoom);
        return chatRoom;
    }

    // 특정 채팅방의 사용자 수 반환
    public long getUserCount(String roomId) {
        return Optional.ofNullable(hashOpsUserCount.get(USER_COUNT, roomId)).orElse(0);
    }

    // 사용자가 이미 구독되어 있는지 확인
    public boolean isUserAlreadySubscribed(String sessionId, String roomId) {
        String existingRoomId = hashOpsEnterInfo.get(ENTER_INFO, sessionId);
        return roomId.equals(existingRoomId);
    }

    // 사용자가 채팅방에 들어왔을 때의 정보 설정
    public void setUserEnterInfo(String sessionId, String roomId) {
        // 중복 방지를 위해 기존 정보 확인 후 설정
        if (!isUserAlreadySubscribed(sessionId, roomId)) {
            hashOpsEnterInfo.put(ENTER_INFO, sessionId, roomId);
        }
    }

    // 특정 채팅방의 사용자 수 증가
    public void plusUserCount(String roomId) {
        hashOpsUserCount.increment(USER_COUNT, roomId, 1);
    }

    // 특정 채팅방의 사용자 수 감소
    public void minusUserCount(String roomId) {
        hashOpsUserCount.increment(USER_COUNT, roomId, -1);
    }

    // 사용자가 채팅방에서 나갔을 때의 정보 삭제
    public void removeUserEnterInfo(String sessionId) {
        hashOpsEnterInfo.delete(ENTER_INFO, sessionId);
    }

    // 사용자가 입장한 방의 ID를 반환
    public String getUserEnterRoomId(String sessionId) {
        return hashOpsEnterInfo.get(ENTER_INFO, sessionId);
    }
}
