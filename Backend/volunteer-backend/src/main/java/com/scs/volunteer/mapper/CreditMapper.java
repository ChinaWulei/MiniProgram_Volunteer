package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class CreditMapper {
    private final JdbcTemplate jdbcTemplate;

    public CreditMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int score(Long userId) {
        List<Integer> scores = jdbcTemplate.queryForList("select coalesce(credit_score,100) from volunteer_profile where user_id=?", Integer.class, userId);
        return scores.isEmpty() || scores.get(0) == null ? 100 : scores.get(0);
    }

    public List<Map<String, Object>> records(Long userId) {
        return jdbcTemplate.queryForList("""
                select id,change_value as changeValue,reason,source_type as sourceType,source_id as sourceId,created_at as createdAt
                from credit_record
                where user_id=?
                order by created_at desc,id desc
                limit 20
                """, userId);
    }

    public void apply(Long userId, int changeValue, String reason, String sourceType, Long sourceId) {
        jdbcTemplate.update("""
                insert into credit_record(user_id,change_value,reason,source_type,source_id,created_at)
                values(?,?,?,?,?,?)
                """, userId, changeValue, reason, sourceType, sourceId, LocalDateTime.now());
        jdbcTemplate.update("""
                update volunteer_profile
                set credit_score=greatest(0, least(100, credit_score + ?))
                where user_id=?
                """, changeValue, userId);
    }

    public List<Map<String, Object>> rules() {
        return jdbcTemplate.queryForList("select * from credit_rule order by id asc");
    }

    public void saveRule(String code, String name, Integer changeValue, Boolean enabled) {
        jdbcTemplate.update("""
                insert into credit_rule(code,name,change_value,enabled)
                values(?,?,?,?)
                on duplicate key update name=values(name),change_value=values(change_value),enabled=values(enabled)
                """, code, name, changeValue, enabled);
    }
}
