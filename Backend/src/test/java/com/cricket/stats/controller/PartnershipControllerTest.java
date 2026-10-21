package com.cricket.stats.controller;

import com.cricket.stats.dto.PartnershipDTO;
import com.cricket.stats.service.PartnershipService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PartnershipControllerTest {
    @Mock PartnershipService service;
    MockMvc mvc;
    static final String BODY = """
            {"innings":1,"batter1Id":1,"batter2Id":2,"runs":45,"ballsFaced":30,"wicketNumber":1}
            """;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new PartnershipController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void retrievesMatchInningsAndIndividualRecords() throws Exception {
        PartnershipDTO dto = new PartnershipDTO();
        dto.setPartnershipId(50L);
        dto.setInnings(1);
        when(service.getForMatch(10, 1)).thenReturn(List.of(dto));
        when(service.getById(50L)).thenReturn(dto);
        mvc.perform(get("/matches/10/partnerships").param("innings", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].partnershipId").value(50));
        mvc.perform(get("/partnerships/50"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.innings").value(1));
    }

    @Test
    void supportsPlayerPairSeasonFiltersAndDefaultPagination() throws Exception {
        when(service.search(null, null, 2026, 1L, 2L, 0, 50))
                .thenReturn(Page.empty(PageRequest.of(0, 50)));
        mvc.perform(get("/partnerships").param("seasonYear", "2026")
                .param("playerId", "1").param("partnerId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void supportsCreateUpdateAndDeleteUnderTheMatch() throws Exception {
        PartnershipDTO dto = new PartnershipDTO();
        dto.setPartnershipId(50L);
        when(service.create(eq(10), any())).thenReturn(dto);
        when(service.update(eq(10), eq(50L), any())).thenReturn(dto);
        mvc.perform(post("/matches/10/partnerships").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.partnershipId").value(50));
        mvc.perform(put("/matches/10/partnerships/50").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
        mvc.perform(delete("/matches/10/partnerships/50")).andExpect(status().isNoContent());
        verify(service).delete(10, 50L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "innings", "runs", "ballsFaced", "wicketNumber", "batter1Id"})
    void rejectsInvalidCreateAndUpdateBodiesBeforeWriting(String field) throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var fields = json.readTree(BODY);
        if (field.equals("missing")) {
            fields = json.createObjectNode();
        } else {
            ((com.fasterxml.jackson.databind.node.ObjectNode) fields)
                    .put(field, field.equals("wicketNumber") ? 11 : -1);
        }
        String body = json.writeValueAsString(fields);
        mvc.perform(post("/matches/10/partnerships").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        mvc.perform(put("/matches/10/partnerships/50").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsStructuredNotFoundErrors() throws Exception {
        when(service.getById(99L)).thenThrow(new ResponseStatusException(NOT_FOUND, "Partnership not found"));
        mvc.perform(get("/partnerships/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Partnership not found"));
    }
}
