package ac.su.suport.livescore.domain;

import ac.su.suport.livescore.domain.Lineup;
import ac.su.suport.livescore.domain.MatchTeam;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Entity
@Table(name = "teams")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "department")
    private String department;

    @Column(name = "team_point")
    private Integer teamPoint;

    @Column(name = "score")
    private Integer score;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lineup> lineups;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchTeam> matchTeams;
}