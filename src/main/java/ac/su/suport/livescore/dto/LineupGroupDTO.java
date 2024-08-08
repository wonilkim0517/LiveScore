package ac.su.suport.livescore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class LineupGroupDTO {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        private String teamName;
        private String department;
        private List<PlayerDTO.Request> lineup;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private String teamName;
        private String department;
        private List<PlayerDTO.Response> lineup;
    }
}