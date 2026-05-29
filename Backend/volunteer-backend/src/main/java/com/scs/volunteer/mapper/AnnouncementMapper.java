package com.scs.volunteer.mapper;

import com.scs.volunteer.dto.AnnouncementDTO;
import com.scs.volunteer.vo.AnnouncementVO;
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
public class AnnouncementMapper {
    private final JdbcTemplate jdbcTemplate;

    public AnnouncementMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long save(AnnouncementDTO dto, Long createdBy) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into announcement(title,content,status,created_by,published_at)
                    values(?,?,?,?,if(?='PUBLISHED',now(),null))
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, dto.getTitle());
            ps.setString(2, dto.getContent());
            ps.setString(3, dto.getStatus() == null ? "DRAFT" : dto.getStatus());
            ps.setLong(4, createdBy);
            ps.setString(5, dto.getStatus() == null ? "DRAFT" : dto.getStatus());
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        replaceImages(id, dto.getImageUrls());
        replaceAttachments(id, dto.getRuleFileIds());
        return id;
    }

    public void publish(Long id) {
        jdbcTemplate.update("update announcement set status='PUBLISHED',published_at=coalesce(published_at,now()) where id=?", id);
    }

    public List<AnnouncementVO> published() {
        return jdbcTemplate.query("""
                select * from announcement
                where status='PUBLISHED'
                order by published_at desc,created_at desc
                """, new BeanPropertyRowMapper<>(AnnouncementVO.class));
    }

    public List<AnnouncementVO> adminList() {
        return jdbcTemplate.query("select * from announcement order by created_at desc", new BeanPropertyRowMapper<>(AnnouncementVO.class));
    }

    public Optional<AnnouncementVO> find(Long id) {
        List<AnnouncementVO> list = jdbcTemplate.query("select * from announcement where id=?", new BeanPropertyRowMapper<>(AnnouncementVO.class), id);
        return list.stream().findFirst();
    }

    public Optional<AnnouncementVO> findPublished(Long id) {
        List<AnnouncementVO> list = jdbcTemplate.query("select * from announcement where id=? and status='PUBLISHED'",
                new BeanPropertyRowMapper<>(AnnouncementVO.class), id);
        return list.stream().findFirst();
    }

    public List<String> imageUrls(Long id) {
        return jdbcTemplate.queryForList("select image_url from announcement_image where announcement_id=? order by sort_order,id", String.class, id);
    }

    public List<AnnouncementVO.Attachment> attachments(Long id) {
        return jdbcTemplate.query("""
                select a.rule_file_id,f.original_name as file_name,f.file_type,f.s3_url as url
                from announcement_attachment a join rule_file f on a.rule_file_id=f.id
                where a.announcement_id=?
                order by a.id
                """, new BeanPropertyRowMapper<>(AnnouncementVO.Attachment.class), id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from announcement_image where announcement_id=?", id);
        jdbcTemplate.update("delete from announcement_attachment where announcement_id=?", id);
        jdbcTemplate.update("delete from announcement where id=?", id);
    }

    private void replaceImages(Long id, List<String> imageUrls) {
        jdbcTemplate.update("delete from announcement_image where announcement_id=?", id);
        if (imageUrls == null) return;
        for (int i = 0; i < imageUrls.size(); i++) {
            jdbcTemplate.update("insert into announcement_image(announcement_id,image_url,sort_order) values(?,?,?)", id, imageUrls.get(i), i);
        }
    }

    private void replaceAttachments(Long id, List<Long> ruleFileIds) {
        jdbcTemplate.update("delete from announcement_attachment where announcement_id=?", id);
        if (ruleFileIds == null) return;
        for (Long ruleFileId : ruleFileIds) {
            jdbcTemplate.update("insert into announcement_attachment(announcement_id,rule_file_id) values(?,?)", id, ruleFileId);
        }
    }
}
