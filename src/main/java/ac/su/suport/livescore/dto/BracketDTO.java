package ac.su.suport.livescore.dto;

import ac.su.suport.livescore.constant.DepartmentEnum;
import ac.su.suport.livescore.constant.MatchResult;
import ac.su.suport.livescore.constant.MatchStatus;
import ac.su.suport.livescore.constant.MatchType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
public class BracketDTO {
    private Long matchId;
    private String sports;
    private LocalDate matchDate;
    private LocalTime startTime;
    private MatchType matchType;
    private MatchStatus matchStatus;
    private String groupName;
    private String round;
    private DepartmentEnum teamOneName;
    private DepartmentEnum teamTwoName;
    private Integer teamOneScore;
    private Integer teamTwoScore;


}
