package com.cricket.stats.controller;

import com.cricket.stats.dto.*;
import java.util.List;
import com.cricket.stats.service.MatchupService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchupController.class)
class MatchupControllerTest {
    @Autowired MockMvc mvc;
    @MockBean MatchupService service;

    @Test
    void bindsSeasonAndInclusiveDateFiltersOnThePairEndpoint() throws Exception {
        mvc.perform(get("/matchups/players/1/2").param("seasonYear", "2025")
                .param("fromDate", "2025-01-01").param("toDate", "2025-02-01"))
                .andExpect(status().isOk());
        verify(service).pair(eq(1L), eq(2L), argThat(filter -> filter.getSeasonYear() == 2025
                && filter.getFromDate().equals(LocalDate.parse("2025-01-01"))
                && filter.getToDate().equals(LocalDate.parse("2025-02-01"))));
    }

    @Test
    void routesOpponentHistoryAndMatrixRequests() throws Exception {
        mvc.perform(get("/matchups/players/1").param("perspective", "bowling")).andExpect(status().isOk());
        verify(service).opponents(eq(1L), eq("bowling"), any(MatchupFilterDTO.class), eq(0), eq(25));
        mvc.perform(get("/matchups/players/1/2/matches").param("page", "2")).andExpect(status().isOk());
        verify(service).matches(eq(1L), eq(2L), any(MatchupFilterDTO.class), eq(2), eq(25));
        mvc.perform(get("/matchups/teams").param("battingTeamId", "10").param("bowlingTeamId", "20"))
                .andExpect(status().isOk());
        verify(service).matrix(eq(10), eq(20), any(MatchupFilterDTO.class));
    }

    @Test
    void returnsCompleteMatrixAxesAsLists() throws Exception {
        var roster = java.util.stream.LongStream.rangeClosed(1, 26)
                .mapToObj(id -> new MatchupPlayerDTO(id, "Player " + id, "FastMedium")).toList();
        when(service.matrix(eq(10), eq(20), any(MatchupFilterDTO.class)))
                .thenReturn(new MatchupMatrixDTO(10, "Team A", 20, "Team B", roster, roster, List.of()));
        mvc.perform(get("/matchups/teams").param("battingTeamId", "10").param("bowlingTeamId", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.batters.length()").value(26))
                .andExpect(jsonPath("$.bowlers.length()").value(26))
                .andExpect(jsonPath("$.batters[25].playerId").value(26));
    }

    @ParameterizedTest
    @ValueSource(strings = {"seasonYear=-1", "fromDate=2025-02-30", "toDate=invalid", "innings=0", "bowlingStyle=unknown"})
    void rejectsMalformedFiltersWithAnActionableMessage(String query) throws Exception {
        mvc.perform(get("/matchups/players/1/2?" + query)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(service);
    }
}
