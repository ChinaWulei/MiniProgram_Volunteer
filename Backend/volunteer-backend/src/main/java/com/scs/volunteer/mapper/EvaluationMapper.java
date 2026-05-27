package com.scs.volunteer.mapper;

import com.scs.volunteer.dto.ActivityEvaluationDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class EvaluationMapper {
    private final JdbcTemplate jdbcTemplate;

    public EvaluationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(Long activityId, Long evaluatorId, String targetType, Long targetUserId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from activity_evaluation
                where activity_id=? and evaluator_id=? and target_type=? and coalesce(target_user_id,0)=coalesce(?,0)
                """, Integer.class, activityId, evaluatorId, targetType, targetUserId);
        return count != null && count > 0;
    }

    public Long insert(Long activityId, Long evaluatorId, ActivityEvaluationDTO dto) {
        jdbcTemplate.update("""
                insert into activity_evaluation(activity_id,evaluator_id,target_user_id,target_type,score,content,created_at)
                values(?,?,?,?,?,?,?)
                """, activityId, evaluatorId, dto.getTargetUserId(), dto.getTargetType(), dto.getScore(), dto.getContent(), LocalDateTime.now());
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    public List<Map<String, Object>> byActivity(Long activityId) {
        return jdbcTemplate.queryForList("""
                select e.*,u.name as evaluatorName,tu.name as targetUserName
                from activity_evaluation e
                left join user u on u.id=e.evaluator_id
                left join user tu on tu.id=e.target_user_id
                where e.activity_id=?
                order by e.created_at desc
                """, activityId);
    }

    public List<Map<String, Object>> byVolunteer(Long userId) {
        return jdbcTemplate.queryForList("""
                select e.score,e.content,e.created_at,a.name as activityName
                from activity_evaluation e
                join activity a on a.id=e.activity_id
                where e.target_type='VOLUNTEER' and e.target_user_id=?
                order by e.created_at desc
                limit 10
                """, userId);
    }
}
