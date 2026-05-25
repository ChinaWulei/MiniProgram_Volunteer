package com.scs.volunteer.mapper;

import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.vo.AiActivityCandidateVO;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ActivityMapper {
    private final JdbcTemplate jdbcTemplate;

    public ActivityMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Activity> search(String category, String status, String keyword) {
        return jdbcTemplate.query("""
                select * from activity
                where (? is null or category=?)
                  and (? is null or status=? or (?='报名中' and status='已发布'))
                  and (? is null or name like concat('%',?,'%') or description like concat('%',?,'%'))
                order by start_time desc
                """, new BeanPropertyRowMapper<>(Activity.class),
                n(category), n(category), n(status), n(status), n(status), n(keyword), n(keyword), n(keyword));
    }

    public Optional<Activity> findById(Long id) {
        List<Activity> list = jdbcTemplate.query("select * from activity where id=?",
                new BeanPropertyRowMapper<>(Activity.class), id);
        return list.stream().findFirst();
    }

    public Long insert(Activity a) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into activity(name,cover_image_url,category,location,start_time,end_time,signup_deadline,
                    recruit_number,registered_number,skill_requirements,description,signup_requirement,contact_name,
                    contact_phone,service_hours,review_method,status,created_by,published_at)
                    values(?,?,?,?,?,?,?,?,0,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, a.getName());
            ps.setString(2, a.getCoverImageUrl());
            ps.setString(3, a.getCategory());
            ps.setString(4, a.getLocation());
            ps.setObject(5, a.getStartTime());
            ps.setObject(6, a.getEndTime());
            ps.setObject(7, a.getSignupDeadline());
            ps.setInt(8, a.getRecruitNumber());
            ps.setString(9, a.getSkillRequirements());
            ps.setString(10, a.getDescription());
            ps.setString(11, a.getSignupRequirement());
            ps.setString(12, a.getContactName());
            ps.setString(13, a.getContactPhone());
            ps.setObject(14, a.getServiceHours());
            ps.setString(15, a.getReviewMethod());
            ps.setString(16, a.getStatus());
            ps.setLong(17, a.getCreatedBy());
            ps.setObject(18, a.getPublishedAt());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void update(Long id, Activity a) {
        jdbcTemplate.update("""
                update activity set name=?,cover_image_url=?,category=?,location=?,start_time=?,end_time=?,signup_deadline=?,recruit_number=?,
                skill_requirements=?,description=?,signup_requirement=?,contact_name=?,contact_phone=?,
                service_hours=?,review_method=?,status=?,published_at=? where id=?
                """, a.getName(), a.getCoverImageUrl(), a.getCategory(), a.getLocation(), a.getStartTime(), a.getEndTime(),
                a.getSignupDeadline(), a.getRecruitNumber(), a.getSkillRequirements(), a.getDescription(), a.getSignupRequirement(),
                a.getContactName(), a.getContactPhone(), a.getServiceHours(), a.getReviewMethod(), a.getStatus(), a.getPublishedAt(), id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from activity where id=?", id);
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("update activity set status=? where id=?", status, id);
    }

    public int participantCount(Long id) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from registration where activity_id=? and status in ('已通过','已完成')", Integer.class, id);
        return count == null ? 0 : count;
    }

    public void increaseRegistered(Long id) {
        jdbcTemplate.update("update activity set registered_number=registered_number+1, status=if(registered_number+1>=recruit_number,'已满员',status) where id=?", id);
    }

    public void finishExpired() {
        jdbcTemplate.update("update activity set status='已结束' where end_time<? and status in ('报名中','已发布')", LocalDateTime.now());
    }

    public List<AiActivityCandidateVO> availableForAi() {
        return jdbcTemplate.query("""
                select id,name,category,location,start_time,end_time,skill_requirements,description,service_hours,
                       greatest(recruit_number - registered_number, 0) as remaining_slots
                from activity
                where end_time > ?
                  and status in ('报名中','已发布')
                  and registered_number < recruit_number
                order by start_time asc
                limit 50
                """, new BeanPropertyRowMapper<>(AiActivityCandidateVO.class), LocalDateTime.now());
    }

    private String n(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
