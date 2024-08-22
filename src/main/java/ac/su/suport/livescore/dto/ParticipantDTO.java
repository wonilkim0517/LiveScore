package ac.su.suport.livescore.dto;

import lombok.Data;

@Data
public class ParticipantDTO {
    private String id;
    private String resultText;
    private boolean isWinner;
    private String status;
    private String name;
    private String image;
    private String SubScore;

    public void setIsWinner(boolean isWinner) {
        this.isWinner = isWinner;
    }
}