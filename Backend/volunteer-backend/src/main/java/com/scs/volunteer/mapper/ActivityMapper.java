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
                    insert into activity(name,cover_image_url,category,location,latitude,longitude,start_time,end_time,signup_start_time,signup_deadline,checkin_start_time,checkin_end_time,
                    recruit_number,registered_number,skill_requirements,description,signup_requirement,contact_name,
                    contact_phone,service_hours,tips,review_method,status,created_by,published_at)
                    values(?,?,?,?,?,?,?,?,?,?,?,?,?,0,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, a.getName());
            ps.setString(2, a.getCoverImageUrl());
            ps.setString(3, a.getCategory());
            ps.setString(4, a.getLocation());
            ps.setObject(5, a.getLatitude());
            ps.setObject(6, a.getLongitude());
            ps.setObject(7, a.getStartTime());
            ps.setObject(8, a.getEndTime());
            ps.setObject(9, a.getSignupStartTime());
            ps.setObject(10, a.getSignupDeadline());
            ps.setObject(11, a.getCheckinStartTime());
            ps.setObject(12, a.getCheckinEndTime());
            ps.setInt(13, a.getRecruitNumber());
            ps.setString(14, a.getSkillRequirements());
            ps.setString(15, a.getDescription());
            ps.setString(16, a.getSignupRequirement());
            ps.setString(17, a.getContactName());
            ps.setString(18, a.getContactPhone());
            ps.setObject(19, a.getServiceHours());
            ps.setString(20, a.getTips());
            ps.setString(21, a.getReviewMethod());
            ps.setString(22, a.getStatus());
            ps.setLong(23, a.getCreatedBy());
            ps.setObject(24, a.getPublishedAt());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void update(Long id, Activity a) {
        jdbcTemplate.update("""
                update activity set name=?,cover_image_url=?,category=?,location=?,latitude=?,longitude=?,start_time=?,end_time=?,
                signup_start_time=?,signup_deadline=?,checkin_start_time=?,checkin_end_time=?,recruit_number=?,
                skill_requirements=?,description=?,signup_requirement=?,contact_name=?,contact_phone=?,tips=?,
                service_hours=?,review_method=?,status=?,published_at=? where id=?
                """, a.getName(), a.getCoverImageUrl(), a.getCategory(), a.getLocation(), a.getLatitude(), a.getLongitude(), a.getStartTime(), a.getEndTime(),
                a.getSignupStartTime(), a.getSignupDeadline(), a.getCheckinStartTime(), a.getCheckinEndTime(), a.getRecruitNumber(), a.getSkillRequirements(), a.getDescription(), a.getSignupRequirement(),
                a.getContactName(), a.getContactPhone(), a.getTips(), a.getServiceHours(), a.getReviewMethod(), a.getStatus(), a.getPublishedAt(), id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from activity where id=?", id);
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("update activity set status=?, finished_at=if(?='已结束', now(), finished_at) where id=?", status, status, id);
    }

    public int participantCount(Long id) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from registration where activity_id=? and status in ('已通过','已完成')", Integer.class, id);
        return count == null ? 0 : count;
    }

    public List<Long> participantUserIds(Long id) {
        return jdbcTemplate.queryForList("select user_id from registration where activity_id=? and status in ('已通过','已完成')", Long.class, id);
    }

    public void increaseRegistered(Long id) {
        jdbcTemplate.update("update activity set registered_number=registered_number+1, status=if(registered_number+1>=recruit_number,'已满员',status) where id=?", id);
    }

    public void decreaseRegistered(Long id) {
        jdbcTemplate.update("update activity set registered_number=greatest(registered_number-1,0), status=if(status='已满员','已发布',status) where id=?", id);
    }

    public void finishExpired() {
        jdbcTemplate.update("update activity set status='已结束' where end_time<? and status in ('报名中','已发布')", LocalDateTime.now());
    }

    public void refreshLifecycleStatus() {
        LocalDateTime now = LocalDateTime.now();
        refreshRegisteredNumbers();
        jdbcTemplate.update("""
                update activity
                set status = case
                    when finished_at is not null then '已结束'
                    when end_time < ? then '已结束'
                    when registered_number >= recruit_number then '已满员'
                    when coalesce(signup_start_time, published_at, created_at) <= ?
                         and coalesce(signup_deadline, start_time) >= ? then '报名中'
                    else '已发布'
                end
                where status not in ('草稿','已取消')
                """, now, now, now);
    }

    public void refreshRegisteredNumbers() {
        jdbcTemplate.update("""
                update activity a
                set registered_number = (
                    select count(*)
                    from registration r
                    where r.activity_id = a.id
                      and r.status in ('待审核','已通过','已完成')
                )
                """);
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

    public List<Activity> openActivities() {
        return jdbcTemplate.query("""
                select * from activity
                where end_time > ?
                  and status in ('报名中','已发布')
                  and registered_number < recruit_number
                order by start_time asc
                limit 50
                """, new BeanPropertyRowMapper<>(Activity.class), LocalDateTime.now());
    }

    private String n(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
