package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.dto.FavoriteDTO;
import ac.su.suport.livescore.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/bookmark")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @GetMapping
    public List<FavoriteDTO> getFavorites(@PathVariable Long userId) {
        return favoriteService.getFavoritesByUserId(userId);
    }

    @PostMapping
    public FavoriteDTO addFavorite(@PathVariable Long userId, @RequestBody FavoriteDTO favoriteDTO) {
        favoriteDTO.setUserId(userId);
        return favoriteService.addFavorite(favoriteDTO);
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long favoriteId) {
        favoriteService.removeFavorite(favoriteId);
        return ResponseEntity.noContent().build();
    }
}
