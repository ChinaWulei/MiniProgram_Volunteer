package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class ExamScheduleMapper {
    private final JdbcTemplate jdbcTemplate;

    public ExamScheduleMapper(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public List<Map<String, Object>> list(Long userId) {
        return jdbcTemplate.queryForList("""
                select id,course_name as courseName,start_time as startTime,end_time as endTime,location
                from exam_schedule where user_id=? order by start_time
                """, userId);
    }

    public void replace(Long userId, List<Map<String, Object>> exams) {
        jdbcTemplate.update("delete from exam_schedule where user_id=?", userId);
        if (exams == null) return;
        for (Map<String, Object> exam : exams) {
            jdbcTemplate.update("""
                    insert into exam_schedule(user_id,course_name,start_time,end_time,location) values(?,?,?,?,?)
                    """, userId, text(exam.get("courseName")), parse(exam.get("startTime")),
                    parse(exam.get("endTime")), text(exam.get("location")));
        }
    }

    public List<Map<String, Object>> conflicts(Long userId, LocalDateTime start, LocalDateTime end) {
        return jdbcTemplate.queryForList("""
                select course_name as courseName,start_time as startTime,end_time as endTime
                from exam_schedule where user_id=? and start_time<? and end_time>?
                """, userId, end, start);
    }

    private LocalDateTime parse(Object value) {
        String text = text(value).replace(" ", "T");
        return LocalDateTime.parse(text.length() == 16 ? text + ":00" : text);
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
}
