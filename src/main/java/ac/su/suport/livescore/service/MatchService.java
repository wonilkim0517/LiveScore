package ac.su.suport.livescore.service;

import ac.su.suport.livescore.constant.MatchResult;
import ac.su.suport.livescore.constant.MatchStatus;
import ac.su.suport.livescore.domain.Match;
import ac.su.suport.livescore.domain.MatchTeam;
import ac.su.suport.livescore.domain.Team;
import ac.su.suport.livescore.dto.MatchModificationDTO;
import ac.su.suport.livescore.dto.MatchSummaryDTO;
import ac.su.suport.livescore.repository.MatchRepository;
import ac.su.suport.livescore.repository.MatchTeamRepository;
import ac.su.suport.livescore.repository.TeamRepository;
import ac.su.suport.livescore.repository.VideoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final TeamRepository teamRepository;
    private final VideoRepository videoRepository;

    @Autowired
    public MatchService(MatchRepository matchRepository, MatchTeamRepository matchTeamRepository, TeamRepository teamRepository, VideoRepository videoRepository) {
        this.matchRepository = matchRepository;
        this.matchTeamRepository = matchTeamRepository;
        this.teamRepository = teamRepository;
        this.videoRepository = videoRepository;
    }

    public List<MatchSummaryDTO.Response> getAllMatches() {
        return matchRepository.findAllByOrderByDateDesc().stream()
                .map(this::convertToMatchSummaryResponse)
                .collect(Collectors.toList());
    }

    public List<MatchSummaryDTO.Response> getFilteredMatches(String sport, LocalDate date, String department) {
        List<Match> matches;

        if (sport != null && date != null && department != null) {
            matches = matchRepository.findBySportAndDateAndDepartmentOrderByDateDesc(sport, date, department);
        } else if (sport != null && date != null) {
            matches = matchRepository.findBySportAndDateOrderByDateDesc(sport, date);
        } else if (sport != null && department != null) {
            matches = matchRepository.findBySportAndDepartmentOrderByDateDesc(sport, department);
        } else if (date != null && department != null) {
            matches = matchRepository.findByDateAndDepartmentOrderByDateDesc(date, department);
        } else if (sport != null) {
            matches = matchRepository.findBySportOrderByDateDesc(sport);
        } else if (date != null) {
            matches = matchRepository.findByDateOrderByDateDesc(date);
        } else if (department != null) {
            matches = matchRepository.findByDepartmentOrderByDateDesc(department);
        } else {
            matches = matchRepository.findAllByOrderByDateDesc();
        }

        return matches.stream()
                .distinct()
                .map(this::convertToMatchSummaryResponse)
                .collect(Collectors.toList());
    }

    public MatchSummaryDTO.Response getMatchById(Long id) {
        return matchRepository.findById(id)
                .map(this::convertToMatchSummaryResponse)
                .orElse(null);
    }

    @Transactional
    public boolean deleteMatch(Long id) {
        if (matchRepository.existsById(id)) {
            matchRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public MatchModificationDTO createMatch(MatchModificationDTO matchModificationDTO) {
        Match match = convertToEntity(matchModificationDTO);
        Match savedMatch = matchRepository.save(match);
        saveMatchTeam(savedMatch, matchModificationDTO.getTeamId1(), matchModificationDTO.getTeamId2());
        return convertToModificationDTO(savedMatch);
    }

    @Transactional
    public MatchModificationDTO updateMatch(Long id, MatchModificationDTO matchModificationDTO) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        MatchStatus oldStatus = match.getStatus();
        updateMatchDetails(match, matchModificationDTO);

        if (match.getStatus() == MatchStatus.PAST) {
            updateMatchTeams(match, matchModificationDTO);
            if (oldStatus != MatchStatus.PAST) {
                updateMatchResult(match);
            }
        } else {
            // For LIVE or FUTURE matches, only update non-score information
            matchModificationDTO.setTeamScore1(match.getMatchTeams().get(0).getScore());
            matchModificationDTO.setTeamScore2(match.getMatchTeams().get(1).getScore());
        }

        Match updatedMatch = matchRepository.save(match);
        return convertToModificationDTO(updatedMatch);
    }

    private void updateMatchResult(Match match) {
        List<MatchTeam> matchTeams = match.getMatchTeams();
        if (matchTeams.size() >= 2) {
            MatchTeam team1 = matchTeams.get(0);
            MatchTeam team2 = matchTeams.get(1);

            int scoreOne = team1.getScore();
            int scoreTwo = team2.getScore();

            MatchResult result;
            if (scoreOne > scoreTwo) {
                result = MatchResult.TEAM_ONE_WIN;
            } else if (scoreOne < scoreTwo) {
                result = MatchResult.TEAM_TWO_WIN;
            } else {
                result = MatchResult.DRAW;
            }

            match.setResult(result);
            updateTeamPoints(team1.getTeam(), team2.getTeam(), result);
        }
    }

    private void updateTeamPoints(Team team1, Team team2, MatchResult result) {
        switch (result) {
            case TEAM_ONE_WIN:
                addPoints(team1, 3);
                break;
            case TEAM_TWO_WIN:
                addPoints(team2, 3);
                break;
            case DRAW:
                addPoints(team1, 1);
                addPoints(team2, 1);
                break;
        }
    }

    private void addPoints(Team team, int points) {
        team.setTeamPoint(team.getTeamPoint() + points);
        teamRepository.save(team);
    }

    private void saveMatchTeam(Match match, Long teamId1, Long teamId2) {
        MatchTeam matchTeam1 = new MatchTeam();
        matchTeam1.setMatch(match);
        matchTeam1.setTeam(teamRepository.findById(teamId1).orElseThrow(() -> new RuntimeException("Team not found")));
        matchTeam1.setLineup(null);
        matchTeamRepository.save(matchTeam1);

        MatchTeam matchTeam2 = new MatchTeam();
        matchTeam2.setMatch(match);
        matchTeam2.setTeam(teamRepository.findById(teamId2).orElseThrow(() -> new RuntimeException("Team not found")));
        matchTeam2.setLineup(null);
        matchTeamRepository.save(matchTeam2);

        match.setMatchTeams(List.of(matchTeam1, matchTeam2));
    }

    private void updateMatchDetails(Match match, MatchModificationDTO matchModificationDTO) {
        match.setSport(matchModificationDTO.getSport());
        match.setDate(matchModificationDTO.getDate());
        match.setStartTime(matchModificationDTO.getStartTime());
        match.setMatchType(matchModificationDTO.getMatchType());
        match.setStatus(matchModificationDTO.getMatchStatus());
        match.setGroupName(matchModificationDTO.getGroupName());
        match.setRound(matchModificationDTO.getRound());
    }

    private void updateMatchTeams(Match match, MatchModificationDTO matchModificationDTO) {
        List<MatchTeam> matchTeams = match.getMatchTeams();
        if (matchTeams.size() >= 2) {
            updateTeam(matchTeams.get(0), matchModificationDTO.getTeamId1(), matchModificationDTO.getTeamScore1());
            updateTeam(matchTeams.get(1), matchModificationDTO.getTeamId2(), matchModificationDTO.getTeamScore2());
        }
    }

    private void updateTeam(MatchTeam matchTeam, Long newTeamId, Integer newScore) {
        if (!matchTeam.getTeam().getTeamId().equals(newTeamId)) {
            Team newTeam = teamRepository.findById(newTeamId)
                    .orElseThrow(() -> new RuntimeException("Team not found"));
            matchTeam.setTeam(newTeam);
        }
        matchTeam.setScore(newScore);
        matchTeamRepository.save(matchTeam);
    }

    private MatchSummaryDTO.Response convertToMatchSummaryResponse(Match match) {
        MatchSummaryDTO.Response dto = new MatchSummaryDTO.Response();
        dto.setMatchId(match.getMatchId());
        dto.setSport(match.getSport());
        dto.setDate(match.getDate());
        dto.setStartTime(match.getStartTime());
        dto.setStatus(match.getStatus().toString());
        dto.setGroupName(match.getGroupName());
        dto.setRound(match.getRound());

        List<MatchTeam> matchTeams = match.getMatchTeams();
        if (matchTeams != null && matchTeams.size() >= 2) {
            MatchTeam team1 = matchTeams.get(0);
            MatchTeam team2 = matchTeams.get(1);
            dto.setTeamName1(team1.getTeam().getTeamName());
            dto.setTeamName2(team2.getTeam().getTeamName());
            dto.setDepartment1(String.valueOf(team1.getTeam().getDepartment()));
            dto.setDepartment2(String.valueOf(team2.getTeam().getDepartment()));
            dto.setTeamScore1(team1.getScore() != null ? team1.getScore() : 0);
            dto.setTeamScore2(team2.getScore() != null ? team2.getScore() : 0);
        }

        if (match.getStatus() == MatchStatus.PAST) {
            dto.setResult(match.getResult() != null ? match.getResult() : MatchResult.NOT_PLAYED);
        } else {
            dto.setResult(MatchResult.NOT_PLAYED);
        }

        return dto;
    }

        public List<Match> getAllMatchesWithVideos() {
        List<Match> matches = matchRepository.findAll();
        for (Match match : matches) {
            match.setVideos(videoRepository.findByMatch(match));
        }
        return matches;
    }

    private MatchModificationDTO convertToModificationDTO(Match savedMatch) {
        MatchModificationDTO dto = new MatchModificationDTO();

        MatchTeam matchTeam1 = savedMatch.getMatchTeams().get(0);
        MatchTeam matchTeam2 = savedMatch.getMatchTeams().get(1);

        dto.setTeamId1(matchTeam1.getTeam().getTeamId());
        dto.setTeamId2(matchTeam2.getTeam().getTeamId());
        dto.setTeamScore1(matchTeam1.getScore());
        dto.setTeamScore2(matchTeam2.getScore());

        dto.setSport(savedMatch.getSport());
        dto.setDate(savedMatch.getDate());
        dto.setStartTime(savedMatch.getStartTime());
        dto.setMatchType(savedMatch.getMatchType());
        dto.setMatchStatus(savedMatch.getStatus());
        dto.setGroupName(savedMatch.getGroupName());
        dto.setRound(savedMatch.getRound());

        return dto;
    }

    private Match convertToEntity(MatchModificationDTO matchModificationDTO) {
        Match match = new Match();
        match.setSport(matchModificationDTO.getSport());
        match.setDate(matchModificationDTO.getDate());
        match.setStartTime(matchModificationDTO.getStartTime());
        match.setMatchType(matchModificationDTO.getMatchType());
        match.setStatus(matchModificationDTO.getMatchStatus());
        match.setGroupName(matchModificationDTO.getGroupName());
        match.setRound(matchModificationDTO.getRound());

        LocalDateTime matchDateTime = LocalDateTime.of(matchModificationDTO.getDate(), matchModificationDTO.getStartTime());
        LocalDateTime now = LocalDateTime.now();

        if (matchDateTime.isBefore(now)) {
            match.setStatus(MatchStatus.PAST);
        } else {
            match.setStatus(MatchStatus.FUTURE);
        }

        return match;
    }
}