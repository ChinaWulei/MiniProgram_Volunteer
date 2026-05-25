package com.scs.volunteer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseStartupCheck implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupCheck.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseStartupCheck(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            log.info("Database connection check succeeded");
        } catch (Exception e) {
            log.error("Database connection check failed", e);
        }
    }
}
