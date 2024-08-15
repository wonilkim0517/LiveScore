package ac.su.suport.livescore.service;

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
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
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
    public MatchModificationDTO createMatch(MatchModificationDTO matchDTO) {
        Team team1 = teamRepository.findById(matchDTO.getTeamId1())
                .orElseThrow(() -> new EntityNotFoundException("Team 1 not found"));
        Team team2 = teamRepository.findById(matchDTO.getTeamId2())
                .orElseThrow(() -> new EntityNotFoundException("Team 2 not found"));

        Match match = new Match();
        updateMatchFromDTO(match, matchDTO);

        MatchTeam matchTeam1 = new MatchTeam(match, team1, matchDTO.getTeamScore1() != null ? matchDTO.getTeamScore1() : 0);
        MatchTeam matchTeam2 = new MatchTeam(match, team2, matchDTO.getTeamScore2() != null ? matchDTO.getTeamScore2() : 0);

        match.setMatchTeams(Arrays.asList(matchTeam1, matchTeam2));

        Match savedMatch = matchRepository.save(match);
        return convertToMatchModificationDTO(savedMatch);
    }

    @Transactional
    public MatchModificationDTO updateMatch(Long matchId, MatchModificationDTO matchDTO) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found"));

        updateMatchFromDTO(match, matchDTO);

        if (match.getMatchTeams().size() >= 2) {
            MatchTeam matchTeam1 = match.getMatchTeams().get(0);
            MatchTeam matchTeam2 = match.getMatchTeams().get(1);

            updateTeamIfChanged(matchTeam1, matchDTO.getTeamId1(), matchDTO.getTeamScore1());
            updateTeamIfChanged(matchTeam2, matchDTO.getTeamId2(), matchDTO.getTeamScore2());

            matchTeamRepository.saveAll(Arrays.asList(matchTeam1, matchTeam2));
        }

        Match updatedMatch = matchRepository.save(match);
        return convertToMatchModificationDTO(updatedMatch);
    }

    private void updateTeamIfChanged(MatchTeam matchTeam, Long newTeamId, Integer newScore) {
        if (!matchTeam.getTeam().getTeamId().equals(newTeamId)) {
            Team newTeam = teamRepository.findById(newTeamId)
                    .orElseThrow(() -> new EntityNotFoundException("Team not found"));
            matchTeam.setTeam(newTeam);
        }
        matchTeam.setScore(newScore);
    }

    private MatchSummaryDTO.Response convertToMatchSummaryResponse(Match match) {
        MatchSummaryDTO.Response dto = new MatchSummaryDTO.Response();
        dto.setMatchId(match.getMatchId());
        dto.setSport(match.getSport());
        dto.setDate(match.getDate());
        dto.setStartTime(match.getStartTime());
        dto.setStatus(MatchStatus.valueOf(match.getStatus().toString()));
        dto.setGroupName(match.getGroupName());  // 추가
        dto.setRound(match.getRound());  // 추가

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

        return dto;
    }

    public List<Match> getAllMatchesWithVideos() {
        List<Match> matches = matchRepository.findAll();
        for (Match match : matches) {
            match.setVideos(videoRepository.findByMatch(match));
        }
        return matches;
    }

    private void updateMatchFromDTO(Match match, MatchModificationDTO matchDTO) {
        match.setSport(matchDTO.getSport());
        match.setDate(matchDTO.getDate());
        match.setStartTime(matchDTO.getStartTime());
        match.setMatchType(matchDTO.getMatchType());
        match.setStatus(matchDTO.getMatchStatus());
        match.setGroupName(matchDTO.getGroupName());
        match.setRound(matchDTO.getRound());

        LocalDateTime matchDateTime = LocalDateTime.of(matchDTO.getDate(), matchDTO.getStartTime());
        LocalDateTime now = LocalDateTime.now();

        if (matchDateTime.isBefore(now)) {
            match.setStatus(MatchStatus.PAST);
        } else if (match.getStatus() != MatchStatus.LIVE) {
            match.setStatus(MatchStatus.FUTURE);
        }
    }

    private MatchModificationDTO convertToMatchModificationDTO(Match match) {
        MatchModificationDTO dto = new MatchModificationDTO();
        dto.setSport(match.getSport());
        dto.setDate(match.getDate());
        dto.setStartTime(match.getStartTime());
        dto.setMatchType(match.getMatchType());
        dto.setMatchStatus(match.getStatus());
        dto.setGroupName(match.getGroupName());
        dto.setRound(match.getRound());

        List<MatchTeam> matchTeams = match.getMatchTeams();
        if (matchTeams != null && matchTeams.size() >= 2) {
            MatchTeam team1 = matchTeams.get(0);
            MatchTeam team2 = matchTeams.get(1);
            dto.setTeamId1(team1.getTeam().getTeamId());
            dto.setTeamId2(team2.getTeam().getTeamId());
            dto.setTeamScore1(team1.getScore());
            dto.setTeamScore2(team2.getScore());
        }

        return dto;
    }
}