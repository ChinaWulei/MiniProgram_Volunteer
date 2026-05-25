package com.scs.volunteer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String token = maskToken(request.getHeader("Authorization"));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            String fullPath = query == null || query.isBlank() ? uri : uri + "?" + query;
            log.info("HTTP {} {} -> {} {}ms token={}", method, fullPath, response.getStatus(), elapsed, token);
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Request logging filter enabled");
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "-";
        }
        if (token.length() <= 12) {
            return "***";
        }
        return token.substring(0, 8) + "***" + token.substring(token.length() - 4);
    }
}
