package com.cricket.stats.service;

import com.cricket.stats.dto.*;
import com.cricket.stats.entity.*;
import com.cricket.stats.repository.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

/** Exercises the actual MySQL aggregation, including history outside selected teams. */
@SpringBootTest
@Transactional
class MatchupIntegrationTest {
    @Autowired MatchupService service;
    @Autowired PlayerRepository players;
    @Autowired TeamRepository teams;
    @Autowired MatchRepository matches;
    @Autowired SeasonRepository seasons;
    @Autowired MatchSquadRepository squads;
    @Autowired DeliveryRepository deliveries;
    @Autowired EntityManager em;
    Player batter, bowler, other;
    Team battingTeam, bowlingTeam;
    Match first, second, rosterMatch;

    @BeforeEach
    void setup() {
        battingTeam = team("Current batting team");
        bowlingTeam = team("Current bowling team");
        Team oldA = team("Earlier A");
        Team oldB = team("Earlier B");
        batter = player("Batter A", Roles.Batsman, null);
        bowler = player("Bowler B", Roles.Baller, BowlingStyle.FastMedium);
        other = player("Other C", Roles.Keeper, null);
        Season oldSeason = season(2024);
        Season newSeason = season(2025);
        // A season is assigned explicitly; it must not be inferred from the match-date year.
        first = match(oldSeason, "2025-01-01", "Ground One", oldA, oldB);
        second = match(newSeason, "2025-02-01", "Ground Two", oldA, oldB);
        rosterMatch = match(newSeason, "2025-03-15", "Ground One", battingTeam, bowlingTeam);
        member(rosterMatch, batter, battingTeam);
        member(rosterMatch, bowler, bowlingTeam);
        member(rosterMatch, other, bowlingTeam);
        em.flush();
    }

    @Test
    void calculatesTheRequestedThirtyBallExample() {
        for (int i = 0; i < 30; i++) {
            Delivery d = delivery(first, batter, bowler, i < 3 ? 6 : i == 3 ? 4 : i < 27 ? 1 : 0, 0, null);
            if (i >= 28) dismiss(d, i == 28 ? "bowled" : "caught", batter);
        }
        em.flush();
        var stats = pair().stats();
        assertThat(stats.runs()).isEqualTo(45);
        assertThat(stats.ballsFaced()).isEqualTo(30);
        assertThat(stats.legalBalls()).isEqualTo(30);
        assertThat(stats.fours()).isEqualTo(1);
        assertThat(stats.sixes()).isEqualTo(3);
        assertThat(stats.dismissals()).isEqualTo(2);
        assertThat(stats.strikeRate()).isEqualByComparingTo("150.00");
        assertThat(stats.average()).isEqualByComparingTo("22.50");
    }

    @Test
    void extrasAndRunOutsDoNotInflateBatterRunsOrBowlerWickets() {
        delivery(first, batter, bowler, 0, 0, null);
        delivery(first, batter, bowler, 0, 2, "byes");
        delivery(first, batter, bowler, 0, 4, "LEG_BYES");
        delivery(first, batter, bowler, 6, 1, "no-ball");
        delivery(first, batter, bowler, 0, 2, "wides");
        dismiss(delivery(first, batter, bowler, 0, 1, "wide"), "stumped", batter);
        dismiss(delivery(first, batter, bowler, 0, 0, null), "run out", other);
        dismiss(delivery(first, batter, bowler, 1, 0, null), "RUN_OUT", batter);
        dismiss(delivery(first, batter, bowler, 0, 1, "No Ball"), "bowled", batter);
        dismiss(delivery(first, batter, bowler, 0, 0, null), "retired hurt", batter);
        dismiss(delivery(first, batter, bowler, 0, 0, null), "hit wicket", batter);
        em.flush();
        var stats = pair().stats();
        assertThat(stats.deliveries()).isEqualTo(11);
        assertThat(stats.runs()).isEqualTo(7);
        assertThat(stats.ballsFaced()).isEqualTo(9);
        assertThat(stats.legalBalls()).isEqualTo(7);
        assertThat(stats.dotBalls()).isEqualTo(4);
        assertThat(stats.sixes()).isEqualTo(1);
        assertThat(stats.dismissals()).isEqualTo(2);
        assertThat(stats.runOuts()).isEqualTo(1);
        assertThat(stats.runsConceded()).isEqualTo(12);
        assertThat(stats.strikeRate()).isEqualByComparingTo("77.78");
        assertThat(stats.average()).isEqualByComparingTo("3.50");
        assertThat(stats.economy()).isEqualByComparingTo("10.29");
    }

    @Test
    void noEncounterIsDifferentFromARecordedWideOrDotAndUndefinedRatesAreNull() {
        assertThat(pair().stats().deliveries()).isZero();
        assertThat(pair().stats().average()).isNull();
        delivery(first, batter, bowler, 0, 1, "wides");
        em.flush();
        assertThat(pair().stats().deliveries()).isEqualTo(1);
        assertThat(pair().stats().ballsFaced()).isZero();
        assertThat(pair().stats().strikeRate()).isNull();
        assertThat(pair().stats().economy()).isNull();
        delivery(first, batter, bowler, 0, 0, null);
        em.flush();
        assertThat(pair().stats().strikeRate()).isEqualByComparingTo("0.00");
        assertThat(pair().stats().average()).isNull();
    }

