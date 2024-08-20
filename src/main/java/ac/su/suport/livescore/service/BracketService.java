package ac.su.suport.livescore.service;

import ac.su.suport.livescore.constant.DepartmentEnum;
import ac.su.suport.livescore.constant.MatchStatus;
import ac.su.suport.livescore.constant.MatchType;
import ac.su.suport.livescore.constant.TournamentRound;
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

    public List<TournamentMatchDTO> getSportTournamentBrackets(String sport) {
        List<Match> matches = matchRepository.findBySportAndMatchTypeOrderByRoundDesc(sport, MatchType.TOURNAMENT);
        List<TournamentMatchDTO> dtos = matches.stream()
                .map(this::convertToTournamentMatchDTO)
                .collect(Collectors.toList());

        updateNextMatchIds(dtos);
        return dtos;
    }

    private void updateNextMatchIds(List<TournamentMatchDTO> matches) {
        List<String> roundOrder = Arrays.stream(TournamentRound.values())
                .map(TournamentRound::getDisplayName)
                .collect(Collectors.toList());

        Map<String, List<TournamentMatchDTO>> roundMatches = matches.stream()
                .collect(Collectors.groupingBy(TournamentMatchDTO::getTournamentRoundText));

        for (int i = 0; i < roundOrder.size() - 1; i++) {
            String currentRound = roundOrder.get(i);
            String nextRound = roundOrder.get(i + 1);

            List<TournamentMatchDTO> currentMatches = roundMatches.get(currentRound);
            List<TournamentMatchDTO> nextMatches = roundMatches.get(nextRound);

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
    private void updateNextRoundMatch(Match currentMatch) {
        TournamentRound currentRound = TournamentRound.valueOf(currentMatch.getRound());
        TournamentRound nextRound = getNextRound(currentRound);

        if (nextRound == null) {
            return; // 현재 라운드가 결승전이면 다음 라운드가 없음
        }

        Match nextMatch = matchRepository.findByRoundAndSport(nextRound.name(), currentMatch.getSport())
                .orElseGet(() -> createNextRoundMatch(currentMatch, nextRound));

        MatchTeam winner = determineWinner(currentMatch);
        if (winner != null) {
            addParticipantToNextMatch(nextMatch, winner);
        }

        if (nextMatch.getMatchTeams().size() == 2) {
            nextMatch.setStatus(MatchStatus.FUTURE);
            matchRepository.save(nextMatch);
        }
    }
    private void addParticipantToNextMatch(Match nextMatch, MatchTeam winnerTeam) {
        MatchTeam newParticipant = new MatchTeam();
        newParticipant.setMatch(nextMatch);
        newParticipant.setTeam(winnerTeam.getTeam());
        newParticipant.setScore(winnerTeam.getScore());
        newParticipant.setSubScores(winnerTeam.getSubScores());
        nextMatch.getMatchTeams().add(newParticipant);
        matchTeamRepository.save(newParticipant);
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
            updateNextRoundMatch(match);
        }
        return convertToBracketDTO(match);
    }


    private Match createNextRoundMatch(Match currentMatch, TournamentRound nextRound) {
        Match nextMatch = new Match();
        nextMatch.setSport(currentMatch.getSport());
        nextMatch.setMatchType(MatchType.TOURNAMENT);
        nextMatch.setRound(nextRound.name());
        nextMatch.setStatus(MatchStatus.FUTURE);
        nextMatch.setDate(currentMatch.getDate().plusDays(7)); // 예시: 일주일 후
        nextMatch.setStartTime(currentMatch.getStartTime());
        return matchRepository.save(nextMatch);
    }

    private void addParticipantToNextMatch(Match nextMatch, Team team) {
        MatchTeam newParticipant = new MatchTeam();
        newParticipant.setMatch(nextMatch);
        newParticipant.setTeam(team);
        nextMatch.getMatchTeams().add(newParticipant);
        matchTeamRepository.save(newParticipant);
    }

    private MatchTeam determineWinner(Match match) {
        return match.getMatchTeams().stream()
                .max(Comparator.comparing(MatchTeam::getScore)
                        .thenComparing(mt -> mt.getSubScores() != null ?
                                Integer.parseInt(mt.getSubScores().split(":")[0]) : 0))
                .orElse(null);
    }

    private TournamentRound getNextRound(TournamentRound currentRound) {
        TournamentRound[] rounds = TournamentRound.values();
        int currentIndex = Arrays.asList(rounds).indexOf(currentRound);
        return (currentIndex < rounds.length - 1) ? rounds[currentIndex + 1] : null;
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
                // 동점일 경우 승부차기 점수 확인
                if (matchTeamOne.getSubScores() != null && matchTeamTwo.getSubScores() != null) {
                    int subScoreOne = Integer.parseInt(matchTeamOne.getSubScores().split(":")[0]);
                    int subScoreTwo = Integer.parseInt(matchTeamTwo.getSubScores().split(":")[0]);
                    if (subScoreOne > subScoreTwo) {
                        teamOne.setTeamPoint(teamOne.getTeamPoint() + 3);
                    } else if (subScoreOne < subScoreTwo) {
                        teamTwo.setTeamPoint(teamTwo.getTeamPoint() + 3);
                    } else {
                        teamOne.setTeamPoint(teamOne.getTeamPoint() + 1);
                        teamTwo.setTeamPoint(teamTwo.getTeamPoint() + 1);
                    }
                } else {
                    teamOne.setTeamPoint(teamOne.getTeamPoint() + 1);
                    teamTwo.setTeamPoint(teamTwo.getTeamPoint() + 1);
                }
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
            // 동점일 경우 승부차기 점수 확인
            if (teamOne.getSubScores() != null && teamTwo.getSubScores() != null) {
                int subScoreOne = Integer.parseInt(teamOne.getSubScores().split(":")[0]);
                int subScoreTwo = Integer.parseInt(teamTwo.getSubScores().split(":")[0]);
                if (subScoreOne > subScoreTwo) {
                    standingOne.setWin(standingOne.getWin() + 1);
                    standingTwo.setLose(standingTwo.getLose() + 1);
                } else if (subScoreOne < subScoreTwo) {
                    standingTwo.setWin(standingTwo.getWin() + 1);
                    standingOne.setLose(standingOne.getLose() + 1);
                } else {
                    standingOne.setDraw(standingOne.getDraw() + 1);
                    standingTwo.setDraw(standingTwo.getDraw() + 1);
                }
            } else {
                standingOne.setDraw(standingOne.getDraw() + 1);
                standingTwo.setDraw(standingTwo.getDraw() + 1);
            }
        }

        standingOne.updatePoints();
        standingTwo.updatePoints();
    }

    private TournamentMatchDTO convertToTournamentMatchDTO(Match match) {
        TournamentMatchDTO dto = new TournamentMatchDTO();
        dto.setId(match.getMatchId());
        dto.setName("Round " + match.getRound() + " - Match " + match.getMatchId());
        dto.setTournamentRoundText("Round " + match.getRound());
        dto.setStartTime(match.getDate().atTime(match.getStartTime()).toString());
        dto.setState(match.getStatus().toString());

        List<ParticipantDTO> participants = new ArrayList<>();
        for (MatchTeam matchTeam : match.getMatchTeams()) {
            ParticipantDTO participant = new ParticipantDTO();
            participant.setId(matchTeam.getTeam().getTeamId().toString());
            participant.setResultText(matchTeam.getScore() != null ? matchTeam.getScore().toString() : "");
            participant.setStatus(match.getStatus().toString());
            participant.setName(matchTeam.getTeam().getDepartment().name());  // 직접 한글 학과명 사용
            participant.setImage("userImage");
            participant.setIsWinner(determineWinner(match, matchTeam));
            if (match.getStatus() == MatchStatus.PAST) {
                participant.setResultText(matchTeam.getScore().toString());
            }
            participants.add(participant);
        }
        dto.setParticipants(participants);

        return dto;
    }

    private boolean determineWinner(Match match, MatchTeam matchTeam) {
        if (match.getStatus() != MatchStatus.PAST) {
            return false; // 경기가 끝나지 않았으면 승자가 없음
        }
        MatchTeam winner = determineWinner(match);
        return winner != null && winner.equals(matchTeam);
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
            bracketDTO.setShowSubScores(teamOne.getSubScores() != null && !teamOne.getSubScores().isEmpty() &&
                    teamTwo.getSubScores() != null && !teamTwo.getSubScores().isEmpty());
        }

        return bracketDTO;
    }
}