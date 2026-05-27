package com.scs.volunteer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class VectorJdbcConfig {
    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(RagProperties properties) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(properties.getVector().getUrl());
        dataSource.setUsername(properties.getVector().getUsername());
        dataSource.setPassword(properties.getVector().getPassword());
        return new JdbcTemplate(dataSource);
    }
}
