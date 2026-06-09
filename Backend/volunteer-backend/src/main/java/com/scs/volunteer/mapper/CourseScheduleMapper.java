package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Repository
public class CourseScheduleMapper {
    private final JdbcTemplate jdbcTemplate;

    public CourseScheduleMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> list(Long userId) {
        return jdbcTemplate.queryForList("""
                select id,course_name as courseName,weekday,
                       date_format(start_time,'%H:%i') as startTime,
                       date_format(end_time,'%H:%i') as endTime,location
                from course_schedule
                where user_id=?
                order by weekday,start_time
                """, userId);
    }

    @Transactional
    public void replace(Long userId, List<Map<String, Object>> courses) {
        jdbcTemplate.update("delete from course_schedule where user_id=?", userId);
        if (courses == null) return;
        for (Map<String, Object> course : courses) {
            String courseName = text(course.get("courseName"));
            int weekday = intValue(course.get("weekday"));
            LocalTime startTime = LocalTime.parse(text(course.get("startTime")));
            LocalTime endTime = LocalTime.parse(text(course.get("endTime")));
            if (courseName.isBlank()) throw new IllegalArgumentException("课程名称不能为空");
            if (weekday < 1 || weekday > 7) throw new IllegalArgumentException("星期必须为1至7");
            if (!endTime.isAfter(startTime)) throw new IllegalArgumentException("课程结束时间必须晚于开始时间");
            jdbcTemplate.update("""
                    insert into course_schedule(user_id,course_name,weekday,start_time,end_time,location)
                    values(?,?,?,?,?,?)
                    """, userId, courseName, weekday, startTime, endTime,
                    text(course.get("location")));
        }
    }

    public List<Map<String, Object>> conflicts(Long userId, LocalDateTime start, LocalDateTime end) {
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            LocalDateTime cursor = start;
            while (!cursor.toLocalDate().isAfter(end.toLocalDate())) {
                LocalTime dayStart = cursor.toLocalDate().equals(start.toLocalDate()) ? start.toLocalTime() : LocalTime.MIN;
                LocalTime dayEnd = cursor.toLocalDate().equals(end.toLocalDate()) ? end.toLocalTime() : LocalTime.MAX;
                result.addAll(conflictsOnDay(userId, cursor.getDayOfWeek().getValue(), dayStart, dayEnd));
                cursor = cursor.plusDays(1).toLocalDate().atStartOfDay();
            }
            return result;
        }
        return conflictsOnDay(userId, start.getDayOfWeek().getValue(), start.toLocalTime(), end.toLocalTime());
    }

    private List<Map<String, Object>> conflictsOnDay(Long userId, int weekday, LocalTime start, LocalTime end) {
        return jdbcTemplate.queryForList("""
                select course_name as courseName,weekday,start_time as startTime,end_time as endTime
                from course_schedule
                where user_id=? and weekday=? and start_time<? and end_time>?
                """, userId, weekday, end, start);
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(text(value));
    }
}
