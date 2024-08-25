package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.domain.LiveVideoStream;
import ac.su.suport.livescore.domain.Match;
import ac.su.suport.livescore.domain.Video;
import ac.su.suport.livescore.dto.MatchModificationDTO;
import ac.su.suport.livescore.dto.MatchSummaryDTO;
import ac.su.suport.livescore.logger.AdminLogger;
import ac.su.suport.livescore.logger.UserLogger;  // UserLogger 추가
import ac.su.suport.livescore.service.LiveVideoStreamService;
import ac.su.suport.livescore.service.MatchService;
import ac.su.suport.livescore.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final LiveVideoStreamService liveVideoStreamService;
    private final VideoService videoService;

    // 모든 경기 데이터를 단순 리스트로 반환
    @GetMapping("/all")
    public ResponseEntity<List<MatchSummaryDTO.Response>> getAllMatches(HttpServletRequest request) {
        List<MatchSummaryDTO.Response> matches = matchService.getAllMatches();

        // 사용자 로깅 추가: 모든 경기 데이터 조회
        UserLogger.logRequest("i", "모든 경기 데이터 조회", "/api/matches/all", "GET", "user", "Retrieved all matches", request);

        return new ResponseEntity<>(matches, HttpStatus.OK);
    }

    @GetMapping("/view")
    public String viewMatches(Model model, HttpServletRequest request) {
        List<Match> matches = matchService.getAllMatchesWithVideos();
        model.addAttribute("matches", matches);

        // 사용자 로깅 추가: 경기 데이터 뷰 조회
        UserLogger.logRequest("i", "경기 데이터 뷰 조회", "/api/matches/view", "GET", "user", "Viewed matches page", request);

        return "matches";
    }

    // 필터링
    @GetMapping
    public ResponseEntity<List<MatchSummaryDTO.Response>> getMatches(
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department,
            HttpServletRequest request) {
        List<MatchSummaryDTO.Response> matches = matchService.getFilteredMatches(sport, date, department);

        // 사용자 로깅 추가: 필터링된 경기 데이터 조회
        UserLogger.logRequest("i", "필터링된 경기 데이터 조회", "/api/matches", "GET", "user", "Filter: sport=" + sport + ", date=" + date + ", department=" + department, request);

        return new ResponseEntity<>(matches, HttpStatus.OK);
    }

    // 특정 경기 데이터 반환
    @GetMapping("/{id}")
    public ResponseEntity<MatchSummaryDTO.Response> getMatchById(@PathVariable Long id, HttpServletRequest request) {
        MatchSummaryDTO.Response match = matchService.getMatchById(id);

        // 사용자 로깅 추가: 특정 경기 데이터 조회
        UserLogger.logRequest("i", "특정 경기 데이터 조회", "/api/matches/" + id, "GET", "user", "Match ID: " + id, request);

        return new ResponseEntity<>(match, HttpStatus.OK);
    }

    // 매치 생성
    @PostMapping
    public ResponseEntity<MatchModificationDTO> createMatch(@RequestBody MatchModificationDTO matchModificationDTO, HttpServletRequest request) {
        try {
            MatchModificationDTO createdMatch = matchService.createMatch(matchModificationDTO);
            AdminLogger.logRequest("i", "매치생성", "/api/matches", "POST", "admin", createdMatch.toString(), request);
            return new ResponseEntity<>(createdMatch, HttpStatus.CREATED);
        } catch (Exception e) {
            AdminLogger.logRequest("e", "매치생성실패", "/api/matches", "POST", "admin", e.getMessage(), request);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 매치 수정
    @PutMapping("/{id}")
    public ResponseEntity<MatchModificationDTO> updateMatch(@PathVariable Long id, @RequestBody MatchModificationDTO matchModificationDTO, HttpServletRequest request) {
        try {
            MatchModificationDTO updatedMatch = matchService.updateMatch(id, matchModificationDTO);
            AdminLogger.logRequest("i", "매치수정", "/api/matches/" + id, "UPDATE", "admin", matchModificationDTO.toString(), request);
            return new ResponseEntity<>(updatedMatch, HttpStatus.OK);
        } catch (Exception e) {
            AdminLogger.logRequest("e", "매치수정실패", "/api/matches/" + id, "UPDATE", "admin", e.getMessage(), request);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 매치 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id, HttpServletRequest request) {
        if (matchService.deleteMatch(id)) {
            AdminLogger.logRequest("o","매치삭제성공", "/api/matches/" + id,"DELETE", "admin", id.toString(), request);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            AdminLogger.logRequest("e","매치삭제실패", "/api/matches/" + id,"DELETE", "admin", id.toString(), request);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
