package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.config.kafka.KafkaFrameProducer;
import ac.su.suport.livescore.domain.LiveVideoStream;
import ac.su.suport.livescore.service.LiveVideoStreamService;
import ac.su.suport.livescore.service.FFmpegService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://kim11.shop"})
@RequestMapping("/api/live")
public class LiveVideoStreamController {
    private final LiveVideoStreamService liveVideoStreamService;
    private final FFmpegService ffmpegService;
    private final KafkaFrameProducer kafkaFrameProducer;

    @GetMapping("/match/{matchId}")
    public Mono<ResponseEntity<LiveVideoStream>> getLiveStream(@PathVariable Long matchId) {
        return liveVideoStreamService.getStreamByMatchId(matchId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public Mono<ResponseEntity<List<LiveVideoStream>>> getAllActiveStreams() {
        return liveVideoStreamService.getAllActiveStreams()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/webcam/start/{matchId}")
    public Mono<ResponseEntity<String>> startWebcamStream(@PathVariable Long matchId) {
        log.info("Received request to start webcam stream for match ID: {}", matchId);
        return liveVideoStreamService.startStream(matchId)
                .doOnSuccess(stream -> ffmpegService.startWebcamStreaming(matchId.toString()))
                .doOnSuccess(stream -> log.info("Webcam stream started successfully for match ID: {}", matchId))
                .thenReturn(ResponseEntity.ok("Webcam stream started for match " + matchId))
                .onErrorResume(e -> {
                    log.error("Error starting webcam stream for match ID: {}", matchId, e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to start webcam stream: " + e.getMessage()));
                });
    }

    @PostMapping("/webcam/stop/{matchId}")
    public Mono<ResponseEntity<String>> stopWebcamStream(@PathVariable Long matchId) {
        log.info("Received request to stop webcam stream for match ID: {}", matchId);
        return liveVideoStreamService.stopStream(matchId)
                .doOnSuccess(unused -> {
                    try {
                        ffmpegService.stopStreaming(matchId.toString());
                        log.info("FFmpeg process stopped successfully for match ID: {}", matchId);
                    } catch (Exception e) {
                        log.error("Error stopping FFmpeg process for match ID: {}", matchId, e);
                        throw new RuntimeException("Failed to stop FFmpeg process", e);
                    }
                })
                .doOnSuccess(unused -> log.info("Webcam stream stopped successfully for match ID: {}", matchId))
                .thenReturn(ResponseEntity.ok("Webcam stream stopped for match " + matchId))
                .onErrorResume(e -> {
                    log.error("Error stopping webcam stream for match ID: {}", matchId, e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to stop webcam stream: " + e.getMessage()));
                });
    }

    @PostMapping("/webcam/frame/{matchId}")
    public ResponseEntity<String> processWebcamFrame(@PathVariable Long matchId, @RequestParam("video") MultipartFile videoChunk) {
        try {
            byte[] frameData = videoChunk.getBytes();
            log.debug("Received frame data for match ID: {}. Size: {} bytes", matchId, frameData.length);
            // kafka 없이 스트리밍
            // liveVideoStreamService.processWebcamData(matchId.toString(), frameData);
            // kafka 를 이용하여 스트리밍
            kafkaFrameProducer.sendFrame(matchId.toString(), frameData);
            return ResponseEntity.ok("Frame data sent to Kafka for match " + matchId);
        } catch (IOException e) {
            log.error("Error processing webcam frame for match ID: {}", matchId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process webcam frame: " + e.getMessage());
        }
    }
}