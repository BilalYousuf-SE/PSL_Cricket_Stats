package com.cricket.stats.controller;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthControllerTest {
    @Test
    void reportsDatabaseFailuresWithoutExposingConnectionDetails() {
        var jdbc = mock(JdbcTemplate.class);
        var controller = new HealthController(jdbc);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        assertThat(controller.health().getStatusCode().value()).isEqualTo(200);
        when(jdbc.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("Private connection details"));
        var failure = controller.health();
        assertThat(failure.getStatusCode().value()).isEqualTo(503);
        assertThat(failure.getBody()).containsEntry("status", "DOWN").hasSize(1);
    }
}