    @Test
    void appliesAssignedSeasonAndInclusiveDatesTogetherAcrossEveryTeam() {
        sample();
        assertThat(pair().stats().runs()).isEqualTo(11);
        MatchupFilterDTO filter = new MatchupFilterDTO();
        filter.setFromDate(LocalDate.parse("2025-01-01"));
        filter.setToDate(LocalDate.parse("2025-02-01"));
        assertThat(pair(filter).stats().runs()).isEqualTo(11);
        filter.setFromDate(LocalDate.parse("2025-02-01"));
        assertThat(pair(filter).stats().runs()).isEqualTo(7);
        filter.setSeasonYear(2024);
        assertThat(pair(filter).stats().deliveries()).isZero();
        filter.setFromDate(null);
        filter.setToDate(null);
        assertThat(pair(filter).stats().runs()).isEqualTo(4);
        filter.setSeasonYear(null);
        filter.setToDate(LocalDate.parse("2025-01-01"));
        assertThat(pair(filter).stats().runs()).isEqualTo(4);
        filter.setToDate(null);
        filter.setFromDate(LocalDate.parse("2025-02-01"));
        assertThat(pair(filter).stats().runs()).isEqualTo(7);
    }

    @Test
    void matrixUsesTheSamePairHistoryEvenWhenTeamsHaveNeverFacedEachOther() {
        sample();
        var filter = new MatchupFilterDTO();
        var matrix = service.matrix(battingTeam.getTeamId(), bowlingTeam.getTeamId(), filter);
        assertThat(matrix.batters()).extracting(MatchupPlayerDTO::playerId).containsExactly(batter.getId());
        assertThat(matrix.bowlers()).extracting(MatchupPlayerDTO::playerId).containsExactly(bowler.getId());
        assertThat(matrix.cells()).hasSize(1);
        assertThat(matrix.cells().get(0)).isEqualTo(pair());
        filter.setSeasonYear(2025);
        var seasonal = service.matrix(battingTeam.getTeamId(), bowlingTeam.getTeamId(), filter);
        assertThat(seasonal.cells().get(0)).isEqualTo(pair(filter));
        assertThat(seasonal.cells().get(0).stats().runs()).isEqualTo(7);
        // Date ranges narrow encounters without removing the chosen season's squad members.
        filter.setToDate(LocalDate.parse("2025-02-01"));
        assertThat(service.matrix(battingTeam.getTeamId(), bowlingTeam.getTeamId(), filter).cells())
                .hasSize(1);
    }

    @Test
    void includesOccasionalBowlersAndDistinctSquadMembersWithoutDuplicatingCells() {
        sample();
        delivery(second, batter, other, 2, 0, null); // keeper with a recorded bowling delivery
        member(second, bowler, bowlingTeam); // same player in multiple match squads
        member(rosterMatch, bowler, battingTeam); // historic membership in both teams is allowed
        em.flush();
        var matrix = service.matrix(battingTeam.getTeamId(), bowlingTeam.getTeamId(), new MatchupFilterDTO());
        assertThat(matrix.batters()).hasSize(2);
        assertThat(matrix.bowlers()).hasSize(2);
        assertThat(matrix.cells()).hasSize(2);
        assertThat(matrix.cells()).allSatisfy(cell -> assertThat(cell.batter().playerId()).isNotEqualTo(cell.bowler().playerId()));
    }

    @Test
    void matrixIncludesEveryRosterMemberAndEncountersBeyondTheFormerPageLimits() {
        Player lastBatter = null, lastBowler = null;
        for (int i = 0; i < 25; i++) {
            lastBatter = player("Late batter " + i, Roles.Batsman, null);
            member(rosterMatch, lastBatter, battingTeam);
            lastBowler = player("Late bowler " + i, Roles.Baller, BowlingStyle.FastMedium);
            member(rosterMatch, lastBowler, bowlingTeam);
        }
        // This pair's encounter occurred while representing other teams.
        delivery(first, lastBatter, lastBowler, 6, 0, null);
        em.flush();
        var matrix = service.matrix(battingTeam.getTeamId(), bowlingTeam.getTeamId(), new MatchupFilterDTO());
        assertThat(matrix.batters()).hasSize(26);
        assertThat(matrix.bowlers()).hasSize(26);
        assertThat(matrix.cells()).containsExactly(service.pair(lastBatter.getId(), lastBowler.getId(), new MatchupFilterDTO()));
        assertThat(matrix.cells().get(0).stats().runs()).isEqualTo(6);
    }

