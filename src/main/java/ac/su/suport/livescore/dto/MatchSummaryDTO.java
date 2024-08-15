package ac.su.suport.livescore.dto;

import ac.su.suport.livescore.constant.MatchStatus;
import ac.su.suport.livescore.constant.MatchType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

public class MatchSummaryDTO {
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        private String teamName1;
        private String teamName2;
        private String department1;
        private String department2;
        private int teamScore1;
        private int teamScore2;
        private String sport;
        private MatchStatus status;  // 수정: String에서 MatchStatus로 변경
        private LocalDate date;
        private LocalTime startTime;
        private MatchType matchType;  // 추가
        private String groupName;  // 추가
        private String round;  // 추가
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private Long matchId;
        private String teamName1;
        private String teamName2;
        private String department1;
        private String department2;
        private int teamScore1;
        private int teamScore2;
        private String sport;
        private MatchStatus status;  // 수정: String에서 MatchStatus로 변경
        private LocalDate date;
        private LocalTime startTime;
        private MatchType matchType;  // 추가
        private String groupName;
        private String round;
    }
}