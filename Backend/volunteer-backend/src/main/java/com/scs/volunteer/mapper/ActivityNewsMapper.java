package com.scs.volunteer.mapper;

import com.scs.volunteer.dto.ActivityNewsDTO;
import com.scs.volunteer.vo.ActivityNewsVO;
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
public class ActivityNewsMapper {
    private final JdbcTemplate jdbcTemplate;

    public ActivityNewsMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long save(ActivityNewsDTO dto, Long adminId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into activity_news(activity_id,title,content,result_summary,status,created_by)
                    values(?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, dto.getActivityId());
            ps.setString(2, dto.getTitle());
            ps.setString(3, dto.getContent());
            ps.setString(4, dto.getResultSummary());
            ps.setString(5, dto.getStatus() == null || dto.getStatus().isBlank() ? "DRAFT" : dto.getStatus());
            ps.setLong(6, adminId);
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        replaceImages(id, dto.getImageUrls());
        return id;
    }

    public void replaceImages(Long newsId, List<String> imageUrls) {
        jdbcTemplate.update("delete from activity_news_image where news_id=?", newsId);
        if (imageUrls == null) return;
        for (int i = 0; i < imageUrls.size(); i++) {
            jdbcTemplate.update("insert into activity_news_image(news_id,image_url,sort_order) values(?,?,?)", newsId, imageUrls.get(i), i);
        }
    }

    public void publish(Long newsId) {
        jdbcTemplate.update("update activity_news set status='PUBLISHED', published_at=? where id=?", LocalDateTime.now(), newsId);
    }

    public Optional<ActivityNewsVO> find(Long id) {
        List<ActivityNewsVO> list = jdbcTemplate.query(baseSql() + " where n.id=?",
                new BeanPropertyRowMapper<>(ActivityNewsVO.class), id);
        list.forEach(this::attachImages);
        return list.stream().findFirst();
    }

    public List<ActivityNewsVO> published() {
        List<ActivityNewsVO> list = jdbcTemplate.query(baseSql() + " where n.status='PUBLISHED' order by n.published_at desc",
                new BeanPropertyRowMapper<>(ActivityNewsVO.class));
        list.forEach(this::attachImages);
        return list;
    }

    public List<ActivityNewsVO> adminList() {
        List<ActivityNewsVO> list = jdbcTemplate.query(baseSql() + " order by n.updated_at desc",
                new BeanPropertyRowMapper<>(ActivityNewsVO.class));
        list.forEach(this::attachImages);
        return list;
    }

    public void increaseRead(Long id) {
        jdbcTemplate.update("update activity_news set read_count=read_count+1 where id=?", id);
    }

    private void attachImages(ActivityNewsVO vo) {
        vo.setImageUrls(jdbcTemplate.queryForList("select image_url from activity_news_image where news_id=? order by sort_order asc,id asc", String.class, vo.getId()));
    }

    private String baseSql() {
        return """
                select n.id,n.activity_id,a.name as activity_name,
                       concat(date_format(a.start_time,'%Y-%m-%d %H:%i'),' 至 ',date_format(a.end_time,'%Y-%m-%d %H:%i')) as activity_time,
                       a.location,
                       (select count(*) from registration r where r.activity_id=a.id and r.status in ('已通过','已完成')) as participant_count,
                       (select coalesce(count(*) * coalesce(a.service_hours,1),0) from registration r where r.activity_id=a.id and r.status in ('已通过','已完成')) as total_service_hours,
                       n.title,n.content,n.result_summary,n.status,n.read_count,n.published_at
                from activity_news n join activity a on n.activity_id=a.id
                """;
    }
}