    @Test
    void opponentListsAndMatchBreakdownsAgreeWithThePairTotalAndRefreshAfterDeliveryEdits() {
        sample();
        delivery(second, batter, other, 2, 0, null);
        Delivery editable = delivery(second, batter, bowler, 3, 0, null);
        editable.setInnings(2);
        em.flush();
        var opponents = service.opponents(batter.getId(), "batting", new MatchupFilterDTO(), 0, 1);
        assertThat(opponents.getTotalElements()).isEqualTo(2);
        assertThat(opponents.getContent().get(0)).isEqualTo(pair());
        var bowling = service.opponents(bowler.getId(), "bowling", new MatchupFilterDTO(), 0, 25);
        assertThat(bowling.getContent()).containsExactly(pair());
        var history = service.matches(batter.getId(), bowler.getId(), new MatchupFilterDTO(), 0, 25);
        assertThat(history.getTotalElements()).isEqualTo(3);
        assertThat(history.getContent()).extracting(MatchupMatchDTO::innings).containsExactly(1, 2, 1);
        assertThat(history.getContent().stream().mapToLong(row -> row.stats().runs()).sum()).isEqualTo(pair().stats().runs());
        assertThat(service.matches(batter.getId(), bowler.getId(), new MatchupFilterDTO(), 1, 2).getContent()).hasSize(1);
        editable.setRunsBatter(6);
        em.flush();
        assertThat(pair().stats().runs()).isEqualTo(17);
    }

    @Test
    void filtersVenueInningsAndBowlingStyleAndBindsUserInputAsData() {
        sample();
        MatchupFilterDTO filter = new MatchupFilterDTO();
        filter.setVenue("Ground Two");
        filter.setInnings(1);
        filter.setBowlingStyle(BowlingStyle.FastMedium);
        assertThat(pair(filter).stats().runs()).isEqualTo(7);
        filter.setBowlingStyle(BowlingStyle.OffSpinner);
        assertThat(pair(filter).stats().deliveries()).isZero();
        filter.setBowlingStyle(null);
        filter.setVenue("Ground Two' OR 1=1 --");
        assertThat(pair(filter).stats().deliveries()).isZero();
        assertThat(service.options().seasons()).contains(2024, 2025);
        assertThat(service.options().venues()).contains("Ground One", "Ground Two");
    }

    @Test
    void rejectsInvalidRangesIdsPerspectiveAndUnboundedPages() {
        MatchupFilterDTO filter = new MatchupFilterDTO();
        filter.setFromDate(LocalDate.parse("2025-02-01"));
        filter.setToDate(LocalDate.parse("2025-01-01"));
        assertThatThrownBy(() -> pair(filter)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("fromDate");
        assertThatThrownBy(() -> service.pair(batter.getId(), batter.getId(), new MatchupFilterDTO()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("different players");
        assertThatThrownBy(() -> service.pair(batter.getId(), Long.MAX_VALUE, new MatchupFilterDTO()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Player not found");
        assertThatThrownBy(() -> service.opponents(batter.getId(), "invalid", new MatchupFilterDTO(), 0, 25))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("perspective");
        assertThatThrownBy(() -> service.opponents(batter.getId(), "batting", new MatchupFilterDTO(), -1, 25))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.matrix(battingTeam.getTeamId(), battingTeam.getTeamId(), new MatchupFilterDTO()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("different teams");
    }

    private MatchupDTO pair() { return pair(new MatchupFilterDTO()); }
    private MatchupDTO pair(MatchupFilterDTO filter) { return service.pair(batter.getId(), bowler.getId(), filter); }
    private void sample() {
        delivery(first, batter, bowler, 4, 0, null);
        dismiss(delivery(first, batter, bowler, 0, 0, null), "caught and bowled", batter);
        delivery(second, batter, bowler, 6, 0, null);
        delivery(second, batter, bowler, 1, 0, null);
        em.flush();
    }
    private Team team(String name) { Team t = new Team(); t.setTeamName(name); return teams.save(t); }
    private Season season(int year) { Season s = new Season(); s.setYear(year); return seasons.save(s); }
    private Player player(String name, Roles role, BowlingStyle style) {
        Player p = new Player(); p.setFullName(name); p.setFirstName(name); p.setLastName("Test");
        p.setNationality("Pakistan"); p.setRole(role); p.setBowlingStyle(style); return players.save(p);
    }
    private Match match(Season season, String date, String venue, Team a, Team b) {
        Match m = new Match(); m.setSeason(season); m.setMatchDate(LocalDate.parse(date));
        m.setVenue(venue); m.setTeamA(a); m.setTeamB(b); m.setFormat(MatchFormat.T20); return matches.save(m);
    }
    private void member(Match match, Player player, Team team) {
        MatchSquad s = new MatchSquad(); s.setMatch(match); s.setPlayer(player); s.setTeam(team); squads.save(s);
    }
    private Delivery delivery(Match match, Player batter, Player bowler, int runs, int extras, String extra) {
        Delivery d = new Delivery(); d.setMatch(match); d.setBatter(batter); d.setBowler(bowler);
        d.setNonStriker(other); d.setInnings(1); d.setOverNum(0); d.setBallNum(1);
        d.setRunsBatter(runs); d.setRunsExtras(extras); d.setExtraType(extra); return deliveries.save(d);
    }
    private void dismiss(Delivery delivery, String kind, Player dismissed) {
        delivery.setWicketType(kind); delivery.setPlayerDismissed(dismissed);
    }
}
