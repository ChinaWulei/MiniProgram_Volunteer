package com.scs.volunteer.mapper;

import com.scs.volunteer.dto.ActivityPriorityRuleDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class ActivityPriorityRuleMapper {
    private static final Set<String> TYPES = Set.of(
            "历史活动", "系别", "校区", "技能", "最低信用分", "最低服务时长"
    );

    private final JdbcTemplate jdbcTemplate;

    public ActivityPriorityRuleMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> list(Long activityId) {
        return jdbcTemplate.queryForList("""
                select id,activity_id as activityId,rule_type as ruleType,
                       rule_value as ruleValue,weight
                from activity_priority_rule
                where activity_id=?
                order by id
                """, activityId);
    }

    @Transactional
    public void replace(Long activityId, List<ActivityPriorityRuleDTO> rules) {
        validate(rules);
        jdbcTemplate.update("delete from activity_priority_rule where activity_id=?", activityId);
        if (rules == null) return;
        for (ActivityPriorityRuleDTO rule : rules) {
            jdbcTemplate.update("""
                    insert into activity_priority_rule(activity_id,rule_type,rule_value,weight)
                    values(?,?,?,?)
                    """, activityId, rule.getRuleType().trim(), rule.getRuleValue().trim(), rule.getWeight());
        }
    }

    public void deleteByActivity(Long activityId) {
        jdbcTemplate.update("delete from activity_priority_rule where activity_id=?", activityId);
    }

    private void validate(List<ActivityPriorityRuleDTO> rules) {
        if (rules == null) return;
        for (ActivityPriorityRuleDTO rule : rules) {
            if (rule == null || rule.getRuleType() == null || !TYPES.contains(rule.getRuleType().trim())) {
                throw new IllegalArgumentException("不支持的优先规则类型");
            }
            if (rule.getRuleValue() == null || rule.getRuleValue().isBlank()) {
                throw new IllegalArgumentException("优先规则条件不能为空");
            }
            if (rule.getWeight() == null || rule.getWeight() < 1 || rule.getWeight() > 100) {
                throw new IllegalArgumentException("优先规则分值应为1至100");
            }
            if (("最低信用分".equals(rule.getRuleType()) || "最低服务时长".equals(rule.getRuleType()))) {
                Double.parseDouble(rule.getRuleValue().trim());
            }
        }
    }
}
