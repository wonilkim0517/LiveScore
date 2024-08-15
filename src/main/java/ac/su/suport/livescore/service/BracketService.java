package ac.su.suport.livescore.service;

import ac.su.suport.livescore.constant.DepartmentEnum;
import ac.su.suport.livescore.constant.MatchStatus;
import ac.su.suport.livescore.constant.MatchType;
import ac.su.suport.livescore.domain.Match;
import ac.su.suport.livescore.domain.MatchTeam;
import ac.su.suport.livescore.domain.Team;
import ac.su.suport.livescore.dto.*;
import ac.su.suport.livescore.repository.MatchRepository;
import ac.su.suport.livescore.repository.MatchTeamRepository;
import ac.su.suport.livescore.repository.TeamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BracketService {

    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final TeamRepository teamRepository;

    // 팀 ID를 키로 하고 TeamStandingDTO를 값으로 하는 Map을 사용하여 팀 순위 정보를 저장
    private final Map<Long, TeamStandingDTO> teamStandings = new HashMap<>();

    public Map<String, List<GroupDTO>> getSportLeagueBrackets(String sport) {
        List<Match> matches = matchRepository.findBySportAndMatchType(sport, MatchType.LEAGUE);
        Map<String, List<Team>> teamsByGroup = groupTeamsByGroup(matches);
        Map<String, List<GroupDTO>> result = new HashMap<>();

        for (Map.Entry<String, List<Team>> entry : teamsByGroup.entrySet()) {
            String groupName = entry.getKey();
            List<Team> teamsInGroup = entry.getValue();

            GroupDTO groupDTO = new GroupDTO();
            groupDTO.setGroup(groupName);
            groupDTO.setTeams(calculateStandings(teamsInGroup, matches));

            result.computeIfAbsent(groupName, k -> new ArrayList<>()).add(groupDTO);
        }

        return result;
    }

    public List<TournamentMatchDTO> getSportTournamentBrackets(String sport) {
        List<Match> matches = matchRepository.findBySportAndMatchType(sport, MatchType.TOURNAMENT);
        return matches.stream()
                .map(this::convertToTournamentMatchDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BracketDTO createLeagueBracket(BracketDTO bracketDTO) {
        Match match = convertToMatch(bracketDTO);
        match.setMatchTeams(new ArrayList<>());

        Team teamOne = teamRepository.findByDepartment(bracketDTO.getTeamOneName())
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + bracketDTO.getTeamOneName()));
        Team teamTwo = teamRepository.findByDepartment(bracketDTO.getTeamTwoName())
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + bracketDTO.getTeamTwoName()));

        MatchTeam matchTeamOne = new MatchTeam();
        matchTeamOne.setMatch(match);
        matchTeamOne.setTeam(teamOne);
        matchTeamOne.setScore(bracketDTO.getTeamOneScore());
        match.getMatchTeams().add(matchTeamOne);

        MatchTeam matchTeamTwo = new MatchTeam();
        matchTeamTwo.setMatch(match);
        matchTeamTwo.setTeam(teamTwo);
        matchTeamTwo.setScore(bracketDTO.getTeamTwoScore());
        match.getMatchTeams().add(matchTeamTwo);

        match = matchRepository.save(match);

        return convertToBracketDTO(match);
    }

    @Transactional
    public BracketDTO updateLeagueBracket(Long id, BracketDTO bracketDTO) {
        Match match = matchRepository.findById(id).orElseThrow(() -> new RuntimeException("Match not found"));
        MatchStatus oldStatus = match.getStatus();

        updateMatchFromDTO(match, bracketDTO);

        List<MatchTeam> matchTeams = match.getMatchTeams();
        if (matchTeams.size() >= 2) {
            MatchTeam teamOne = matchTeams.get(0);
            MatchTeam teamTwo = matchTeams.get(1);

            teamOne.setScore(bracketDTO.getTeamOneScore());
            teamTwo.setScore(bracketDTO.getTeamTwoScore());

            matchTeamRepository.save(teamOne);
            matchTeamRepository.save(teamTwo);

            // Only update standings if the status changed to PAST
            if (oldStatus != MatchStatus.PAST && match.getStatus() == MatchStatus.PAST) {
                updateLeagueStandings(teamOne, teamTwo);
            }
        }

        match = matchRepository.save(match);
        return convertToBracketDTO(match);
    }

    public void deleteLeagueBracket(Long id) {
        Match match = matchRepository.findById(id).orElseThrow();
        matchTeamRepository.deleteAll(match.getMatchTeams());
        matchRepository.delete(match);
    }

    public BracketDTO createTournamentBracket(BracketDTO bracketDTO) {
        Match match = convertToMatch(bracketDTO);
        matchRepository.save(match);
        saveMatchTeams(match, bracketDTO);
        return convertToBracketDTO(match);
    }

    public BracketDTO updateTournamentBracket(Long id, BracketDTO bracketDTO) {
        Match match = matchRepository.findById(id).orElseThrow();
        updateMatchFromDTO(match, bracketDTO);
        matchRepository.save(match);
        updateMatchTeams(match, bracketDTO);
        if (match.getStatus() == MatchStatus.PAST) {
            updateStandings(match);
        }
        return convertToBracketDTO(match);
    }

    public void deleteTournamentBracket(Long id) {
        Match match = matchRepository.findById(id).orElseThrow();
        matchTeamRepository.deleteAll(match.getMatchTeams());
        matchRepository.delete(match);
    }

    private Map<String, List<Team>> groupTeamsByGroup(List<Match> matches) {
        Map<String, List<Team>> groupedTeams = new HashMap<>();
        for (Match match : matches) {
            String groupName = match.getGroupName();
            for (MatchTeam matchTeam : match.getMatchTeams()) {
                Team team = matchTeam.getTeam();
                groupedTeams.computeIfAbsent(groupName, k -> new ArrayList<>()).add(team);
            }
        }
        return groupedTeams;
    }

    private void updateLeagueStandings(MatchTeam teamOne, MatchTeam teamTwo) {
        int scoreOne = teamOne.getScore();
        int scoreTwo = teamTwo.getScore();

        TeamStandingDTO standingOne = teamStandings.computeIfAbsent(teamOne.getTeam().getTeamId(),
                k -> new TeamStandingDTO(teamOne.getTeam().getTeamId(), teamOne.getTeam().getDepartment()));
        TeamStandingDTO standingTwo = teamStandings.computeIfAbsent(teamTwo.getTeam().getTeamId(),
                k -> new TeamStandingDTO(teamTwo.getTeam().getTeamId(), teamTwo.getTeam().getDepartment()));

        if (scoreOne > scoreTwo) {
            updateTeamStanding(standingOne, 1, 0, 0);
            updateTeamStanding(standingTwo, 0, 0, 1);
        } else if (scoreOne < scoreTwo) {
            updateTeamStanding(standingOne, 0, 0, 1);
            updateTeamStanding(standingTwo, 1, 0, 0);
        } else {
            updateTeamStanding(standingOne, 0, 1, 0);
            updateTeamStanding(standingTwo, 0, 1, 0);
        }
    }

    private void updateTeamStanding(TeamStandingDTO standing, int wins, int draws, int losses) {
        standing.setWin(standing.getWin() + wins);
        standing.setDraw(standing.getDraw() + draws);
        standing.setLose(standing.getLose() + losses);
        standing.updatePoints();
    }

    public List<TeamStandingDTO> getLeagueStandings(String sport, String groupName) {
        List<Match> matches = matchRepository.findBySportAndGroupNameAndStatus(sport, groupName, MatchStatus.PAST);
        Map<Long, TeamStandingDTO> standings = new HashMap<>();

        for (Match match : matches) {
            for (MatchTeam matchTeam : match.getMatchTeams()) {
                Team team = matchTeam.getTeam();
                standings.computeIfAbsent(team.getTeamId(),
                        k -> new TeamStandingDTO(team.getTeamId(), team.getDepartment()));
            }
            updateStandingsForMatch(match);
        }

        return new ArrayList<>(standings.values());
    }

    private void updateStandingsForMatch(Match match) {
        List<MatchTeam> matchTeams = match.getMatchTeams();
        if (matchTeams.size() >= 2) {
            MatchTeam teamOne = matchTeams.get(0);
            MatchTeam teamTwo = matchTeams.get(1);
            updateLeagueStandings(teamOne, teamTwo);
        }
    }

    private Match convertToMatch(BracketDTO bracketDTO) {
        Match match = new Match();
        updateMatchFromDTO(match, bracketDTO);
        return match;
    }

    private void updateMatchFromDTO(Match match, BracketDTO bracketDTO) {
        match.setSport(bracketDTO.getSports());
        match.setDate(bracketDTO.getMatchDate());
        match.setStartTime(bracketDTO.getStartTime());
        match.setMatchType(bracketDTO.getMatchType());
        match.setStatus(bracketDTO.getMatchStatus());
        match.setGroupName(bracketDTO.getGroupName());
        match.setRound(bracketDTO.getRound());
    }

    private void saveMatchTeams(Match match, BracketDTO bracketDTO) {
        Team teamOne = findOrCreateTeam(bracketDTO.getTeamOneName());
        Team teamTwo = findOrCreateTeam(bracketDTO.getTeamTwoName());

        teamOne = teamRepository.save(teamOne);
        teamTwo = teamRepository.save(teamTwo);

        MatchTeam matchTeamOne = new MatchTeam(match, teamOne, bracketDTO.getTeamOneScore());
        MatchTeam matchTeamTwo = new MatchTeam(match, teamTwo, bracketDTO.getTeamTwoScore());

        matchTeamRepository.save(matchTeamOne);
        matchTeamRepository.save(matchTeamTwo);
    }

    private Team findOrCreateTeam(DepartmentEnum department) {
        return teamRepository.findByDepartment(department)
                .orElseGet(() -> {
                    Team newTeam = new Team();
                    newTeam.setDepartment(department);
                    newTeam.setTeamName(department.name());
                    newTeam.setTeamPoint(0);
                    newTeam.setScore(0);
                    return teamRepository.save(newTeam);
                });
    }

    private void updateMatchTeams(Match match, BracketDTO bracketDTO) {
        List<MatchTeam> matchTeams = match.getMatchTeams();

        if (matchTeams.size() >= 2) {
            MatchTeam matchTeamOne = matchTeams.get(0);
            MatchTeam matchTeamTwo = matchTeams.get(1);

            Team teamOne = findOrCreateTeam(bracketDTO.getTeamOneName());
            Team teamTwo = findOrCreateTeam(bracketDTO.getTeamTwoName());

            matchTeamOne.setTeam(teamOne);
            matchTeamOne.setScore(bracketDTO.getTeamOneScore());

            matchTeamTwo.setTeam(teamTwo);
            matchTeamTwo.setScore(bracketDTO.getTeamTwoScore());

            matchTeamRepository.save(matchTeamOne);
            matchTeamRepository.save(matchTeamTwo);
        }
    }

    private void updateStandings(Match match) {
        List<MatchTeam> matchTeams = match.getMatchTeams();

        if (matchTeams.size() >= 2) {
            MatchTeam matchTeamOne = matchTeams.get(0);
            MatchTeam matchTeamTwo = matchTeams.get(1);
            updateLeagueStandings(matchTeamOne, matchTeamTwo);
        }
    }

    private List<TeamStandingDTO> calculateStandings(List<Team> teamsInGroup, List<Match> allMatches) {
        Map<Long, TeamStandingDTO> standings = new HashMap<>();

        for (Team team : teamsInGroup) {
            standings.put(team.getTeamId(), new TeamStandingDTO(team.getTeamId(), team.getDepartment()));
        }

        for (Match match : allMatches) {
            if (match.getStatus() == MatchStatus.PAST) {
                updateStandingsForMatch(match);
            }
        }

        return new ArrayList<>(standings.values());
    }

    private TournamentMatchDTO convertToTournamentMatchDTO(Match match) {
        TournamentMatchDTO dto = new TournamentMatchDTO();
        dto.setId(match.getMatchId());
        dto.setName("Round " + match.getRound() + " - Match " + match.getMatchId());
        dto.setNextMatchId(null); // 이 부분은 토너먼트 구조에 따라 별도로 설정해야 합니다
        dto.setTournamentRoundText("Round " + match.getRound());
        dto.setStartTime(match.getDate().atTime(match.getStartTime()).toString());
        dto.setState(match.getStatus().toString());

        List<ParticipantDTO> participants = new ArrayList<>();
        for (MatchTeam matchTeam : match.getMatchTeams()) {
            ParticipantDTO participant = new ParticipantDTO();
            participant.setId(matchTeam.getTeam().getTeamId().toString());
            participant.setResultText(matchTeam.getScore().toString());
            participant.setIsWinner(determineWinner(match, matchTeam));
            participant.setStatus(match.getStatus().toString());
            participant.setName(String.valueOf(matchTeam.getTeam().getDepartment()));
            participant.setImage("userImage"); // 실제 이미지 URL로 대체 필요
            participants.add(participant);
        }
        dto.setParticipants(participants);

        return dto;
    }

    private boolean determineWinner(Match match, MatchTeam matchTeam) {
        return match.getMatchTeams().stream()
                .max(Comparator.comparing(MatchTeam::getScore))
                .map(winner -> winner.equals(matchTeam))
                .orElse(false);
    }

    private BracketDTO convertToBracketDTO(Match match) {
        BracketDTO bracketDTO = new BracketDTO();
        bracketDTO.setMatchId(match.getMatchId());
        bracketDTO.setSports(match.getSport());
        bracketDTO.setMatchDate(match.getDate());
        bracketDTO.setStartTime(match.getStartTime());
        bracketDTO.setMatchType(match.getMatchType());
        bracketDTO.setMatchStatus(match.getStatus());
        bracketDTO.setGroupName(match.getGroupName());
        bracketDTO.setRound(match.getRound());

        List<MatchTeam> matchTeams = match.getMatchTeams();
        if (matchTeams != null && !matchTeams.isEmpty()) {
            if (matchTeams.size() >= 1) {
                MatchTeam teamOne = matchTeams.get(0);
                bracketDTO.setTeamOneName(teamOne.getTeam().getDepartment());
                bracketDTO.setTeamOneScore(teamOne.getScore());
            }
            if (matchTeams.size() >= 2) {
                MatchTeam teamTwo = matchTeams.get(1);
                bracketDTO.setTeamTwoName(teamTwo.getTeam().getDepartment());
                bracketDTO.setTeamTwoScore(teamTwo.getScore());
            }
        }

        return bracketDTO;
    }
}