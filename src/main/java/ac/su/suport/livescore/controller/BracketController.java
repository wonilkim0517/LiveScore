package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.dto.BracketDTO;
import ac.su.suport.livescore.dto.GroupDTO;
import ac.su.suport.livescore.service.BracketService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/brackets")
public class BracketController {

    private final BracketService bracketService;

    @GetMapping("/league")
    public ResponseEntity<List<GroupDTO>> getSoccerLeagueBrackets() {
        List<GroupDTO> groupDTOs = bracketService.getSportLeagueBrackets("Soccer");
        return new ResponseEntity<>(groupDTOs, HttpStatus.OK);
    }

    @GetMapping("/league/{sport}")
    public ResponseEntity<List<GroupDTO>> getSportLeagueBrackets(@PathVariable("sport") String sport) {
        List<GroupDTO> groupDTOs = bracketService.getSportLeagueBrackets(sport);
        return new ResponseEntity<>(groupDTOs, HttpStatus.OK);
    }

    @GetMapping("/tournament")
    public ResponseEntity<List<BracketDTO>> getSoccerTournamentBrackets() {
        List<BracketDTO> bracketDTOs = bracketService.getSportTournamentBrackets("SOCCER");
        return new ResponseEntity<>(bracketDTOs, HttpStatus.OK);
    }

    @GetMapping("/tournament/{sport}")
    public ResponseEntity<List<BracketDTO>> getSportTournamentBrackets(@PathVariable("sport") String sport) {
        List<BracketDTO> bracketDTOs = bracketService.getSportTournamentBrackets(sport);
        return new ResponseEntity<>(bracketDTOs, HttpStatus.OK);
    }

    @PostMapping("/league")
    public ResponseEntity<?> createLeagueBracket(@RequestBody BracketDTO bracketDTO) {
        try {
            BracketDTO createdBracket = bracketService.createLeagueBracket(bracketDTO);
            return new ResponseEntity<>(createdBracket, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
}
