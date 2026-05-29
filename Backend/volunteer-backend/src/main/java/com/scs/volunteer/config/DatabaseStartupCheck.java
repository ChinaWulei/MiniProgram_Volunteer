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
            jdbcTemplate.execute("""
                    create table if not exists announcement (
                        id bigint primary key auto_increment,
                        title varchar(200) not null,
                        content text not null,
                        status varchar(20) not null default 'DRAFT',
                        created_by bigint not null,
                        published_at datetime null,
                        created_at datetime not null default current_timestamp,
                        updated_at datetime not null default current_timestamp on update current_timestamp,
                        index idx_announcement_status(status),
                        index idx_announcement_published(published_at)
                    )
                    """);
            jdbcTemplate.execute("""
                    create table if not exists announcement_image (
                        id bigint primary key auto_increment,
                        announcement_id bigint not null,
                        image_url varchar(700) not null,
                        sort_order int not null default 0,
                        created_at datetime not null default current_timestamp,
                        index idx_announcement_image(announcement_id)
                    )
                    """);
            jdbcTemplate.execute("""
                    create table if not exists announcement_attachment (
                        id bigint primary key auto_increment,
                        announcement_id bigint not null,
                        rule_file_id bigint not null,
                        created_at datetime not null default current_timestamp,
                        unique key uk_announcement_rule_file(announcement_id, rule_file_id),
                        index idx_announcement_attachment(announcement_id)
                    )
                    """);
            jdbcTemplate.execute("""
                    create table if not exists checkin_adjustment (
                        id bigint primary key auto_increment,
                        activity_id bigint not null,
                        user_id bigint not null,
                        original_status varchar(40) null,
                        new_status varchar(40) null,
                        original_checkin_time datetime null,
                        new_checkin_time datetime null,
                        reason varchar(500) null,
                        description text null,
                        proof_image_url varchar(700) null,
                        original_service_hours decimal(8,2) null,
                        new_service_hours decimal(8,2) null,
                        hours_reason varchar(500) null,
                        admin_remark varchar(500) null,
                        audit_status varchar(30) not null default 'PENDING',
                        admin_id bigint null,
                        created_at datetime not null default current_timestamp,
                        updated_at datetime not null default current_timestamp on update current_timestamp,
                        index idx_checkin_adjustment_user(user_id, created_at),
                        index idx_checkin_adjustment_admin(audit_status, activity_id, created_at),
                        index idx_checkin_adjustment_target(activity_id, user_id)
                    )
                    """);
            jdbcTemplate.execute("""
                    create table if not exists ai_report (
                        id bigint primary key auto_increment,
                        report_no varchar(80) not null,
                        report_type varchar(20) not null,
                        user_id bigint not null,
                        period_start varchar(20) null,
                        period_end varchar(20) null,
                        stats_json longtext not null,
                        ai_analysis longtext null,
                        pdf_url varchar(700) null,
                        created_at datetime not null default current_timestamp,
                        updated_at datetime not null default current_timestamp on update current_timestamp,
                        unique key uk_ai_report_no(report_no),
                        index idx_ai_report_user(user_id, report_type, created_at)
                    )
                    """);
            ensureColumn("activity", "tips", "alter table activity add column tips text null after service_hours");
            log.info("Database connection check succeeded");
        } catch (Exception e) {
            log.error("Database connection check failed", e);
        }
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = ?
                  and column_name = ?
                """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
            log.info("Added missing column {}.{}", tableName, columnName);
        }
    }
}
