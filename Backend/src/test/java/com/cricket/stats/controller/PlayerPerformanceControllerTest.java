package com.cricket.stats.controller;

import com.cricket.stats.dto.*;
import com.cricket.stats.service.PlayerPerformanceService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayerPerformanceController.class)
class PlayerPerformanceControllerTest {
    @Autowired MockMvc mvc;
    @MockBean PlayerPerformanceService service;

    @Test void servesPublicPerformanceWithoutAdminCredentials() throws Exception {
        when(service.performance(eq(1L), any())).thenReturn(new PlayerPerformanceDTO(1L, "fielding", null, Map.of(), Map.of()));
        mvc.perform(get("/players/1/performance").param("discipline", "fielding"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.discipline").value("fielding"));
    }

    @Test void rejectsInvalidDisciplinePositionAndEnumValues() throws Exception {
        mvc.perform(get("/players/1/performance").param("discipline", "anything")).andExpect(status().isBadRequest());
        mvc.perform(get("/players/1/performance").param("battingPosition", "12")).andExpect(status().isBadRequest());
        mvc.perform(get("/players/1/performance").param("stage", "bad-stage")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void bindsContextualFiltersIncludingInclusiveDates() throws Exception {
        mvc.perform(get("/players/1/performance").param("discipline", "keeping")
            .param("seasonYear", "2025").param("fromDate", "2025-04-01").param("toDate", "2025-05-25")
            .param("captaincy", "not-captain").param("batterHand", "Left").param("stage", "Qualifier"))
            .andExpect(status().isOk());
        verify(service).performance(eq(1L), argThat(f -> f.getDiscipline().equals("keeping")
            && f.getSeasonYear() == 2025 && f.getBatterHand().name().equals("Left")
            && f.getStage().name().equals("Qualifier") && f.getCaptaincy().equals("not-captain")));
    }
}
