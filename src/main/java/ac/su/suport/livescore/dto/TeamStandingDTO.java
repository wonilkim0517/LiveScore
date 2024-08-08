package ac.su.suport.livescore.dto;

import lombok.Data;

@Data
public class TeamStandingDTO {
    private String teamName;
    private int played;
    private int won;
    private int drawn;
    private int lost;
    private int points;

    public TeamStandingDTO(String teamName) {
        this.teamName = teamName;
    }

    public void incrementPlayed() {
        this.played++;
    }

    public void incrementWon() {
        this.won++;
    }

    public void incrementDrawn() {
        this.drawn++;
    }

    public void incrementLost() {
        this.lost++;
    }

    public void addPoints(int points) {
        this.points += points;
    }
}
