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
                createNewMatch(nextRound, sport, winner1, winner2);
            }
        }
    }

    private ParticipantDTO getWinner(TournamentMatchDTO match) {
        return match.getParticipants().stream()
                .filter(ParticipantDTO::isWinner)
                .findFirst()
                .orElse(null);
    }

    private void createNewMatch(TournamentRound round, String sport, ParticipantDTO participant1, ParticipantDTO participant2) {
        Match newMatch = new Match();
        newMatch.setSport(sport);
        newMatch.setMatchType(MatchType.TOURNAMENT);
        newMatch.setRound(round.name());
        newMatch.setStatus(MatchStatus.FUTURE);
        newMatch.setDate(LocalDate.now().plusDays(7)); // 예시: 일주일 후로 설정
        newMatch.setStartTime(LocalTime.of(18, 0));

        newMatch = matchRepository.save(newMatch);

        createMatchTeam(newMatch, participant1);
        createMatchTeam(newMatch, participant2);
    }

    private void createMatchTeam(Match match, ParticipantDTO participant) {
        Team team = teamRepository.findById(Long.parseLong(participant.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        MatchTeam matchTeam = new MatchTeam();
        matchTeam.setMatch(match);
        matchTeam.setTeam(team);
        matchTeam.setScore(0);

        matchTeamRepository.save(matchTeam);
    }

    // 새로 추가된 초기화 메서드
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
                match.setDate(LocalDate.now().plusDays(i)); // 예시: 각 경기마다 하루씩 늦춤
                match.setStartTime(LocalTime.of(18, 0)); // 예시: 모든 경기 18:00 시작
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

    private int getMatchesCountForRound(TournamentRound round) {
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

}@Transactional
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
        dto.setResultText(matchTeam.getScore() != null ? matchTeam.getScore().toString() : "");
        dto.setStatus(matchTeam.getMatch().getStatus().toString());
        dto.setName(convertToKoreanDepartmentName(matchTeam.getTeam().getDepartment().name()));
        dto.setImage("userImage");
        dto.setIsWinner(determineWinner(matchTeam));
        return dto;
    }

    private String convertToKoreanDepartmentName(String englishName) {
        Map<String, String> departmentNameMap = new HashMap<>();
        departmentNameMap.put("THEOLOGY", "신학과");
        departmentNameMap.put("NURSING", "간호학과");
        departmentNameMap.put("PHARMACY", "약학과");
        departmentNameMap.put("EARLY_CHILDHOOD_EDUCATION", "유아교육과");
        departmentNameMap.put("MUSIC", "음악학과");
        departmentNameMap.put("ART_AND_DESIGN", "미술디자인학과");
        departmentNameMap.put("PHYSICAL_EDUCATION", "체육학과");
        departmentNameMap.put("SOCIAL_WELFARE", "사회복지학과");
        departmentNameMap.put("COUNSELING_PSYCHOLOGY", "상담심리학과");
        departmentNameMap.put("ENGLISH_LITERATURE", "영어영문학과");
        departmentNameMap.put("AVIATION_TOURISM_FOREIGN_LANGUAGES", "항공관광외국어학과");
        departmentNameMap.put("GLOBAL_KOREAN_STUDIES", "글로벌한국학과");
        departmentNameMap.put("BUSINESS_ADMINISTRATION", "경영학과");
        departmentNameMap.put("COMPUTER_SCIENCE", "컴퓨터공학과");
        departmentNameMap.put("AI_CONVERGENCE", "AI융합학과");
        departmentNameMap.put("FOOD_NUTRITION", "식품영양학과");
        departmentNameMap.put("HEALTH_MANAGEMENT", "보건관리학과");
        departmentNameMap.put("ENVIRONMENTAL_DESIGN_HORTICULTURE", "환경디자인원예학과");
        departmentNameMap.put("ANIMAL_RESOURCE_SCIENCE", "동물자원학과");
        departmentNameMap.put("CHEMISTRY_LIFE_SCIENCE", "화학생명과학과");
        departmentNameMap.put("BIO_CONVERGENCE_ENGINEERING", "바이오융합공학과");
        departmentNameMap.put("ARCHITECTURE", "건축학과");
        departmentNameMap.put("PHYSICAL_THERAPY", "물리치료학과");
        departmentNameMap.put("DATA_CLOUD_ENGINEERING", "데이터클라우드공학과");
        departmentNameMap.put("FACULTY_TEAM", "교직원팀");

        return departmentNameMap.getOrDefault(englishName, englishName);
    }

    private boolean determineWinner(MatchTeam matchTeam) {
        if (matchTeam.getMatch().getStatus() != MatchStatus.PAST) {
            return false;
        }
        return matchTeam.getMatch().getMatchTeams().stream()
                .max(Comparator.comparing(MatchTeam::getScore))
                .map(winner -> winner.equals(matchTeam))
                .orElse(false);
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
            bracketDTO.setTeamOneSubScores(teamOne.getSubScores());
            bracketDTO.setTeamTwoSubScores(teamTwo.getSubScores());

            // 서브스코어 표시 여부 설정
            bracketDTO.setShowSubScores(match.getSport().equals("SOCCER") ?
                    (teamOne.getSubScores() != null && !teamOne.getSubScores().isEmpty()) : true);
        }

        return bracketDTO;
    }
}