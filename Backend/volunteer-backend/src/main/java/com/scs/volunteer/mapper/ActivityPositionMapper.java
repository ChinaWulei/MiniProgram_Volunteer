package com.scs.volunteer.mapper;

import com.scs.volunteer.dto.ActivityPositionDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class ActivityPositionMapper {
    private final JdbcTemplate jdbcTemplate;

    public ActivityPositionMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> list(Long activityId) {
        return jdbcTemplate.queryForList("""
                select p.id,p.activity_id as activityId,p.name,p.start_time as startTime,p.end_time as endTime,
                       p.recruit_number as recruitNumber,p.requirements,p.requires_rehearsal as requiresRehearsal,
                       p.rehearsal_start_time as rehearsalStartTime,p.rehearsal_end_time as rehearsalEndTime,
                       greatest(p.recruit_number-count(r.id),0) as remainingNumber
                from activity_position p
                left join registration r on r.position_id=p.id and r.status in ('待审核','已通过','已完成')
                where p.activity_id=?
                group by p.id
                order by p.sort_order,p.start_time
                """, activityId);
    }

    public Map<String, Object> find(Long id, Long activityId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select p.*,greatest(p.recruit_number-count(r.id),0) as remaining_number
                from activity_position p
                left join registration r on r.position_id=p.id and r.status in ('待审核','已通过','已完成')
                where p.id=? and p.activity_id=?
                group by p.id
                """, id, activityId);
        return rows.stream().findFirst().orElse(null);
    }

    public void replace(Long activityId, List<ActivityPositionDTO> positions,
                        java.util.function.Function<String, LocalDateTime> parser) {
        if (positions == null) return;
        List<Long> retainedIds = new java.util.ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            ActivityPositionDTO item = positions.get(i);
            if (item.getId() != null) {
                jdbcTemplate.update("""
                        update activity_position
                        set name=?,start_time=?,end_time=?,recruit_number=?,requirements=?,requires_rehearsal=?,
                            rehearsal_start_time=?,rehearsal_end_time=?,sort_order=?
                        where id=? and activity_id=?
                        """, item.getName(), parser.apply(item.getStartTime()), parser.apply(item.getEndTime()),
                        item.getRecruitNumber(), item.getRequirements(), Boolean.TRUE.equals(item.getRequiresRehearsal()),
                        optionalTime(item.getRehearsalStartTime(), parser), optionalTime(item.getRehearsalEndTime(), parser),
                        i, item.getId(), activityId);
                retainedIds.add(item.getId());
            } else {
                org.springframework.jdbc.support.GeneratedKeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
                int sortOrder = i;
                jdbcTemplate.update(connection -> {
                    java.sql.PreparedStatement statement = connection.prepareStatement("""
                            insert into activity_position(activity_id,name,start_time,end_time,recruit_number,requirements,
                                                          requires_rehearsal,rehearsal_start_time,rehearsal_end_time,sort_order)
                            values(?,?,?,?,?,?,?,?,?,?)
                            """, java.sql.Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, activityId);
                    statement.setString(2, item.getName());
                    statement.setObject(3, parser.apply(item.getStartTime()));
                    statement.setObject(4, parser.apply(item.getEndTime()));
                    statement.setInt(5, item.getRecruitNumber());
                    statement.setString(6, item.getRequirements());
                    statement.setBoolean(7, Boolean.TRUE.equals(item.getRequiresRehearsal()));
                    statement.setObject(8, optionalTime(item.getRehearsalStartTime(), parser));
                    statement.setObject(9, optionalTime(item.getRehearsalEndTime(), parser));
                    statement.setInt(10, sortOrder);
                    return statement;
                }, keyHolder);
                if (keyHolder.getKey() != null) retainedIds.add(keyHolder.getKey().longValue());
            }
        }
        if (retainedIds.isEmpty()) {
            jdbcTemplate.update("""
                    delete p from activity_position p
                    where p.activity_id=?
                      and not exists(select 1 from registration r where r.position_id=p.id)
                    """, activityId);
        } else {
            String placeholders = retainedIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
            List<Object> args = new java.util.ArrayList<>();
            args.add(activityId);
            args.addAll(retainedIds);
            jdbcTemplate.update("""
                    delete p from activity_position p
                    where p.activity_id=?
                      and p.id not in (%s)
                      and not exists(select 1 from registration r where r.position_id=p.id)
                    """.formatted(placeholders), args.toArray());
        }
    }

    private LocalDateTime optionalTime(String value, java.util.function.Function<String, LocalDateTime> parser) {
        return value == null || value.isBlank() ? null : parser.apply(value);
    }
}
