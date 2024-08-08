package ac.su.suport.livescore.service;

import ac.su.suport.livescore.constant.MatchResult;
import ac.su.suport.livescore.constant.MatchStatus;
import ac.su.suport.livescore.constant.MatchType;
import ac.su.suport.livescore.domain.Match;
import ac.su.suport.livescore.domain.MatchTeam;
import ac.su.suport.livescore.domain.Team;
import ac.su.suport.livescore.dto.BracketDTO;
import ac.su.suport.livescore.dto.GroupDTO;
import ac.su.suport.livescore.dto.TeamStandingDTO;
import ac.su.suport.livescore.repository.MatchRepository;
import ac.su.suport.livescore.repository.MatchTeamRepository;
import ac.su.suport.livescore.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BracketService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final MatchTeamRepository matchTeamRepository;

    public List<GroupDTO> getSportLeagueBrackets(String sport) {
        List<Match> matches = matchRepository.findBySportAndMatchType(sport, MatchType.LEAGUE);
        return groupMatchesByGroupName(matches);
    }

    public List<BracketDTO> getSportTournamentBrackets(String sport) {
        List<Match> matches = matchRepository.findBySportAndMatchType(sport, MatchType.TOURNAMENT);
        return matches.stream()
                .map(this::convertToBracketDTO)
                .collect(Collectors.toList());
    }

    private BracketDTO convertToBracketDTO(Match match) {
        BracketDTO bracketDTO = new BracketDTO();
        bracketDTO.setMatchId(match.getMatchId());
        bracketDTO.setSports(match.getSport());
        bracketDTO.setMatchDate(match.getDate());
        bracketDTO.setStartTime(match.getStartTime());
        bracketDTO.setMatchType(match.getMatchType());
        bracketDTO.setMatchStatus(match.getStatus());
        bracketDTO.setRound(match.getRound());

        List<MatchTeam> matchTeams = match.getMatchTeams();
        if (matchTeams.size() >= 2) {
            MatchTeam matchTeamOne = matchTeams.get(0);
            MatchTeam matchTeamTwo = matchTeams.get(1);

            bracketDTO.setTeamOneId(matchTeamOne.getTeam().getTeamId());
            bracketDTO.setTeamTwoId(matchTeamTwo.getTeam().getTeamId());
            bracketDTO.setTeamOneName(matchTeamOne.getTeam().getTeamName());
            bracketDTO.setTeamTwoName(matchTeamTwo.getTeam().getTeamName());
            bracketDTO.setTeamOneScore(matchTeamOne.getScore());
            bracketDTO.setTeamTwoScore(matchTeamTwo.getScore());
        }

        return bracketDTO;
    }

    public BracketDTO createLeagueBracket(BracketDTO bracketDTO) {
        Match match = new Match();
        match.setSport(bracketDTO.getSports());
        match.setDate(bracketDTO.getMatchDate());
        match.setStartTime(bracketDTO.getStartTime());
        match.setMatchType(MatchType.LEAGUE);
        match.setStatus(MatchStatus.FUTURE);
        match.setGroupName(bracketDTO.getGroupName());
        match.setRound(bracketDTO.getRound());

        matchRepository.save(match);

        Team teamOne = teamRepository.findById(bracketDTO.getTeamOneId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + bracketDTO.getTeamOneId()));
        Team teamTwo = teamRepository.findById(bracketDTO.getTeamTwoId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + bracketDTO.getTeamTwoId()));

        MatchTeam matchTeamOne = new MatchTeam();
        matchTeamOne.setMatch(match);
        matchTeamOne.setTeam(teamOne);
        matchTeamOne.setScore(0);

        MatchTeam matchTeamTwo = new MatchTeam();
        matchTeamTwo.setMatch(match);
        matchTeamTwo.setTeam(teamTwo);
        matchTeamTwo.setScore(0);

        matchTeamRepository.save(matchTeamOne);
        matchTeamRepository.save(matchTeamTwo);

        bracketDTO.setMatchId(match.getMatchId());
        // 클라이언트가 제공한 팀 이름을 사용
        bracketDTO.setTeamOneName(bracketDTO.getTeamOneName());
        bracketDTO.setTeamTwoName(bracketDTO.getTeamTwoName());

        return bracketDTO;
    }

    public BracketDTO createTournamentBracket(BracketDTO bracketDTO) {
        Match match = new Match();
        match.setSport(bracketDTO.getSports());
        match.setDate(bracketDTO.getMatchDate());
        match.setStartTime(bracketDTO.getStartTime());
        match.setMatchType(MatchType.TOURNAMENT);
        match.setStatus(MatchStatus.FUTURE);
        match.setRound(bracketDTO.getRound());

        matchRepository.save(match);

        Team teamOne = teamRepository.findById(bracketDTO.getTeamOneId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + bracketDTO.getTeamOneId()));
        Team teamTwo = teamRepository.findById(bracketDTO.getTeamTwoId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + bracketDTO.getTeamTwoId()));

        MatchTeam matchTeamOne = new MatchTeam();
        matchTeamOne.setMatch(match);
        matchTeamOne.setTeam(teamOne);
        matchTeamOne.setScore(0);

        MatchTeam matchTeamTwo = new MatchTeam();
        matchTeamTwo.setMatch(match);
        matchTeamTwo.setTeam(teamTwo);
        matchTeamTwo.setScore(0);

        matchTeamRepository.save(matchTeamOne);
        matchTeamRepository.save(matchTeamTwo);

        bracketDTO.setMatchId(match.getMatchId());
        bracketDTO.setTeamOneName(teamOne.getTeamName());
        bracketDTO.setTeamTwoName(teamTwo.getTeamName());
        bracketDTO.setMatchStatus(MatchStatus.FUTURE);
        bracketDTO.setMatchType(MatchType.TOURNAMENT);

        return bracketDTO;
    }

    public BracketDTO updateLeagueBracket(Long id, BracketDTO bracketDTO) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));

        match.setSport(bracketDTO.getSports());
        match.setDate(bracketDTO.getMatchDate());
        match.setStartTime(bracketDTO.getStartTime());
        match.setGroupName(bracketDTO.getGroupName());
        match.setStatus(bracketDTO.getMatchStatus());

        List<MatchTeam> matchTeams = match.getMatchTeams();
        MatchResult result = null;
        int teamOnePoints = 0;
        int teamTwoPoints = 0;

        if (matchTeams.size() >= 2) {
            MatchTeam matchTeamOne = matchTeams.get(0);
            MatchTeam matchTeamTwo = matchTeams.get(1);

            matchTeamOne.setScore(bracketDTO.getTeamOneScore());
            matchTeamTwo.setScore(bracketDTO.getTeamTwoScore());

            Team teamOne = matchTeamOne.getTeam();
            Team teamTwo = matchTeamTwo.getTeam();

            // 경기 결과 결정
            if (bracketDTO.getTeamOneScore() > bracketDTO.getTeamTwoScore()) {
                result = MatchResult.TEAM_ONE_WIN;
                teamOnePoints = 3;
                teamTwoPoints = 0;
            } else if (bracketDTO.getTeamOneScore() < bracketDTO.getTeamTwoScore()) {
                result = MatchResult.TEAM_TWO_WIN;
                teamOnePoints = 0;
                teamTwoPoints = 3;
            } else {
                result = MatchResult.DRAW;
                teamOnePoints = 1;
                teamTwoPoints = 1;
            }

            // 팀의 누적 포인트 업데이트
            updateTeamPoints(teamOne, teamOnePoints);
            updateTeamPoints(teamTwo, teamTwoPoints);

            matchTeamRepository.save(matchTeamOne);
            matchTeamRepository.save(matchTeamTwo);
        }

        Match updatedMatch = matchRepository.save(match);

        BracketDTO updatedBracketDTO = new BracketDTO();
        updatedBracketDTO.setMatchId(updatedMatch.getMatchId());
        updatedBracketDTO.setSports(updatedMatch.getSport());
        updatedBracketDTO.setMatchDate(updatedMatch.getDate());
        updatedBracketDTO.setStartTime(updatedMatch.getStartTime());
        updatedBracketDTO.setGroupName(updatedMatch.getGroupName());
        updatedBracketDTO.setMatchStatus(updatedMatch.getStatus());
        updatedBracketDTO.setMatchType(updatedMatch.getMatchType());
        updatedBracketDTO.setRound(updatedMatch.getRound());
        updatedBracketDTO.setMatchResult(result);

        if (matchTeams.size() >= 2) {
            MatchTeam matchTeamOne = matchTeams.get(0);
            MatchTeam matchTeamTwo = matchTeams.get(1);

            updatedBracketDTO.setTeamOneId(matchTeamOne.getTeam().getTeamId());
            updatedBracketDTO.setTeamTwoId(matchTeamTwo.getTeam().getTeamId());
            updatedBracketDTO.setTeamOneName(matchTeamOne.getTeam().getTeamName());
            updatedBracketDTO.setTeamTwoName(matchTeamTwo.getTeam().getTeamName());
            updatedBracketDTO.setTeamOneScore(matchTeamOne.getScore());
            updatedBracketDTO.setTeamTwoScore(matchTeamTwo.getScore());
            updatedBracketDTO.setTeamOnePoints(teamOnePoints);  // 이 경기에서 얻은 포인트
            updatedBracketDTO.setTeamTwoPoints(teamTwoPoints);  // 이 경기에서 얻은 포인트
        }

        return updatedBracketDTO;
    }

    private void updateTeamPoints(Team team, int pointsToAdd) {
        team.setTeamPoint(team.getTeamPoint() + pointsToAdd);
        teamRepository.save(team);
    }

    private BracketDTO convertToBracketDTO(Match match, MatchResult result) {
        BracketDTO bracketDTO = new BracketDTO();
        bracketDTO.setMatchId(match.getMatchId());
        bracketDTO.setSports(match.getSport());
        bracketDTO.setMatchDate(match.getDate());
        bracketDTO.setStartTime(match.getStartTime());
        bracketDTO.setMatchType(match.getMatchType());
        bracketDTO.setMatchStatus(match.getStatus());
        bracketDTO.setRound(match.getRound());
        bracketDTO.setMatchResult(result);

        List<MatchTeam> matchTeams = match.getMatchTeams();
        if (matchTeams.size() >= 2) {
            MatchTeam matchTeamOne = matchTeams.get(0);
            MatchTeam matchTeamTwo = matchTeams.get(1);

            bracketDTO.setTeamOneId(matchTeamOne.getTeam().getTeamId());
            bracketDTO.setTeamTwoId(matchTeamTwo.getTeam().getTeamId());
            bracketDTO.setTeamOneName(matchTeamOne.getTeam().getTeamName());
            bracketDTO.setTeamTwoName(matchTeamTwo.getTeam().getTeamName());
            bracketDTO.setTeamOneScore(matchTeamOne.getScore());
            bracketDTO.setTeamTwoScore(matchTeamTwo.getScore());
        }

        return bracketDTO;
    }



    public void deleteLeagueBracket(Long id) {
        Match match = matchRepository.findById(id).orElseThrow();

        // 먼저 MatchTeam 엔티티 삭제
        matchTeamRepository.deleteAll(match.getMatchTeams());

        // Match 엔티티 삭제
        matchRepository.delete(match);
    }

    public BracketDTO updateTournamentBracket(Long id, BracketDTO bracketDTO) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + id));

        match.setSport(bracketDTO.getSports());
        match.setDate(bracketDTO.getMatchDate());
        match.setStartTime(bracketDTO.getStartTime());
        match.setRound(bracketDTO.getRound());
        match.setStatus(bracketDTO.getMatchStatus());

        List<MatchTeam> matchTeams = match.getMatchTeams();
        MatchResult result = null;

        if (matchTeams.size() >= 2) {
            MatchTeam matchTeamOne = matchTeams.get(0);
            MatchTeam matchTeamTwo = matchTeams.get(1);

            matchTeamOne.setScore(bracketDTO.getTeamOneScore());
            matchTeamTwo.setScore(bracketDTO.getTeamTwoScore());

            // 경기 결과 결정
            if (bracketDTO.getTeamOneScore() > bracketDTO.getTeamTwoScore()) {
                result = MatchResult.TEAM_ONE_WIN;
            } else if (bracketDTO.getTeamOneScore() < bracketDTO.getTeamTwoScore()) {
                result = MatchResult.TEAM_TWO_WIN;
            } else {
                result = MatchResult.DRAW;
            }

            matchTeamRepository.save(matchTeamOne);
            matchTeamRepository.save(matchTeamTwo);
        }

        Match updatedMatch = matchRepository.save(match);

        return convertToBracketDTO(updatedMatch, result);
    }

    public void deleteTournamentBracket(Long id) {
        Match match = matchRepository.findById(id).orElseThrow();

        // 먼저 MatchTeam 엔티티 삭제
        matchTeamRepository.deleteAll(match.getMatchTeams());

        // Match 엔티티 삭제
        matchRepository.delete(match);
    }

    private void updateMatchTeams(Match match, BracketDTO bracketDTO) {
        List<MatchTeam> matchTeams = match.getMatchTeams();

        if (matchTeams.size() >= 2) {
            MatchTeam matchTeamOne = matchTeams.get(0);
            MatchTeam matchTeamTwo = matchTeams.get(1);

            Team teamOne = teamRepository.findById(bracketDTO.getTeamOneId()).orElseThrow();
            Team teamTwo = teamRepository.findById(bracketDTO.getTeamTwoId()).orElseThrow();

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

            // 경기 결과를 업데이트
            if (matchTeamOne.getScore() > matchTeamTwo.getScore()) {
                // Team one wins
                teamOne.setTeamPoint(teamOne.getTeamPoint() + 3);
            } else if (matchTeamOne.getScore() < matchTeamTwo.getScore()) {
                // Team two wins
                teamTwo.setTeamPoint(teamTwo.getTeamPoint() + 3);
            } else {
                // Draw
                teamOne.setTeamPoint(teamOne.getTeamPoint() + 1);
                teamTwo.setTeamPoint(teamTwo.getTeamPoint() + 1);
            }

            teamRepository.save(teamOne);
            teamRepository.save(teamTwo);
        }
    }

    private List<GroupDTO> groupMatchesByGroupName(List<Match> matches) {
        Map<String, List<Match>> groupedMatches = matches.stream()
                .collect(Collectors.groupingBy(Match::getGroupName));

        List<GroupDTO> groupDTOs = new ArrayList<>();

        for (Map.Entry<String, List<Match>> entry : groupedMatches.entrySet()) {
            GroupDTO groupDTO = new GroupDTO();
            groupDTO.setGroupName(entry.getKey());
            groupDTO.setMatches(entry.getValue().stream()
                    .map(this::convertToBracketDTO)
                    .collect(Collectors.toList()));
            groupDTO.setStandings(calculateStandings(entry.getValue().stream()
                    .filter(match -> match.getStatus() == MatchStatus.PAST)
                    .collect(Collectors.toList())));
            groupDTOs.add(groupDTO);
        }

        groupDTOs.sort(Comparator.comparing(GroupDTO::getGroupName));

        return groupDTOs;
    }



    private List<TeamStandingDTO> calculateStandings(List<Match> matches) {
        Map<Long, TeamStandingDTO> standings = new HashMap<>();

        for (Match match : matches) {
            List<MatchTeam> matchTeams = match.getMatchTeams();
            if (matchTeams.size() >= 2) {
                Team teamOne = matchTeams.get(0).getTeam();
                Team teamTwo = matchTeams.get(1).getTeam();

                standings.putIfAbsent(teamOne.getTeamId(), new TeamStandingDTO(teamOne.getTeamName()));
                standings.putIfAbsent(teamTwo.getTeamId(), new TeamStandingDTO(teamTwo.getTeamName()));

                TeamStandingDTO teamOneStanding = standings.get(teamOne.getTeamId());
                TeamStandingDTO teamTwoStanding = standings.get(teamTwo.getTeamId());

                teamOneStanding.incrementPlayed();
                teamTwoStanding.incrementPlayed();

                if (matchTeams.get(0).getScore() > matchTeams.get(1).getScore()) {
                    teamOneStanding.incrementWon();
                    teamTwoStanding.incrementLost();
                    teamOneStanding.addPoints(3);
                } else if (matchTeams.get(0).getScore() < matchTeams.get(1).getScore()) {
                    teamTwoStanding.incrementWon();
                    teamOneStanding.incrementLost();
                    teamTwoStanding.addPoints(3);
                } else {
                    teamOneStanding.incrementDrawn();
                    teamTwoStanding.incrementDrawn();
                    teamOneStanding.addPoints(1);
                    teamTwoStanding.addPoints(1);
                }
            }
        }

        return new ArrayList<>(standings.values()).stream()
                .sorted(Comparator.comparingInt(TeamStandingDTO::getPoints).reversed())
                .collect(Collectors.toList());
    }
}
