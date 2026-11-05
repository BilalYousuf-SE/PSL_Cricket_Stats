package com.cricket.stats.service;

import com.cricket.stats.ID.MatchSquadId;
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
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PlayerPerformanceIntegrationTest {
    @Autowired PlayerPerformanceService performance;
    @Autowired MatchService matchService;
    @Autowired PlayerRepository players;
    @Autowired TeamRepository teams;
    @Autowired SeasonRepository seasons;
    @Autowired MatchRepository matches;
    @Autowired MatchSquadRepository squads;
    @Autowired DeliveryRepository deliveries;
    @Autowired EntityManager em;
    Player player, partner, opponent, keeper;
    Team a, b, c;
    Match first, second;

    @BeforeEach void setup() {
        a = team("Performance A"); b = team("Performance B"); c = team("Performance C");
        player = player("Subject", Roles.Allrounder, PlayingStyle.Right, BowlingStyle.FastMedium);
        partner = player("Partner", Roles.Batsman, PlayingStyle.Left, null);
        opponent = player("Opponent", Roles.Baller, PlayingStyle.Left, BowlingStyle.LeftArmOrthodox);
        keeper = player("Keeper", Roles.Keeper, PlayingStyle.Right, null);
        first = match(2024, "2025-01-01", "Ground One", a, b, MatchStage.League);
        second = match(2025, "2025-02-01", "Ground Two", c, b, MatchStage.Final);
        member(first, player, a, true, false); member(first, partner, a, false, true);
        member(second, player, c, false, false); member(second, partner, c, true, true);
        for (Match m : new Match[]{first, second}) {
            member(m, opponent, b, true, false); member(m, keeper, b, false, true);
        }
    }

    @Test void battingRatesUseTotalsAndAllDismissalsAcrossTeams() {
        for (int i = 0; i < 30; i++) {
            Delivery d = ball(i < 15 ? first : second, 1, i % 15 + 1, player, partner, opponent,
                i < 3 ? 6 : i == 3 ? 4 : i < 27 ? 1 : 0, 0, null);
            if (i == 14 || i == 29) wicket(d, i == 14 ? "run out" : "caught", player, keeper);
        }
        var result = stats(player, "batting", new PlayerPerformanceFilterDTO());
        assertThat(result.totals().runs()).isEqualTo(45);
        assertThat(result.totals().ballsFaced()).isEqualTo(30);
        assertThat(result.totals().dismissals()).isEqualTo(2);
        assertThat(result.totals().strikeRate()).isEqualByComparingTo("150.00");
        assertThat(result.totals().average()).isEqualByComparingTo("22.50");
        assertThat(result.totals().sixes()).isEqualTo(3);
        assertThat(result.breakdowns().get("team")).hasSize(2);
    }

    @Test void nonStrikerRunOutBeforeFacingIsAnOutAtPositionTwo() {
        wicket(ball(first, 1, 1, partner, player, opponent, 0, 0, null), "run_out", player, keeper);
        var result = stats(player, "batting", new PlayerPerformanceFilterDTO());
        assertThat(result.totals().innings()).isOne();
        assertThat(result.totals().ballsFaced()).isZero();
        assertThat(result.totals().dismissals()).isOne();
        assertThat(result.totals().average()).isEqualByComparingTo("0.00");
        assertThat(result.totals().strikeRate()).isNull();
        assertThat(result.breakdowns().get("battingPosition").get(0).key()).isEqualTo("2");
    }

    @Test void bowlingExcludesByesRunOutsAndInvalidWickets() {
        ball(first, 2, 1, opponent, keeper, player, 0, 0, null);
        ball(first, 2, 2, opponent, keeper, player, 0, 2, "byes");
        ball(first, 2, 3, opponent, keeper, player, 0, 4, "leg byes");
        ball(first, 2, 4, opponent, keeper, player, 6, 1, "no-ball");
        ball(first, 2, 5, opponent, keeper, player, 0, 2, "wides");
        wicket(ball(first, 2, 6, opponent, keeper, player, 0, 1, "wide"), "stumped", opponent, partner);
        wicket(ball(first, 2, 7, opponent, keeper, player, 0, 0, null), "run out", keeper, partner);
        wicket(ball(first, 2, 8, opponent, keeper, player, 1, 0, null), "run out", opponent, partner);
        wicket(ball(first, 2, 9, opponent, keeper, player, 0, 1, "No Ball"), "bowled", opponent, null);
        wicket(ball(first, 2, 10, opponent, keeper, player, 0, 0, null), "retired hurt", opponent, null);
        wicket(ball(first, 2, 11, opponent, keeper, player, 0, 0, null), "hit wicket", opponent, null);
        var result = stats(player, "bowling", new PlayerPerformanceFilterDTO());
        assertThat(result.totals().runsConceded()).isEqualTo(12);
        assertThat(result.totals().legalBalls()).isEqualTo(7);
        assertThat(result.totals().dismissals()).isEqualTo(2);
        assertThat(result.totals().economy()).isEqualByComparingTo("10.29");
        assertThat(result.totals().bowlingAverage()).isEqualByComparingTo("6.00");
        assertThat(result.totals().bowlingStrikeRate()).isEqualByComparingTo("3.50");
    }

    @Test void combinesAssignedSeasonInclusiveDatesTeamCaptaincyAndStage() {
        ball(first, 1, 1, player, partner, opponent, 4, 0, null);
        ball(second, 1, 1, player, partner, opponent, 6, 0, null);
        var filter = new PlayerPerformanceFilterDTO();
        filter.setSeasonYear(2024); filter.setFromDate(LocalDate.parse("2025-01-01"));
        filter.setToDate(LocalDate.parse("2025-01-01")); filter.setTeamId(a.getTeamId());
        filter.setStage(MatchStage.League); filter.setCaptaincy("captain");
        filter.setInnings(1); filter.setBattingPosition(1); filter.setBowlingStyle(BowlingStyle.LeftArmOrthodox);
        assertThat(stats(player, "batting", filter).totals().runs()).isEqualTo(4);
        filter.setSeasonYear(2025);
        assertThat(stats(player, "batting", filter).totals().deliveries()).isZero();
        var choices = performance.options(player.getId());
        assertThat(choices.seasons()).containsExactly(2025, 2024);
        assertThat(choices.venues()).containsExactly("Ground One", "Ground Two");
    }

    @Test void derivesPositionsSeparatelyInExtraInnings() {
        ball(first, 1, 1, player, partner, opponent, 4, 0, null);
        ball(first, 3, 1, partner, player, opponent, 0, 0, null);
        ball(first, 3, 2, player, partner, opponent, 6, 0, null);
        var filter = new PlayerPerformanceFilterDTO(); filter.setInnings(3); filter.setBattingPosition(2);
        var result = stats(player, "batting", filter);
        assertThat(result.totals().runs()).isEqualTo(6);
        assertThat(result.totals().innings()).isOne();
        assertThat(result.breakdowns().get("innings").get(0).key()).isEqualTo("3");
    }

    @Test void keepingUsesMatchAssignmentRatherThanCareerRole() {
        member(first, player, a, true, true); member(first, partner, a, false, false);
        wicket(ball(first, 2, 1, opponent, keeper, partner, 0, 0, null), "caught", opponent, player);
        wicket(ball(second, 2, 1, opponent, keeper, partner, 0, 0, null), "caught", opponent, player);
        wicket(ball(second, 2, 2, opponent, keeper, partner, 0, 1, "wide"), "stumped", opponent, player);
        var keeping = stats(player, "keeping", new PlayerPerformanceFilterDTO());
        assertThat(keeping.totals().catches()).isOne();
        assertThat(keeping.totals().stumpings()).isOne();
        var fielding = stats(player, "fielding", new PlayerPerformanceFilterDTO());
        assertThat(fielding.totals().catches()).isEqualTo(2);
        assertThat(fielding.totals().keeperCatches()).isOne();
        assertThat(fielding.totals().outfieldCatches()).isOne();
    }

    @Test void stumpingFallbackRequiresExactlyOneAssignedPlayingKeeper() {
        member(first, player, a, true, true); member(first, partner, a, false, false);
        Player bench = player("Bench keeper", Roles.Keeper, PlayingStyle.Right, null);
        MatchSquad reserve = member(first, bench, a, false, true); reserve.setIsPlaying(false);
        Delivery known = wicket(ball(first, 2, 1, opponent, keeper, partner, 0, 1, "wide"), "stumped", opponent, null);
        member(second, player, c, false, true); // partner is also an assigned keeper
        wicket(ball(second, 2, 1, opponent, keeper, partner, 0, 0, null), "stumped", opponent, null);
        var result = stats(player, "keeping", new PlayerPerformanceFilterDTO());
        assertThat(result.totals().stumpings()).isOne();
        assertThat(result.coverage().get("unidentifiedFieldingDismissals")).isEqualTo(1L);
        assertThat(known.getFielder()).isNull(); // retrieval never writes inferred metadata
    }

    @Test void unknownKeeperAssignmentsDoNotTurnAllCatchesIntoKeeperCatches() {
        member(first, partner, a, false, false);
        wicket(ball(first, 2, 1, opponent, keeper, partner, 0, 0, null), "caught", opponent, player);
        var keeping = stats(player, "keeping", new PlayerPerformanceFilterDTO());
        assertThat(keeping.totals().dismissals()).isZero();
        assertThat(keeping.coverage().get("unclassifiedKeeperCatches")).isEqualTo(1L);
        assertThat(stats(player, "fielding", new PlayerPerformanceFilterDTO()).totals().unclassifiedCatches()).isOne();
    }

    @Test void caughtAndBowledIsAnOutfieldCatchEvenForAnAssignedKeeper() {
        member(first, player, a, true, true); member(first, partner, a, false, false);
        wicket(ball(first, 2, 1, opponent, keeper, player, 0, 0, null), "caught & bowled", opponent, null);
        assertThat(stats(player, "fielding", new PlayerPerformanceFilterDTO()).totals().outfieldCatches()).isOne();
        assertThat(stats(player, "keeping", new PlayerPerformanceFilterDTO()).totals().dismissals()).isZero();
    }

    @Test void fieldingHandAndPositionBelongToTheDismissedNonStriker() {
        wicket(ball(first, 2, 1, opponent, keeper, partner, 0, 0, null), "run out", keeper, player);
        var filter = new PlayerPerformanceFilterDTO();
        filter.setBatterHand(PlayingStyle.Right); filter.setBattingPosition(2);
        assertThat(stats(player, "fielding", filter).totals().runOuts()).isOne();
        filter.setBatterHand(PlayingStyle.Left);
        assertThat(stats(player, "fielding", filter).totals().runOuts()).isZero();
    }

    @Test void missingFieldersAndInvalidDismissalsAreNotAttributed() {
        wicket(ball(first, 2, 1, opponent, keeper, player, 0, 0, null), "run out", opponent, null);
        wicket(ball(first, 2, 2, opponent, keeper, partner, 0, 1, "no ball"), "caught", opponent, player);
        wicket(ball(first, 2, 3, opponent, keeper, partner, 0, 1, "no ball"), "stumped", opponent, player);
        var result = stats(player, "fielding", new PlayerPerformanceFilterDTO());
        assertThat(result.totals().dismissals()).isZero();
        assertThat(result.coverage().get("unidentifiedFieldingDismissals")).isEqualTo(1L);
    }

    @Test void allFalseCaptainFlagsAreUnrecordedAndUndefinedRatesStayNull() {
        member(second, partner, c, false, true);
        ball(second, 1, 1, player, partner, opponent, 0, 0, null);
        var result = stats(player, "batting", new PlayerPerformanceFilterDTO());
        assertThat(result.breakdowns().get("captaincy").get(0).key()).isEqualTo("unknown");
        assertThat(result.totals().strikeRate()).isEqualByComparingTo("0.00");
        assertThat(result.totals().average()).isNull();
    }

    @Test void olderDeliveryUpdatesPreserveTheNewFielderUnlessExplicitlyCleared() {
        Delivery d = wicket(ball(first, 1, 1, player, partner, opponent, 0, 0, null), "caught", player, keeper);
        em.flush();
        var request = new DeliveryRequestDTO(); request.setInnings(1); request.setOverNum(1); request.setBallNum(1);
        request.setBatterId(player.getId()); request.setNonStrikerId(partner.getId()); request.setBowlerId(opponent.getId());
        request.setWicketType("caught"); request.setPlayerDismissedId(player.getId());
        assertThat(matchService.updateDelivery(first.getMatchId(), d.getDeliveryId(), request).getFielderId()).isEqualTo(keeper.getId());
        request.setFielderId(null);
        assertThat(matchService.updateDelivery(first.getMatchId(), d.getDeliveryId(), request).getFielderId()).isNull();
    }

    private PlayerPerformanceDTO stats(Player p, String mode, PlayerPerformanceFilterDTO filter) {
        em.flush(); filter.setDiscipline(mode); return performance.performance(p.getId(), filter);
    }
    private Team team(String name) { Team t = new Team(); t.setTeamName(name); return teams.save(t); }
    private Player player(String name, Roles role, PlayingStyle hand, BowlingStyle style) {
        Player p = new Player(); p.setFirstName(name); p.setLastName("Test"); p.setFullName(name + " Test");
        p.setNationality("Pakistan"); p.setRole(role); p.setPlayingStyle(hand); p.setBowlingStyle(style); return players.save(p);
    }
    private Match match(int year, String date, String venue, Team home, Team away, MatchStage stage) {
        Season season = new Season(); season.setYear(year); seasons.save(season);
        Match m = new Match(); m.setSeason(season); m.setMatchDate(LocalDate.parse(date)); m.setVenue(venue);
        m.setFormat(MatchFormat.T20); m.setTeamA(home); m.setTeamB(away); m.setStage(stage); return matches.save(m);
    }
    private MatchSquad member(Match m, Player p, Team t, boolean captain, boolean keeping) {
        MatchSquadId key = new MatchSquadId(); key.setMatch(m.getMatchId()); key.setPlayer(p.getId());
        MatchSquad row = squads.findById(key).orElseGet(MatchSquad::new);
        row.setMatch(m); row.setPlayer(p); row.setTeam(t); row.setIsCaptain(captain); row.setIsKeeper(keeping);
        row.setIsPlaying(true); return squads.save(row);
    }
    private Delivery ball(Match m, int innings, int ordinal, Player batter, Player nonStriker, Player bowler, int runs, int extras, String type) {
        Delivery d = new Delivery(); d.setMatch(m); d.setInnings(innings); d.setOverNum((ordinal - 1) / 6 + 1);
        d.setBallNum((ordinal - 1) % 6 + 1); d.setBatter(batter); d.setNonStriker(nonStriker); d.setBowler(bowler);
        d.setRunsBatter(runs); d.setRunsExtras(extras); d.setExtraType(type); return deliveries.save(d);
    }
    private Delivery wicket(Delivery d, String kind, Player out, Player fielder) {
        d.setWicketType(kind); d.setPlayerDismissed(out); d.setFielder(fielder); return d;
    }
}
