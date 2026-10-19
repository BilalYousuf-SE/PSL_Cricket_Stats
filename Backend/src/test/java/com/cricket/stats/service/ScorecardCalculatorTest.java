package com.cricket.stats.service;

import com.cricket.stats.entity.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ScorecardCalculatorTest {
    final ScorecardCalculator calculator = new ScorecardCalculator();
    final Match match = new Match();

    @Test
    void excludesIllegalBallsByesAndRunOutsFromBowlingFigures() {
        Delivery wide = ball(1, "wides", null, 0, 1);
        Delivery noBall = ball(2, "no_ball", null, 4, 1);
        Delivery legBye = ball(3, "leg_byes", null, 0, 2);
        Delivery runOut = ball(4, null, "run_out", 0, 0);
        List<Delivery> balls = List.of(wide, noBall, legBye, runOut);
        ScorecardBowler row = calculator.bowling(match, balls).get(0);
        assertThat(row.getOversBowled()).isEqualByComparingTo("0.2");
        assertThat(row.getRunsConceded()).isEqualTo(6);
        assertThat(row.getWickets()).isZero();
        assertThat(row.getMaidens()).isZero();
        assertThat(row.getEconomy()).isEqualByComparingTo("18.00");
        ScorecardBatter batting = calculator.batting(match, balls).get(0);
        assertThat(batting.getBallsFaced()).isEqualTo(3);
        assertThat(batting.getFours()).isEqualTo(1);
    }

    @Test
    void onlyACompleteZeroRunOverIsAMaiden() {
        List<Delivery> balls = new ArrayList<>();
        for (int i = 1; i <= 6; i++) balls.add(ball(i, null, null, 0, 0));
        assertThat(calculator.bowling(match, balls.subList(0, 5)).get(0).getMaidens()).isZero();
        assertThat(calculator.bowling(match, balls).get(0).getMaidens()).isEqualTo(1);
        balls.add(ball(7, "wide", null, 0, 1));
        assertThat(calculator.bowling(match, balls).get(0).getMaidens()).isZero();
    }

    @Test
    void includesANonStrikerDismissedBeforeFacingABall() {
        Delivery delivery = ball(1, null, "run out", 0, 0);
        delivery.setPlayerDismissed(player(3));
        List<ScorecardBatter> rows = calculator.batting(match, List.of(delivery));
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).getPlayer().getId()).isEqualTo(3L);
        assertThat(rows.get(1).getBallsFaced()).isZero();
        assertThat(rows.get(1).getIsOut()).isTrue();
        assertThat(calculator.bowling(match, List.of(delivery)).get(0).getWickets()).isZero();
    }

    @Test
    void blankWicketsAreIgnoredAndCaughtWicketsAreCredited() {
        Delivery blank = ball(1, null, " ", 0, 0);
        Delivery caught = ball(2, null, "caught", 0, 0);
        caught.setPlayerDismissed(player(1));
        assertThat(calculator.bowling(match, List.of(blank, caught)).get(0).getWickets()).isEqualTo(1);
        assertThat(calculator.batting(match, List.of(blank)).get(0).getIsOut()).isFalse();
    }

    private Delivery ball(int number, String extra, String wicket, int runs, int extras) {
        Delivery delivery = new Delivery();
        delivery.setMatch(match);
        delivery.setBatter(player(1));
        delivery.setBowler(player(2));
        delivery.setInnings(1);
        delivery.setOverNum(0);
        delivery.setBallNum(number);
        delivery.setRunsBatter(runs);
        delivery.setRunsExtras(extras);
        delivery.setExtraType(extra);
        delivery.setWicketType(wicket);
        return delivery;
    }

    private Player player(long id) {
        Player player = new Player();
        player.setId(id);
        return player;
    }
}
