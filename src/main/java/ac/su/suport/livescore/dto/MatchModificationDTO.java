package ac.su.suport.livescore.dto;

import ac.su.suport.livescore.constant.MatchStatus;
import ac.su.suport.livescore.constant.MatchType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MatchModificationDTO {
    private Long teamId1;
    private Long teamId2;
    private Integer teamScore1;
    private Integer teamScore2;
    private String sport;
    private LocalDate date;
    private LocalTime startTime;
    private MatchType matchType;
    private MatchStatus matchStatus;
    private String groupName;
    private String round;
}