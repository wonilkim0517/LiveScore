package ac.su.suport.livescore.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupDTO {
    private String groupName;
    private List<BracketDTO> matches;
    private List<TeamStandingDTO> standings;
}
