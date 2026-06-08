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
                insert into activity_evaluation(activity_id,evaluator_id,target_user_id,target_type,score,content,anonymous,created_at)
                values(?,?,?,?,?,?,?,?)
                """, activityId, evaluatorId, dto.getTargetUserId(), dto.getTargetType(), dto.getScore(),
                dto.getContent(), Boolean.TRUE.equals(dto.getAnonymous()), LocalDateTime.now());
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    public void saveAnalysis(Long id, String overall, String advantages, String problems, String suggestions, String status) {
        jdbcTemplate.update("""
                update activity_evaluation
                set parsed_overall=?,parsed_advantages=?,parsed_problems=?,parsed_suggestions=?,
                    analysis_status=?,analyzed_at=?
                where id=?
                """, overall, advantages, problems, suggestions, status, LocalDateTime.now(), id);
    }

    public List<Map<String, Object>> byActivity(Long activityId) {
        return jdbcTemplate.queryForList("""
                select e.*,if(e.anonymous=1 and u.role='VOLUNTEER','匿名志愿者',u.name) as evaluatorName,
                       if(e.anonymous=1 and u.role='VOLUNTEER',null,u.id) as evaluatorVisibleId,
                       tu.name as targetUserName,a.name as activityName,a.category
                from activity_evaluation e
                left join user u on u.id=e.evaluator_id
                left join user tu on tu.id=e.target_user_id
                join activity a on a.id=e.activity_id
                where e.activity_id=?
                order by e.created_at desc
                """, activityId);
    }

    public List<Map<String, Object>> feedbackByActivity(Long activityId) {
        return jdbcTemplate.queryForList("""
                select e.id,e.activity_id as activityId,
                       if(e.anonymous=1,null,e.evaluator_id) as evaluatorId,
                       e.target_type as targetType,e.score,e.content,e.created_at as createdAt,
                       e.anonymous,e.parsed_overall as parsedOverall,e.parsed_advantages as parsedAdvantages,
                       e.parsed_problems as parsedProblems,e.parsed_suggestions as parsedSuggestions,
                       e.analysis_status as analysisStatus,
                       if(e.anonymous=1,'匿名志愿者',u.name) as evaluatorName,
                       a.name as activityName,a.category
                from activity_evaluation e
                join user u on u.id=e.evaluator_id
                join activity a on a.id=e.activity_id
                where e.activity_id=?
                  and u.role='VOLUNTEER'
                  and e.target_type in ('ACTIVITY','LEADER')
                order by e.created_at desc
                """, activityId);
    }

    public List<Map<String, Object>> my(Long userId) {
        return jdbcTemplate.queryForList("""
                select e.id,e.activity_id as activityId,e.target_type as targetType,e.score,e.content,e.anonymous,
                       e.parsed_overall as parsedOverall,e.parsed_advantages as parsedAdvantages,
                       e.parsed_problems as parsedProblems,e.parsed_suggestions as parsedSuggestions,
                       e.created_at as createdAt,a.name as activityName,a.category
                from activity_evaluation e
                join activity a on a.id=e.activity_id
                where e.evaluator_id=?
                order by e.created_at desc
                """, userId);
    }

    public Map<String, Object> find(Long id) {
        return jdbcTemplate.queryForMap("""
                select e.*,a.name as activityName,a.category,a.id as activityId
                from activity_evaluation e join activity a on a.id=e.activity_id
                where e.id=?
                """, id);
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
