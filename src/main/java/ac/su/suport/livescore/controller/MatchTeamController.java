package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.dto.MatchTeamDTO;
import ac.su.suport.livescore.service.MatchTeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matchTeams")
public class MatchTeamController {

    private final MatchTeamService matchTeamService;

    @Autowired
    public MatchTeamController(MatchTeamService matchTeamService) {
        this.matchTeamService = matchTeamService;
    }

    @GetMapping("/{matchId}")
    public MatchTeamDTO getMatchRecords(@PathVariable Long matchId) {
        return matchTeamService.getMatchRecords(matchId);
    }
}
