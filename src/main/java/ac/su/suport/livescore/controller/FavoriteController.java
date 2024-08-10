package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.domain.User;
import ac.su.suport.livescore.dto.FavoriteDTO;
import ac.su.suport.livescore.service.FavoriteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @GetMapping
    public List<FavoriteDTO> getFavorites(HttpSession session) {
        Long userId = ((User) session.getAttribute("currentUser")).getUserId();
        return favoriteService.getFavoritesByUserId(userId);
    }

    @PostMapping("/{matchId}")
    public FavoriteDTO addFavorite(@PathVariable Long matchId, HttpSession session) {
        Long userId = ((User) session.getAttribute("currentUser")).getUserId();
        return favoriteService.addFavorite(userId, matchId);
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long favoriteId) {
        favoriteService.removeFavorite(favoriteId);
        return ResponseEntity.noContent().build();
    }
}