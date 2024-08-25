package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.domain.LiveVideoStream;
import ac.su.suport.livescore.logger.AdminLogger;
import ac.su.suport.livescore.logger.UserLogger;  // UserLogger 추가
import ac.su.suport.livescore.service.LiveVideoStreamService;
import ac.su.suport.livescore.service.TusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/live")
public class LiveVideoStreamController {
    private final LiveVideoStreamService liveVideoStreamService;
    private final TusService tusService;

    // 특정 경기의 라이브 스트림 조회
    @GetMapping("/match/{matchId}")
    public ResponseEntity<LiveVideoStream> getLiveStream(@PathVariable Long matchId, HttpServletRequest request) {
        Optional<LiveVideoStream> liveStreamOpt = Optional.ofNullable(liveVideoStreamService.getStreamByMatchId(matchId));

        if (liveStreamOpt.isPresent()) {
            // 사용자 로깅 추가: 라이브 스트림 조회 성공
            UserLogger.logRequest("i", "라이브 스트림 조회", "/api/live/match/" + matchId, "GET", "user", "Match ID: " + matchId, request);
            return ResponseEntity.ok(liveStreamOpt.get());
        } else {
            // 사용자 로깅 추가: 라이브 스트림 조회 실패
            UserLogger.logRequest("e", "라이브 스트림 조회 실패", "/api/live/match/" + matchId, "GET", "user", "Match ID: " + matchId, request);
            return ResponseEntity.notFound().build();
        }
    }

    // 모든 활성 라이브 스트림 조회
    @GetMapping("/active")
    public ResponseEntity<List<LiveVideoStream>> getAllActiveStreams(HttpServletRequest request) {
        List<LiveVideoStream> activeStreams = liveVideoStreamService.getAllActiveStreams();

        // 사용자 로깅 추가: 모든 활성 라이브 스트림 조회
        UserLogger.logRequest("i", "모든 활성 라이브 스트림 조회", "/api/live/active", "GET", "user", "Active Streams Count: " + activeStreams.size(), request);

        return ResponseEntity.ok(activeStreams);
    }

    // 라이브 스트림 시작
    @PostMapping("/start/{matchId}")
    public ResponseEntity<String> startStream(@PathVariable Long matchId, HttpServletRequest request) {
        try {
            liveVideoStreamService.startStream(matchId);
            // 관리자 로깅 추가: 라이브 스트림 시작
            AdminLogger.logRequest("i", "라이브 스트림 시작", "/api/live/start/" + matchId, "POST", "admin", "Match ID: " + matchId, request);
            return ResponseEntity.ok("Stream started for match " + matchId);
        } catch (Exception e) {
            // 관리자 로깅 추가: 라이브 스트림 시작 실패
            AdminLogger.logRequest("e", "라이브 스트림 시작 실패", "/api/live/start/" + matchId, "POST", "admin", "Match ID: " + matchId + ", Error: " + e.getMessage(), request);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start stream: " + e.getMessage());
        }
    }

    // 라이브 스트림 정지
    @PostMapping("/stop/{matchId}")
    public ResponseEntity<String> stopStream(@PathVariable Long matchId, HttpServletRequest request) {
        try {
            liveVideoStreamService.stopStream(matchId);
            // 관리자 로깅 추가: 라이브 스트림 정지
            AdminLogger.logRequest("i", "라이브 스트림 정지", "/api/live/stop/" + matchId, "POST", "admin", "Match ID: " + matchId, request);
            return ResponseEntity.ok("Stream stopped for match " + matchId);
        } catch (Exception e) {
            // 관리자 로깅 추가: 라이브 스트림 정지 실패
            AdminLogger.logRequest("e", "라이브 스트림 정지 실패", "/api/live/stop/" + matchId, "POST", "admin", "Match ID: " + matchId + ", Error: " + e.getMessage(), request);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to stop stream: " + e.getMessage());
        }
    }

    // TUS 업로드 처리
    @RequestMapping(value = "/upload/**", method = {RequestMethod.POST, RequestMethod.PATCH, RequestMethod.HEAD})
    public ResponseEntity<String> handleTusUpload(HttpServletRequest request, HttpServletResponse response) {
        String result = tusService.tusUpload(request, response);

        // 관리자 로깅 추가: TUS 업로드
        AdminLogger.logRequest("i", "TUS 업로드 처리", "/api/live/upload", request.getMethod(), "admin", "Upload result: " + result, request);

        return ResponseEntity.ok(result);
    }
}
