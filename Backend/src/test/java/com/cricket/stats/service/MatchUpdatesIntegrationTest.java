package com.cricket.stats.service;

import com.cricket.stats.dto.*;
import com.cricket.stats.entity.*;
import com.cricket.stats.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MatchUpdatesIntegrationTest {
    @Autowired MatchService matches;
    @Autowired TeamService teams;
    @Autowired PlayerService players;
    @Autowired SeasonService seasons;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @SpyBean ScorecardCalculator calculator;

    Integer matchId, seasonId, teamA, teamB;
    Long batter, bowler;
    final List<Integer> matchIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        TeamDTO team = new TeamDTO();
        team.setTeamName("Test A " + UUID.randomUUID());
        teamA = teams.createTeam(team).getTeamId();
        team.setTeamName("Test B " + UUID.randomUUID());
        teamB = teams.createTeam(team).getTeamId();
        SeasonDTO season = new SeasonDTO();
        season.setYear(3000 + (int) (Math.random() * 100000));
        seasonId = seasons.createSeason(season).getSeasonId();
        batter = createPlayer("Batter");
        bowler = createPlayer("Bowler");
        matchId = matches.createMatch(matchRequest()).getMatchId();
        matchIds.add(matchId);
    }

    @AfterEach
    void cleanup() {
        reset(calculator);
        for (Integer id : matchIds) {
            for (String table : List.of("deliveries", "scorecard_batters", "scorecard_bowlers", "match_squads")) {
                jdbc.update("DELETE FROM " + table + " WHERE match_id = ?", id);
            }
            jdbc.update("DELETE FROM matches WHERE match_id = ?", id);
        }
        if (batter != null) players.deletePlayer(batter);
        if (bowler != null) players.deletePlayer(bowler);
        if (seasonId != null) seasons.deleteSeason(seasonId);
        if (teamA != null) teams.deleteTeam(teamA);
        if (teamB != null) teams.deleteTeam(teamB);
        matchIds.clear();
    }

    @Test
    void matchUpdateCanClearOptionalRelationsAndReadsReturnTheirIds() {
        MatchRequestDTO request = matchRequest();
        request.setWinnerId(teamA);
        request.setTossWinnerId(teamB);
        request.setPlayerOfMatchId(batter);
        MatchDTO saved = matches.updateMatch(matchId, request);
        assertThat(saved.getWinnerId()).isEqualTo(teamA);
        assertThat(saved.getTossWinnerId()).isEqualTo(teamB);
        assertThat(saved.getSeasonId()).isEqualTo(seasonId);
        MatchDTO cleared = matches.updateMatch(matchId, matchRequest());
        assertThat(cleared.getWinnerId()).isNull();
        assertThat(cleared.getTossWinnerId()).isNull();
        assertThat(cleared.getPlayerOfMatchId()).isNull();
    }

    @Test
    void deliveriesRebuildScorecardsOnCreateUpdateAndDelete() {
        DeliveryRequestDTO request = delivery(1);
        request.setRunsBatter(4);
        DeliveryDTO saved = matches.addDelivery(matchId, request);
        assertThat(saved.getBatterId()).isEqualTo(batter);
        assertThat(saved.getBowlerId()).isEqualTo(bowler);
        assertThat(matches.getBattingScorecard(matchId).get(0).getRuns()).isEqualTo(4);
        assertThat(matches.getBowlingScorecard(matchId).get(0).getMaidens()).isZero();

        request.setRunsBatter(1);
        matches.updateDelivery(matchId, saved.getDeliveryId(), request);
        assertThat(matches.getBattingScorecard(matchId).get(0).getRuns()).isEqualTo(1);
        assertThat(matches.getBowlingScorecard(matchId).get(0).getRunsConceded()).isEqualTo(1);

        matches.deleteDelivery(matchId, saved.getDeliveryId());
        assertThat(matches.getDeliveries(matchId)).isEmpty();
        assertThat(matches.getBattingScorecard(matchId)).isEmpty();
        assertThat(matches.getBowlingScorecard(matchId)).isEmpty();
    }

    @Test
    void scorecardFailureRollsBackDeliveryAndPreservesPreviousScorecards() {
        DeliveryDTO first = matches.addDelivery(matchId, delivery(1));
        doThrow(new IllegalStateException("Simulated calculator failure"))
                .when(calculator).bowling(any(Match.class), anyList());
        assertThatThrownBy(() -> matches.addDelivery(matchId, delivery(2)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(matches.getDeliveries(matchId)).extracting(DeliveryDTO::getDeliveryId)
                .containsExactly(first.getDeliveryId());
        assertThat(matches.getBattingScorecard(matchId)).hasSize(1);
        assertThat(matches.getBowlingScorecard(matchId)).hasSize(1);
    }

    @Test
    void cannotEditOrDeleteAnotherMatchesDelivery() {
        Integer other = matches.createMatch(matchRequest()).getMatchId();
        matchIds.add(other);
        DeliveryDTO saved = matches.addDelivery(matchId, delivery(1));
        assertThatThrownBy(() -> matches.updateDelivery(other, saved.getDeliveryId(), delivery(1)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> matches.deleteDelivery(other, saved.getDeliveryId()))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(matches.getDeliveries(matchId)).hasSize(1);
    }

    @Test
    void rejectsIncompleteDeliveryRequestsAndRestoresExtrasEndpoint() throws Exception {
        mvc.perform(post("/matches/" + matchId + "/deliveries")
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        assertThat(matches.getDeliveries(matchId)).isEmpty();
        DeliveryRequestDTO request = delivery(1);
        request.setExtraType("wides");
        request.setRunsExtras(2);
        matches.addDelivery(matchId, request);
        mvc.perform(get("/matches/" + matchId + "/extras"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].totalExtras").value(2));
    }

    @Test
    void squadCreateRejectsDuplicatesAndUpdateUsesPathPlayerId() {
        MatchSquadRequestDTO request = new MatchSquadRequestDTO();
        request.setPlayerId(batter);
        request.setTeamId(teamA);
        assertThat(matches.addSquadMember(matchId, request).getIsPlaying()).isTrue();
        assertThatThrownBy(() -> matches.addSquadMember(matchId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        request.setPlayerId(null);
        request.setIsCaptain(true);
        assertThat(matches.updateSquadMember(matchId, batter, request).getIsCaptain()).isTrue();
        matches.removeSquadMember(matchId, batter);
        assertThat(matches.getMatchSquad(matchId)).isEmpty();
    }

    private Long createPlayer(String name) {
        PlayerDTO player = new PlayerDTO();
        player.setFirstName("Test");
        player.setLastName(name);
        player.setNationality("Pakistan");
        player.setAge(25);
        player.setRole(Roles.Batsman);
        return players.createPlayer(player).getId();
    }

    private MatchRequestDTO matchRequest() {
        MatchRequestDTO request = new MatchRequestDTO();
        request.setSeasonId(seasonId);
        request.setTeamAId(teamA);
        request.setTeamBId(teamB);
        request.setVenue("Test ground");
        request.setMatchDate(LocalDate.of(2026, 1, 1));
        request.setFormat(MatchFormat.T20);
        return request;
    }

    private DeliveryRequestDTO delivery(int ball) {
        DeliveryRequestDTO request = new DeliveryRequestDTO();
        request.setInnings(1);
        request.setOverNum(0);
        request.setBallNum(ball);
        request.setBatterId(batter);
        request.setBowlerId(bowler);
        request.setRunsBatter(0);
        request.setRunsExtras(0);
        return request;
    }
}
