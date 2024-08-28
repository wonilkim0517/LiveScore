package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.dto.BracketDTO;
import ac.su.suport.livescore.dto.GroupDTO;
import ac.su.suport.livescore.dto.TournamentMatchDTO;
import ac.su.suport.livescore.service.BracketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("api/brackets")
public class BracketController {

    private final BracketService bracketService;

    @GetMapping("/league/{sport}")
    public ResponseEntity<Map<String, List<GroupDTO>>> getSportLeagueBrackets(@PathVariable("sport") String sport) {
        Map<String, List<GroupDTO>> leagueData = bracketService.getSportLeagueBrackets(sport);
        return new ResponseEntity<>(leagueData, HttpStatus.OK);
    }

    @GetMapping("/tournament/{sport}")
    public ResponseEntity<List<TournamentMatchDTO>> getSportTournamentBrackets(@PathVariable String sport) {
        log.info("Fetching tournament brackets for sport: {}", sport);
        List<TournamentMatchDTO> tournamentData = bracketService.getSportTournamentBrackets(sport);
        log.info("Returned {} tournament matches for sport: {}", tournamentData.size(), sport);
        return new ResponseEntity<>(tournamentData, HttpStatus.OK);
    }

    @PostMapping("/league")
    public ResponseEntity<BracketDTO> createLeagueBracket(@RequestBody BracketDTO bracketDTO) {
        BracketDTO createdBracket = bracketService.createLeagueBracket(bracketDTO);
        return new ResponseEntity<>(createdBracket, HttpStatus.CREATED);
    }

    @PutMapping("/league/{id}")
    public ResponseEntity<BracketDTO> updateLeagueBracket(@PathVariable Long id, @RequestBody BracketDTO bracketDTO) {
        BracketDTO updatedBracket = bracketService.updateLeagueBracket(id, bracketDTO);
        return new ResponseEntity<>(updatedBracket, HttpStatus.OK);
    }

    @DeleteMapping("/league/{id}")
    public ResponseEntity<Void> deleteLeagueBracket(@PathVariable Long id) {
        bracketService.deleteLeagueBracket(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/tournament")
    public ResponseEntity<BracketDTO> createTournamentBracket(@RequestBody BracketDTO bracketDTO) {
        BracketDTO createdBracket = bracketService.createTournamentBracket(bracketDTO);
        return new ResponseEntity<>(createdBracket, HttpStatus.CREATED);
    }

    @PutMapping("/tournament/{id}")
    public ResponseEntity<BracketDTO> updateTournamentBracket(@PathVariable Long id, @RequestBody BracketDTO bracketDTO) {
        BracketDTO updatedBracket = bracketService.updateTournamentBracket(id, bracketDTO);
        return new ResponseEntity<>(updatedBracket, HttpStatus.OK);
    }

    @DeleteMapping("/tournament/{id}")
    public ResponseEntity<Void> deleteTournamentBracket(@PathVariable Long id) {
        bracketService.deleteTournamentBracket(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/league/{sport}/{group}")
    public ResponseEntity<List<GroupDTO>> getLeagueBracketsByGroup(@PathVariable String sport, @PathVariable String group) {
        Map<String, List<GroupDTO>> allGroups = bracketService.getSportLeagueBrackets(sport);
        List<GroupDTO> groupData = allGroups.get(group);
        if (groupData == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(groupData);
    }
}