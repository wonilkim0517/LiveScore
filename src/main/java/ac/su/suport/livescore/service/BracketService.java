package ac.su.suport.livescore.service;

import ac.su.suport.livescore.constant.*;
import ac.su.suport.livescore.domain.Match;
import ac.su.suport.livescore.domain.MatchTeam;
import ac.su.suport.livescore.domain.Team;
import ac.su.suport.livescore.dto.*;
import ac.su.suport.livescore.repository.MatchRepository;
import ac.su.suport.livescore.repository.MatchTeamRepository;
import ac.su.suport.livescore.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BracketService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final MatchTeamRepository matchTeamRepository;

    public List<MatchSummaryDTO.Response> getAllMatches() {
        List<Match> matches = matchRepository.findAll();
        return matches.stream()
                .map(this::convertToMatchSummaryDTO)
                .collect(Collectors.toList());
    }private int getMatchesCountForRound(TournamentRound round) {
        switch (round) {
            case QUARTER_FINALS:
                return 4;
            case SEMI_FINALS:
                return 2;
            case FINAL:
                return 1;
            default:
                throw new IllegalArgumentException("Unknown round: " + round);
        }
    }

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
        updateMatchFromDTO(match, bracketDTO);

        List<MatchTeam> matchTeams = match.getMatchTeams();
        if (matchTeams.size() >= 2) {
            MatchTeam teamOne = matchTeams.get(0);
            MatchTeam teamTwo = matchTeams.get(1);

            teamOne.setScore(bracketDTO.getTeamOneScore());
            teamTwo.setScore(bracketDTO.getTeamTwoScore());

            matchTeamRepository.save(teamOne);
            matchTeamRepository.save(teamTwo);

            if (match.getStatus() == MatchStatus.PAST) {
                updateTeamStandings(teamOne, teamTwo);
            }
        }

        match = matchRepository.save(match);
        return convertToBracketDTO(match);
    }

    private void updateTeamStandings(MatchTeam teamOne, MatchTeam teamTwo) {
        Team team1 = teamOne.getTeam();
        Team team2 = teamTwo.getTeam();

        int scoreOne = teamOne.getScore();
        int scoreTwo = teamTwo.getScore();

        if (scoreOne > scoreTwo) {
            team1.setTeamPoint(team1.getTeamPoint() + 3);
        } else if (scoreOne < scoreTwo) {
            team2.setTeamPoint(team2.getTeamPoint() + 3);
        } else {
            team1.setTeamPoint(team1.getTeamPoint() + 1);
            team2.setTeamPoint(team2.getTeamPoint() + 1);
        }

        teamRepository.save(team1);
        teamRepository.save(team2);
    }

    @Transactional
    public void deleteLeagueBracket(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));
        matchRepository.delete(match);
    }

    @Transactional
    public BracketDTO createTournamentBracket(BracketDTO bracketDTO) {
        Match match = convertToMatch(bracketDTO);
        match.setMatchTeams(new ArrayList<>());

        Team teamOne = findOrCreateTeam(bracketDTO.getTeamOneName());
        Team teamTwo = findOrCreateTeam(bracketDTO.getTeamTwoName());

        MatchTeam matchTeamOne = new MatchTeam(match, teamOne, bracketDTO.getTeamOneScore());
        matchTeamOne.setSubScores(bracketDTO.getTeamOneSubScores());

        MatchTeam matchTeamTwo = new MatchTeam(match, teamTwo, bracketDTO.getTeamTwoScore());
        matchTeamTwo.setSubScores(bracketDTO.getTeamTwoSubScores());

        match.getMatchTeams().add(matchTeamOne);
        match.getMatchTeams().add(matchTeamTwo);

        match = matchRepository.save(match);
        return convertToBracketDTO(match);
    }

    public BracketDTO updateTournamentBracket(Long id, BracketDTO bracketDTO) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));
        updateMatchFromDTO(match, bracketDTO);
        matchRepository.save(match);
        updateMatchTeams(match, bracketDTO);
        if (match.getStatus() == MatchStatus.PAST) {
            updateStandings(match);
        }
        return convertToBracketDTO(match);
    }

    @Transactional
    public void deleteTournamentBracket(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));
        matchRepository.delete(match);
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

    private Team findOrCreateTeam(DepartmentEnum department) {
        if (department == null) {
            throw new IllegalArgumentException("Department cannot be null");
        }
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
            matchTeamOne.setSubScores(bracketDTO.getTeamOneSubScores());

            matchTeamTwo.setTeam(teamTwo);
            matchTeamTwo.setScore(bracketDTO.getTeamTwoScore());
            matchTeamTwo.setSubScores(bracketDTO.getTeamTwoSubScores());

            matchTeamRepository.save(matchTeamOne);
            matchTeamRepository.save(matchTeamTwo);
        }
    }

    private void updateStandings(Match match) {
        List<MatchTeam> matchTeams = match.getMatchTeams();

        if (matchTeams.size() >= 2) {
            MatchTeam matchTeamOne = matchTeams.get(0);
            MatchTeam matchTeamTwo = matchTeams.get(1);

            Team teamOne = matchTeamOne.getTeam();
            Team teamTwo = matchTeamTwo.getTeam();

            if (matchTeamOne.getScore() > matchTeamTwo.getScore()) {
                teamOne.setTeamPoint(teamOne.getTeamPoint() + 3);
            } else if (matchTeamOne.getScore() < matchTeamTwo.getScore()) {
                teamTwo.setTeamPoint(teamTwo.getTeamPoint() + 3);
            } else {
                teamOne.setTeamPoint(teamOne.getTeamPoint() + 1);
                teamTwo.setTeamPoint(teamTwo.getTeamPoint() + 1);
            }

            teamRepository.save(teamOne);
            teamRepository.save(teamTwo);
        }
    }

    private List<TeamStandingDTO> calculateStandings(List<Team> teamsInGroup, List<Match> allMatches) {
        Map<Long, TeamStandingDTO> standings = new HashMap<>();

        for (Team team : teamsInGroup) {
            standings.put(team.getTeamId(), new TeamStandingDTO(team.getTeamId(), team.getDepartment()));
        }

        for (Match match : allMatches) {
            List<MatchTeam> matchTeams = match.getMatchTeams();
            if (matchTeams.size() >= 2) {
                MatchTeam teamOne = matchTeams.get(0);
                MatchTeam teamTwo = matchTeams.get(1);

                if (standings.containsKey(teamOne.getTeam().getTeamId()) &&
                        standings.containsKey(teamTwo.getTeam().getTeamId())) {
                    TeamStandingDTO standingOne = standings.get(teamOne.getTeam().getTeamId());
                    TeamStandingDTO standingTwo = standings.get(teamTwo.getTeam().getTeamId());

                    standingOne.addMatchId(match.getMatchId());
                    standingTwo.addMatchId(match.getMatchId());

                    if (match.getStatus() == MatchStatus.PAST) {
                        updateStandings(teamOne, teamTwo, standingOne, standingTwo);
                    }
                }
            }
        }

        return new ArrayList<>(standings.values());
    }

    private void updateStandings(MatchTeam teamOne, MatchTeam teamTwo,
                                 TeamStandingDTO standingOne, TeamStandingDTO standingTwo) {
        int scoreOne = teamOne.getScore();
        int scoreTwo = teamTwo.getScore();

        if (scoreOne > scoreTwo) {
            standingOne.setWin(standingOne.getWin() + 1);
            standingTwo.setLose(standingTwo.getLose() + 1);
        } else if (scoreOne < scoreTwo) {
            standingTwo.setWin(standingTwo.getWin() + 1);
            standingOne.setLose(standingOne.getLose() + 1);
        } else {
            standingOne.setDraw(standingOne.getDraw() + 1);
            standingTwo.setDraw(standingTwo.getDraw() + 1);
        }

        standingOne.updatePoints();
        standingTwo.updatePoints();
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
        if (matchTeams != null && matchTeams.size() >= 2) {
            MatchTeam teamOne = matchTeams.get(0);
            MatchTeam teamTwo = matchTeams.get(1);

            bracketDTO.setTeamOneName(teamOne.getTeam().getDepartment());
            bracketDTO.setTeamTwoName(teamTwo.getTeam().getDepartment());
            bracketDTO.setTeamOneScore(teamOne.getScore());
            bracketDTO.setTeamTwoScore(teamTwo.getScore());
            bracketDTO.setTeamOneSubScores(teamOne.getSubScores());
            bracketDTO.setTeamTwoSubScores(teamTwo.getSubScores());

            // 서브스코어 표시 여부 설정
            bracketDTO.setShowSubScores(match.getSport().equals("SOCCER") ?
                    (teamOne.getSubScores() != null && !teamOne.getSubScores().isEmpty()) : true);
        }

        return bracketDTO;
    }

    private void updateNextMatchIds(List<TournamentMatchDTO> matches) {
        Map<String, List<TournamentMatchDTO>> roundMatches = matches.stream()
                .collect(Collectors.groupingBy(TournamentMatchDTO::getTournamentRoundText));

        List<TournamentRound> rounds = Arrays.asList(TournamentRound.values());
        for (int i = 0; i < rounds.size() - 1; i++) {
            TournamentRound currentRound = rounds.get(i);
            TournamentRound nextRound = rounds.get(i + 1);

            List<TournamentMatchDTO> currentMatches = roundMatches.get(currentRound.name());
            List<TournamentMatchDTO> nextMatches = roundMatches.get(nextRound.name());

            if (currentMatches != null && nextMatches != null) {
                for (int j = 0; j < currentMatches.size(); j += 2) {
                    if (j / 2 < nextMatches.size()) {
                        Long nextMatchId = nextMatches.get(j / 2).getId();
                        currentMatches.get(j).setNextMatchId(nextMatchId);
                        if (j + 1 < currentMatches.size()) {
                            currentMatches.get(j + 1).setNextMatchId(nextMatchId);
                        }
                    }
                }
            }
        }
    }


    public List<TournamentMatchDTO> getSportTournamentBrackets(String sport) {
        List<Match> matches = matchRepository.findBySportAndMatchTypeOrderByRoundDesc(sport, MatchType.TOURNAMENT);
        List<TournamentMatchDTO> dtos = matches.stream()
                .map(this::convertToTournamentMatchDTO)
                .collect(Collectors.toList());

        updateNextMatchIds(dtos);
        createNextRoundMatchesIfNeeded(dtos, sport);
        return dtos;
    }

    private void createNextRoundMatchesIfNeeded(List<TournamentMatchDTO> matches, String sport) {
        Map<String, List<TournamentMatchDTO>> roundMatches = matches.stream()
                .collect(Collectors.groupingBy(TournamentMatchDTO::getTournamentRoundText));

        List<TournamentRound> rounds = Arrays.asList(TournamentRound.values());
        for (int i = 0; i < rounds.size() - 1; i++) {
            TournamentRound currentRound = rounds.get(i);
            TournamentRound nextRound = rounds.get(i + 1);

            List<TournamentMatchDTO> currentMatches = roundMatches.get(currentRound.name());
            List<TournamentMatchDTO> nextMatches = roundMatches.get(nextRound.name());

            if (currentMatches != null && (nextMatches == null || nextMatches.isEmpty())) {
                boolean allMatchesFinished = currentMatches.stream()
                        .allMatch(match -> "PAST".equals(match.getState()));

                if (allMatchesFinished) {
                    createNextRoundMatches(currentMatches, nextRound, sport);
                }
            }
        }
    }

    private void createNextRoundMatches(List<TournamentMatchDTO> currentMatches, TournamentRound nextRound, String sport) {
        for (int i = 0; i < currentMatches.size(); i += 2) {
            TournamentMatchDTO match1 = currentMatches.get(i);
            TournamentMatchDTO match2 = i + 1 < currentMatches.size() ? currentMatches.get(i + 1) : null;

            ParticipantDTO winner1 = getWinner(match1);
            ParticipantDTO winner2 = match2 != null ? getWinner(match2) : null;

            if (winner1 != null && winner2 != null) {
                Match newMatch = new Match();
                newMatch.setSport(sport);
                newMatch.setMatchType(MatchType.TOURNAMENT);
                newMatch.setRound(nextRound.name());
                newMatch.setStatus(MatchStatus.FUTURE);
                newMatch.setDate(LocalDate.now().plusDays(7));
                newMatch.setStartTime(LocalTime.of(18, 0));

                newMatch = matchRepository.save(newMatch);

                createMatchTeam(newMatch, winner1, true);
                createMatchTeam(newMatch, winner2, true);
            }
        }
    }

    private ParticipantDTO getWinner(TournamentMatchDTO match) {
        return match.getParticipants().stream()
                .filter(ParticipantDTO::getWinner)
                .findFirst()
                .orElse(null);
    }

    private void createMatchTeam(Match match, ParticipantDTO participant, boolean isWinner) {
        Team team = teamRepository.findById(Long.parseLong(participant.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        MatchTeam matchTeam = new MatchTeam();
        matchTeam.setMatch(match);
        matchTeam.setTeam(team);
        matchTeam.setScore(isWinner ? Integer.parseInt(participant.getResultText()) : 0);

        matchTeamRepository.save(matchTeam);
    }

    private TournamentMatchDTO convertToTournamentMatchDTO(Match match) {
        TournamentMatchDTO dto = new TournamentMatchDTO();
        dto.setId(match.getMatchId());
        dto.setName(match.getRound() + " - Match " + match.getMatchId());
        dto.setTournamentRoundText(match.getRound());
        dto.setStartTime(match.getDate().atTime(match.getStartTime()).toString());
        dto.setState(match.getStatus().toString());

        List<ParticipantDTO> participants = match.getMatchTeams().stream()
                .map(this::convertToParticipantDTO)
                .collect(Collectors.toList());
        dto.setParticipants(participants);

        return dto;
    }

    private ParticipantDTO convertToParticipantDTO(MatchTeam matchTeam) {
        ParticipantDTO dto = new ParticipantDTO();
        dto.setId(matchTeam.getTeam().getTeamId().toString());
        dto.setResultText(matchTeam.getScore() != null ? matchTeam.getScore().toString() : "0");
        dto.setStatus(matchTeam.getMatch().getStatus().toString());
        dto.setName(matchTeam.getTeam().getDepartment().getKoreanName());
        dto.setImage("userImage");
        dto.setSubScore(matchTeam.getSubScores());

        if (matchTeam.getMatch().getStatus() == MatchStatus.PAST) {
            Boolean isWinner = determineWinner(matchTeam);
            dto.setWinner(isWinner != null ? isWinner : false);
        } else {
            dto.setWinner(null);
        }

        return dto;
    }

    private Boolean determineWinner(MatchTeam matchTeam) {
        Match match = matchTeam.getMatch();
        if (match.getMatchTeams().size() != 2) {
            return null;  // 경기에 두 팀이 아니면 승자를 결정할 수 없음
        }

        MatchTeam opponent = match.getMatchTeams().stream()
                .filter(mt -> !mt.equals(matchTeam))
                .findFirst()
                .orElse(null);

        if (opponent == null) {
            return null;  // 상대팀을 찾을 수 없음
        }

        // 주 점수 비교
        if (matchTeam.getScore() > opponent.getScore()) {
            return true;
        } else if (matchTeam.getScore() < opponent.getScore()) {
            return false;
        }

        // 주 점수가 같은 경우 서브스코어 비교
        if (matchTeam.getSubScores() != null && opponent.getSubScores() != null) {
            int subScore1 = Integer.parseInt(matchTeam.getSubScores());
            int subScore2 = Integer.parseInt(opponent.getSubScores());
            return subScore1 > subScore2;
        }

        // 모든 점수가 같은 경우
        return null;  // 또는 다른 규칙을 적용할 수 있음 (예: 첫 번째 팀 승리)
    }

    @Transactional
    public List<TournamentMatchDTO> initializeTournament(String sport, TournamentRound startingRound) {
        List<TournamentRound> tournamentRounds = Arrays.asList(TournamentRound.values());
        int startIndex = tournamentRounds.indexOf(startingRound);
        if (startIndex == -1) {
            throw new IllegalArgumentException("Invalid starting round: " + startingRound);
        }
        List<TournamentRound> rounds = tournamentRounds.subList(startIndex, tournamentRounds.size());

        List<Match> tournamentMatches = new ArrayList<>();

        for (TournamentRound round : rounds) {
            int matchesInRound = getMatchesCountForRound(round);
            for (int i = 0; i < matchesInRound; i++) {
                Match match = new Match();
                match.setSport(sport);
                match.setMatchType(MatchType.TOURNAMENT);
                match.setRound(round.getDisplayName());
                match.setStatus(MatchStatus.FUTURE);
                match.setDate(LocalDate.now().plusDays(i));
                match.setStartTime(LocalTime.of(18, 0));
                tournamentMatches.add(match);
            }
        }

        List<Match> savedMatches = matchRepository.saveAll(tournamentMatches);
        List<TournamentMatchDTO> dtos = savedMatches.stream()
                .map(this::convertToTournamentMatchDTO)
                .collect(Collectors.toList());

        updateNextMatchIds(dtos);
        return dtos;
    }

    private MatchSummaryDTO.Response convertToMatchSummaryDTO(Match match) {
        MatchSummaryDTO.Response dto = new MatchSummaryDTO.Response();
        dto.setMatchId(match.getMatchId());
        dto.setSport(match.getSport());
        dto.setDate(match.getDate());
        dto.setStartTime(match.getStartTime());
        dto.setStatus(match.getStatus().toString());
        dto.setGroupName(match.getGroupName());
        dto.setRound(match.getRound());
        dto.setMatchType(match.getMatchType());

        List<MatchTeam> matchTeams = match.getMatchTeams();
        if (matchTeams.size() >= 2) {
            MatchTeam team1 = matchTeams.get(0);
            MatchTeam team2 = matchTeams.get(1);

            dto.setTeamName1(team1.getTeam().getDepartment().getKoreanName());
            dto.setTeamName2(team2.getTeam().getDepartment().getKoreanName());
            dto.setDepartment1(team1.getTeam().getDepartment().name());
            dto.setDepartment2(team2.getTeam().getDepartment().name());
            dto.setTeamScore1(team1.getScore());
            dto.setTeamScore2(team2.getScore());

            dto.setResult(determineMatchResult(team1.getScore(), team2.getScore(), match.getStatus()));
        }

        return dto;
    }

    private MatchResult determineMatchResult(int score1, int score2, MatchStatus status) {
        if (status == MatchStatus.FUTURE) {
            return MatchResult.NOT_PLAYED;
        } else if (status == MatchStatus.LIVE) {
            return MatchResult.IN_PROGRESS;
        } else {
            if (score1 > score2) {
                return MatchResult.TEAM_ONE_WIN;
            } else if (score1 < score2) {
                return MatchResult.TEAM_TWO_WIN;
            } else {
                return MatchResult.DRAW;
            }
        }
    }
}