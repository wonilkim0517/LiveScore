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
import jakarta.persistence.EntityNotFoundException;
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
        Map<String, TournamentMatchDTO> roundMap = new HashMap<>();
        for (TournamentMatchDTO match : matches) {
            String round = match.getTournamentRoundText();
            if (!roundMap.containsKey(round)) {
                roundMap.put(round, match);
            } else {
                // 같은 라운드의 다음 경기가 있다면, 둘 다 다음 라운드의 경기 ID를 가리키도록 함
                TournamentMatchDTO existingMatch = roundMap.get(round);
                String nextRound = getNextRound(round);
                TournamentMatchDTO nextMatch = roundMap.get(nextRound);
                if (nextMatch != null) {
                    existingMatch.setNextMatchId(nextMatch.getId());
                    match.setNextMatchId(nextMatch.getId());
                }
            }
        }
    }
    private String getNextRound(String currentRound) {
        // 라운드 순서를 정의. 필요에 따라 수정하세요.
        List<String> rounds = Arrays.asList("Round Quarter Final", "Round Semi Final", "Round Final");
        int currentIndex = rounds.indexOf(currentRound);
        if (currentIndex < rounds.size() - 1) {
            return rounds.get(currentIndex + 1);
        }
        return null;
    }


    private void setNextMatchId(List<TournamentMatchDTO> matches) {
        Map<String, TournamentMatchDTO> roundMap = new HashMap<>();
        for (TournamentMatchDTO match : matches) {
            roundMap.put(match.getTournamentRoundText(), match);
        }

        // 라운드 순서 정의 (예: Quarter Final -> Semi Final -> Final)
        List<String> roundOrder = Arrays.asList("Quarter Final", "Semi Final", "Final");

        for (int i = 0; i < roundOrder.size() - 1; i++) {
            String currentRound = roundOrder.get(i);
            String nextRound = roundOrder.get(i + 1);
            TournamentMatchDTO currentMatch = roundMap.get(currentRound);
            TournamentMatchDTO nextMatch = roundMap.get(nextRound);

            if (currentMatch != null && nextMatch != null) {
                currentMatch.setNextMatchId(nextMatch.getId());
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
        MatchTeam matchTeamTwo = new MatchTeam(match, teamTwo, bracketDTO.getTeamTwoScore());

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

//    private Map<String, List<GroupDTO>> groupMatchesByGroupName(List<Match> matches) {
//        Map<String, List<Match>> groupedMatches = matches.stream()
//                .collect(Collectors.groupingBy(Match::getGroupName));
//
//        Map<String, List<GroupDTO>> result = new HashMap<>();
//
//        for (Map.Entry<String, List<Match>> entry : groupedMatches.entrySet()) {
//            GroupDTO groupDTO = new GroupDTO();
//            groupDTO.setGroup(entry.getKey());
//            groupDTO.setTeams(calculateStandings(entry.getValue()));
//
//            result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(groupDTO);
//        }
//
//        return result;
//    }

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
        if (match.getStatus() != MatchStatus.PAST) {
            return false; // 경기가 끝나지 않았으면 승자가 없음
        }
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
        if (matchTeams != null && matchTeams.size() >= 2) {
            MatchTeam teamOne = matchTeams.get(0);
            MatchTeam teamTwo = matchTeams.get(1);

            bracketDTO.setTeamOneName(teamOne.getTeam().getDepartment());
            bracketDTO.setTeamTwoName(teamTwo.getTeam().getDepartment());
            bracketDTO.setTeamOneScore(teamOne.getScore());
            bracketDTO.setTeamTwoScore(teamTwo.getScore());
        }

        return bracketDTO;
    }
}