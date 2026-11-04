package com.cricket.stats.controller;

import com.cricket.stats.config.AdminAuthFilter;
import com.cricket.stats.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminAuthController.class, properties = "app.admin.password=test-admin")
@Import({AdminAuthTest.WriteProbe.class, CorsConfig.class})
class AdminAuthTest {
    @Autowired MockMvc mvc;

    @RestController
    static class WriteProbe {
        @RequestMapping("/test-records")
        public String record() { return "ok"; }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void protectsEveryWriteMethodAndPermitsTheConfiguredCredential(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), "/test-records"))
                .andExpect(status().isUnauthorized());
        mvc.perform(request(HttpMethod.valueOf(method), "/test-records").header("X-Admin-Password", "wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(request(HttpMethod.valueOf(method), "/test-records").header("X-Admin-Password", "test-admin"))
                .andExpect(status().isOk());
    }

    @Test
    void verifiesSignInOnTheServerAndAllowsPublicReads() throws Exception {
        mvc.perform(post("/admin/auth")).andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/auth").header("X-Admin-Password", "test-admin"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/test-records")).andExpect(status().isOk());
        mvc.perform(head("/test-records")).andExpect(status().isOk());
    }

    @Test
    void supportsCrossOriginPreflightAndReadableAuthenticationErrors() throws Exception {
        mvc.perform(options("/admin/auth").header("Origin", "https://psl-ui.onrender.com")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "X-Admin-Password"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "*"));
        mvc.perform(post("/admin/auth").header("Origin", "https://psl-ui.onrender.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void missingConfigurationDisablesWritesAndPreventsHostedStartup() throws Exception {
        var filter = new AdminAuthFilter("", false);
        var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/teams");
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Unconfigured writes must not reach a controller"));
        assertThat(response.getStatus()).isEqualTo(503);
        assertThatThrownBy(() -> new AdminAuthFilter(" ", true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ADMIN_PASSWORD");
    }
}
