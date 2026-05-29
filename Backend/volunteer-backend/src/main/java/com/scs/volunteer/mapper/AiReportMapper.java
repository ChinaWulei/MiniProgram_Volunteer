package com.scs.volunteer.mapper;

import com.scs.volunteer.entity.AiReport;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class AiReportMapper {
    private final JdbcTemplate jdbcTemplate;

    public AiReportMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(AiReport report) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into ai_report(report_no,report_type,user_id,period_start,period_end,stats_json,ai_analysis,pdf_url)
                    values(?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, report.getReportNo());
            ps.setString(2, report.getReportType());
            ps.setLong(3, report.getUserId());
            ps.setString(4, report.getPeriodStart());
            ps.setString(5, report.getPeriodEnd());
            ps.setString(6, report.getStatsJson());
            ps.setString(7, report.getAiAnalysis());
            ps.setString(8, report.getPdfUrl());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updatePdf(Long id, String pdfUrl) {
        jdbcTemplate.update("update ai_report set pdf_url=? where id=?", pdfUrl, id);
    }

    public Optional<AiReport> find(Long id) {
        List<AiReport> list = jdbcTemplate.query("select * from ai_report where id=?",
                new BeanPropertyRowMapper<>(AiReport.class), id);
        return list.stream().findFirst();
    }

    public List<AiReport> list(Long userId, boolean admin) {
        if (admin) {
            return jdbcTemplate.query("""
                    select * from ai_report
                    where report_type='ADMIN'
                    order by created_at desc
                    """, new BeanPropertyRowMapper<>(AiReport.class));
        }
        return jdbcTemplate.query("""
                select * from ai_report
                where user_id=? and report_type='VOLUNTEER'
                order by created_at desc
                """, new BeanPropertyRowMapper<>(AiReport.class), userId);
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from ai_report where id=?", id);
    }
}
