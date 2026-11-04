package com.cricket.stats.service;

import com.cricket.stats.dto.PartnershipRequestDTO;
import com.cricket.stats.entity.*;
import com.cricket.stats.repository.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PartnershipIntegrationTest {
    @Autowired PartnershipService partnerships;
    @Autowired MatchSquadRepository squads;
    @Autowired PlayerRepository players;
    @Autowired MatchRepository matches;
    @Autowired TeamRepository teams;
    @Autowired SeasonRepository seasons;

    Match match;
    Team team;
    Player batter, keeper, allrounder, fastBowler, spinner;

    @BeforeEach
    void setup() {
        team = new Team();
        team.setTeamName("Partnership test team");
        teams.save(team);
        Team opposition = new Team();
        opposition.setTeamName("Partnership test opposition");
        teams.save(opposition);
        Season season = new Season();
        season.setYear(290001);
        seasons.save(season);
        match = new Match();
        match.setTeamA(team);
        match.setTeamB(opposition);
        match.setSeason(season);
        match.setMatchDate(LocalDate.of(2026, 1, 1));
        match.setFormat(MatchFormat.T20);
        matches.save(match);

        // Insert in reverse role order, with names that also oppose role order.
        spinner = squadPlayer("A spinner", Roles.Baller, BowlingStyle.OffSpinner);
        fastBowler = squadPlayer("B pacer", Roles.Baller, BowlingStyle.FastBaller);
        allrounder = squadPlayer("C allrounder", Roles.Allrounder, BowlingStyle.LegSpinner);
        keeper = squadPlayer("D keeper", Roles.Keeper, null);
        batter = squadPlayer("Z batter", Roles.Batsman, null);
        squads.flush();
    }

    @Test
    void everyMatchSquadRetrievalOrdersRolesAndKeepsAllBowlingStylesLast() {
        List<Long> expected = List.of(batter.getId(), keeper.getId(), allrounder.getId(), spinner.getId(), fastBowler.getId());
        assertThat(squads.findSquadByMatchId(match.getMatchId())).extracting(s -> s.getPlayer().getId())
                .containsExactlyElementsOf(expected);
        assertThat(squads.findByMatchMatchId(match.getMatchId())).extracting(s -> s.getPlayer().getId())
                .containsExactlyElementsOf(expected);
        assertThat(squads.findByMatchMatchIdAndTeamTeamId(match.getMatchId(), team.getTeamId()))
                .extracting(s -> s.getPlayer().getId()).containsExactlyElementsOf(expected);
    }

    @Test
    void persistsCrudAndFiltersByInningsSeasonAndEitherPairOrientation() {
        var request = request(2, batter, keeper, 45);
        var first = partnerships.create(match.getMatchId(), request);
        partnerships.create(match.getMatchId(), request(1, batter, keeper, 20));
        partnerships.create(match.getMatchId(), request(1, batter, allrounder, 5));

        assertThat(partnerships.getForMatch(match.getMatchId(), 2)).hasSize(1);
        assertThat(partnerships.getForMatch(match.getMatchId(), null))
                .extracting(p -> p.getInnings()).containsExactly(1, 1, 2);
        var history = partnerships.search(match.getMatchId(), null, 290001,
                keeper.getId(), batter.getId(), 0, 1);
        assertThat(history.getTotalElements()).isEqualTo(2);
        assertThat(history.getContent()).hasSize(1);
        assertThat(partnerships.search(null, null, 290002, null, null, 0, 50)).isEmpty();

        request.setRuns(60);
        assertThat(partnerships.update(match.getMatchId(), first.getPartnershipId(), request).getRuns()).isEqualTo(60);
        assertThat(partnerships.getById(first.getPartnershipId()).getBatter1Name()).isEqualTo("Z batter");
        partnerships.delete(match.getMatchId(), first.getPartnershipId());
        assertThat(partnerships.getForMatch(match.getMatchId(), 2)).isEmpty();
    }

    @Test
    void aPairCanHaveSeparateStandsForTheSameWicket() {
        partnerships.create(match.getMatchId(), request(1, batter, keeper, 10));
        partnerships.create(match.getMatchId(), request(1, keeper, batter, 15));
        var result = partnerships.search(match.getMatchId(), 1, null, batter.getId(), keeper.getId(), 0, 50);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(p -> p.getRuns()).containsExactly(10, 15);
    }

    private Player squadPlayer(String name, Roles role, BowlingStyle bowlingStyle) {
        Player player = new Player();
        player.setFirstName(name);
        player.setLastName("Test");
        player.setFullName(name);
        player.setNationality("Pakistan");
        player.setRole(role);
        player.setBowlingStyle(bowlingStyle);
        players.save(player);
        MatchSquad squad = new MatchSquad();
        squad.setMatch(match);
        squad.setTeam(team);
        squad.setPlayer(player);
        squads.save(squad);
        return player;
    }

    private PartnershipRequestDTO request(int innings, Player first, Player second, int runs) {
        PartnershipRequestDTO request = new PartnershipRequestDTO();
        request.setInnings(innings);
        request.setBatter1Id(first.getId());
        request.setBatter2Id(second.getId());
        request.setRuns(runs);
        request.setBallsFaced(30);
        request.setWicketNumber(1);
        return request;
    }
}
