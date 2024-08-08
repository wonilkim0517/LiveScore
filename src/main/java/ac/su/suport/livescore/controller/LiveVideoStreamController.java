package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.domain.LiveVideoStream;
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

    @GetMapping("/match/{matchId}")
    public ResponseEntity<LiveVideoStream> getLiveStream(@PathVariable Long matchId) {
        Optional<LiveVideoStream> liveStreamOpt = Optional.ofNullable(liveVideoStreamService.getStreamByMatchId(matchId));
        return liveStreamOpt
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<List<LiveVideoStream>> getAllActiveStreams() {
        List<LiveVideoStream> activeStreams = liveVideoStreamService.getAllActiveStreams();
        return ResponseEntity.ok(activeStreams);
    }

    @PostMapping("/start/{matchId}")
    public ResponseEntity<String> startStream(@PathVariable Long matchId) {
        try {
            liveVideoStreamService.startStream(matchId);
            return ResponseEntity.ok("Stream started for match " + matchId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start stream: " + e.getMessage());
        }
    }

    @PostMapping("/stop/{matchId}")
    public ResponseEntity<String> stopStream(@PathVariable Long matchId) {
        try {
            liveVideoStreamService.stopStream(matchId);
            return ResponseEntity.ok("Stream stopped for match " + matchId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to stop stream: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/upload/**", method = {RequestMethod.POST, RequestMethod.PATCH, RequestMethod.HEAD})
    public ResponseEntity<String> handleTusUpload(HttpServletRequest request, HttpServletResponse response) {
        String result = tusService.tusUpload(request, response);
        return ResponseEntity.ok(result);
    }
}
