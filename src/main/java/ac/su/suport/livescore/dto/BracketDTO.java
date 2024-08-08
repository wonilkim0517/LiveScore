package ac.su.suport.livescore.dto;

import ac.su.suport.livescore.constant.MatchResult;
import ac.su.suport.livescore.constant.MatchStatus;
import ac.su.suport.livescore.constant.MatchType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class BracketDTO {
    private Long matchId;
    private Long teamOneId;
    private Long teamTwoId;
    private String teamOneName;
    private String teamTwoName;
    private MatchStatus matchStatus; // "예정", "진행 중", "종료" 등
    private MatchType matchType; // "리그", "토너먼트"
    private LocalDate matchDate;
    private LocalTime startTime;
    private String sports;
    private String round; // "조별리그", "16강", "8강" 등
    private String groupName; // "A조", "B조" 등
    private MatchResult matchResult; // 경기 승, 무, 패
    private Integer teamOneScore;
    private Integer teamTwoScore;
    private Integer teamOnePoints; // 팀1 점수 추가
    private Integer teamTwoPoints; // 팀2 점수 추가
}
