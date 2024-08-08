package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.dto.LineupDTO;
import ac.su.suport.livescore.dto.LineupGroupDTO;
import ac.su.suport.livescore.service.LineupService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lineup")
public class LineupController {

    private final LineupService lineupService;

    public LineupController(LineupService lineupService) {
        this.lineupService = lineupService;
    }

    // 팀 라인업 조회
    @GetMapping("/{teamId}")
    public ResponseEntity<?> getLineup(
            @PathVariable Long teamId,
            @RequestParam(name = "format", required = false, defaultValue = "flat") String format
    ) {
        if ("grouped".equals(format)) {
            LineupGroupDTO.Response lineupGroup = lineupService.getLineupGroupedFormat(teamId);
            return new ResponseEntity<>(lineupGroup, HttpStatus.OK);
        } else { // default to flat format
            List<LineupDTO.Response> lineup = lineupService.getLineupFlatFormat(teamId);
            return new ResponseEntity<>(lineup, HttpStatus.OK);
        }
    }

    // 팀 라인업 생성
    @PostMapping("/{teamId}")
    public ResponseEntity<?> createLineup(@PathVariable Long teamId,
                                          @RequestBody List<LineupDTO.Request> lineupDTOs) {
        try {
            List<LineupDTO.Response> createdLineup = lineupService.createLineup(teamId, lineupDTOs);
            return new ResponseEntity<>(createdLineup, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // 팀 라인업 수정
    @PutMapping("/{teamId}")
    public ResponseEntity<List<LineupDTO.Response>> updateLineup(@PathVariable Long teamId,
                                                                 @RequestBody List<LineupDTO.Request> lineupDTOs) {
        List<LineupDTO.Response> updatedLineup = lineupService.updateLineup(teamId, lineupDTOs);
        return new ResponseEntity<>(updatedLineup, HttpStatus.OK);
    }

    // 팀 라인업 삭제
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteLineup(@PathVariable Long teamId) {
        lineupService.deleteLineup(teamId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}