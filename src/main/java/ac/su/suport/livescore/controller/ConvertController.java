package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.service.ConvertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/convert")
@CrossOrigin(origins = "*") // CORS 설정 추가
public class ConvertController {

    private final ConvertService convertService;

    @PostMapping("/hls/{date}/{matchId}/{filename}")
    public ResponseEntity<String> convertToHls(
            @PathVariable String date,
            @PathVariable Long matchId,
            @PathVariable String filename
    ) {
        try {
            convertService.convertToHls(date, matchId, filename);
            return ResponseEntity.ok("Conversion to HLS started successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Conversion failed: " + e.getMessage());
        }
    }
}