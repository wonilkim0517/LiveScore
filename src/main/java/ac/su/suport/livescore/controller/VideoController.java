package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.domain.Video;
import ac.su.suport.livescore.service.S3Service;
import ac.su.suport.livescore.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/video")
@CrossOrigin(origins = "*") // 모든 도메인에서의 요청을 허용. 실제 운영 환경에서는 특정 도메인만 허용하도록 설정해야 합니다.
public class VideoController {

    private final VideoService videoService;
    private final S3Service s3Service;

    @GetMapping
    public ResponseEntity<List<Video>> getVideos() {
        List<Video> videos = videoService.getAllVideos();
        videos.sort(Comparator.comparing(Video::getDate).reversed());
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<Video>> getVideosByMatchId(@PathVariable Long matchId) {
        List<Video> videos = videoService.getVideosByMatchId(matchId);
        videos.sort(Comparator.comparing(Video::getDate).reversed());
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/stream/{matchId}/{filename}")
    public ResponseEntity<InputStreamResource> streamFromS3(@PathVariable Long matchId, @PathVariable String filename) {
        InputStream inputStream = s3Service.getFile(matchId, filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("video/webm"))
                .body(new InputStreamResource(inputStream));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteVideo(@PathVariable Long id) {
        boolean success = videoService.deleteVideo(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", success);
        return ResponseEntity.ok(response);
    }
}