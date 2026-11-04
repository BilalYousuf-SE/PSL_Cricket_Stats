package com.cricket.stats.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AdminAuthFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private final byte[] password;

    public AdminAuthFilter(@Value("${app.admin.password:}") String password,
                           @Value("${app.admin.require-password:false}") boolean required) {
        if (required && password.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD must be configured before starting the hosted API");
        }
        this.password = password.isBlank() ? new byte[0] : password.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
        if (password.length == 0) {
            reject(response, 503, "Admin access is not configured. Set ADMIN_PASSWORD on the backend.");
            return;
        }
        String supplied = request.getHeader("X-Admin-Password");
        if (supplied == null || !MessageDigest.isEqual(password, supplied.getBytes(StandardCharsets.UTF_8))) {
            reject(response, 401, "Wrong or missing admin password");
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
