package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.constant.SportEnum;
import ac.su.suport.livescore.service.SportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sports")
public class SportController {

    private final SportService sportService;

    public SportController(SportService sportService) {
        this.sportService = sportService;
    }

    @GetMapping
    public List<SportEnum> getAllSports() {
        return sportService.getAllSports();
    }
}
