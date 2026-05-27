package com.scs.volunteer.mapper;

import com.scs.volunteer.entity.RuleFile;
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
public class RuleFileMapper {
    private final JdbcTemplate jdbcTemplate;

    public RuleFileMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(RuleFile file) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into rule_file(original_name,file_type,file_size,s3_key,s3_url,status,chunk_count,created_by)
                    values(?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, file.getOriginalName());
            ps.setString(2, file.getFileType());
            ps.setLong(3, file.getFileSize());
            ps.setString(4, file.getS3Key());
            ps.setString(5, file.getS3Url());
            ps.setString(6, file.getStatus());
            ps.setInt(7, file.getChunkCount() == null ? 0 : file.getChunkCount());
            ps.setLong(8, file.getCreatedBy());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateStatus(Long id, String status, int chunkCount) {
        jdbcTemplate.update("update rule_file set status=?,chunk_count=? where id=?", status, chunkCount, id);
    }

    public List<RuleFile> list() {
        return jdbcTemplate.query("select * from rule_file order by created_at desc", new BeanPropertyRowMapper<>(RuleFile.class));
    }

    public Optional<RuleFile> findById(Long id) {
        List<RuleFile> list = jdbcTemplate.query("select * from rule_file where id=?", new BeanPropertyRowMapper<>(RuleFile.class), id);
        return list.stream().findFirst();
    }

    public Optional<RuleFile> findByS3Key(String s3Key) {
        List<RuleFile> list = jdbcTemplate.query("select * from rule_file where s3_key=?", new BeanPropertyRowMapper<>(RuleFile.class), s3Key);
        return list.stream().findFirst();
    }
}
