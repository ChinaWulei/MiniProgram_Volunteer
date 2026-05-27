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
            jdbcTemplate.execute("""
                    create table if not exists rule_file (
                        id bigint primary key auto_increment,
                        original_name varchar(255) not null,
                        file_type varchar(20) not null,
                        file_size bigint not null,
                        s3_key varchar(500) not null,
                        s3_url varchar(700) not null,
                        status varchar(30) not null default 'PROCESSING',
                        chunk_count int not null default 0,
                        created_by bigint not null,
                        created_at datetime not null default current_timestamp,
                        updated_at datetime not null default current_timestamp on update current_timestamp,
                        index idx_rule_file_status(status)
                    )
                    """);
            jdbcTemplate.execute("""
                    create table if not exists chat_block (
                        id bigint primary key auto_increment,
                        blocker_id bigint not null,
                        blocked_user_id bigint not null,
                        created_at datetime not null default current_timestamp,
                        unique key uk_chat_block_pair(blocker_id, blocked_user_id),
                        index idx_chat_block_blocked(blocked_user_id)
                    )
                    """);
            log.info("Database connection check succeeded");
        } catch (Exception e) {
            log.error("Database connection check failed", e);
        }
    }
}
