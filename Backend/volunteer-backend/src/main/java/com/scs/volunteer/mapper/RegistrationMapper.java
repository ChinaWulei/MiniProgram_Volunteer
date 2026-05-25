package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class RegistrationMapper {
    private final JdbcTemplate jdbcTemplate;

    public RegistrationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(Long activityId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from registration where activity_id=? and user_id=?", Integer.class, activityId, userId);
        return count != null && count > 0;
    }

    public String findStatus(Long activityId, Long userId) {
        List<String> list = jdbcTemplate.queryForList(
                "select status from registration where activity_id=? and user_id=?",
                String.class, activityId, userId);
        return list.stream().findFirst().orElse(null);
    }

    public void insert(Long activityId, Long userId, String status) {
        jdbcTemplate.update("insert into registration(activity_id,user_id,status) values(?,?,?)", activityId, userId, status);
    }

    public List<Map<String, Object>> my(Long userId) {
        return jdbcTemplate.queryForList("""
                select r.*,a.name as activity_name,a.category,a.location,a.start_time,a.end_time,
                       a.service_hours,a.contact_name,a.contact_phone
                from registration r join activity a on r.activity_id=a.id
                where r.user_id=? order by r.created_at desc
                """, userId);
    }

    public List<Map<String, Object>> aiHistory(Long userId) {
        return jdbcTemplate.queryForList("""
                select a.name as activity_name,a.category,r.status,
                       case when r.status='已完成' then 1 else 0 end as completed,
                       a.service_hours
                from registration r join activity a on r.activity_id=a.id
                where r.user_id=?
                order by r.created_at desc
                limit 20
                """, userId);
    }

    public List<Map<String, Object>> recent() {
        return jdbcTemplate.queryForList("""
                select r.id,u.name as user_name,a.name as activity_name,r.status,r.created_at
                from registration r join user u on r.user_id=u.id join activity a on r.activity_id=a.id
                order by r.created_at desc limit 8
                """);
    }

    public Map<String, Object> findMap(Long id) {
        return jdbcTemplate.queryForMap("select * from registration where id=?", id);
    }

    public void review(Long id, String status, String remark) {
        jdbcTemplate.update("update registration set status=?,review_remark=? where id=?", status, remark, id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from registration where id=?", id);
    }
}
