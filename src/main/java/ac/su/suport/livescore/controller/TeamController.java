package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.dto.TeamDTO;
import ac.su.suport.livescore.dto.TeamDTO.Request;
import ac.su.suport.livescore.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    // 모든 팀 조회
    @GetMapping
    public ResponseEntity<List<TeamDTO.Response>> getAllTeams() {
        List<TeamDTO.Response> teams = teamService.getAllTeams();
        return new ResponseEntity<>(teams, HttpStatus.OK);
    }

    // 특정 팀 조회
    @GetMapping("/{id}")
    public ResponseEntity<TeamDTO.Response> getTeamById(@PathVariable Long id) {
        Optional<TeamDTO.Response> teamDTO = teamService.getTeamById(id);
        return teamDTO.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 새로운 팀 생성
    @PostMapping
    public ResponseEntity<Request> createTeam(@RequestBody Request teamDTO) {
        Request createdTeam = teamService.createTeam(teamDTO);
        return new ResponseEntity<>(createdTeam, HttpStatus.CREATED);
    }

    // 팀 수정
    @PutMapping("/{id}")
    public ResponseEntity<Request> updateTeam(@PathVariable Long id, @RequestBody Request teamDTO) {
        Optional<Request> updatedTeam = teamService.updateTeam(id, teamDTO);
        return updatedTeam.map(team -> new ResponseEntity<>(team, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // 팀 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        if (teamService.deleteTeam(id)) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}