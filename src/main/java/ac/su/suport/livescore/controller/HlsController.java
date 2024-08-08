package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.service.HlsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hls")
public class HlsController {

    private final HlsService hlsService;

    @GetMapping("/{matchId}/playlist.m3u8")
    public ResponseEntity<InputStreamResource> getMaster(
            @PathVariable Long matchId
    ) throws FileNotFoundException {
        File file = hlsService.getHlsFile(matchId.toString(), "index.m3u8");
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-mpegURL"))
                .body(resource);
    }

    @GetMapping("/{matchId}/{filename}")
    public ResponseEntity<InputStreamResource> getSegment(
            @PathVariable Long matchId,
            @PathVariable String filename
    ) throws FileNotFoundException {
        File file = hlsService.getHlsFile(matchId.toString(), filename);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/MP2T"))
                .body(resource);
    }
}