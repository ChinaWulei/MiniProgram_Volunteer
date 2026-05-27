package com.scs.volunteer.mapper;

import com.scs.volunteer.vo.RuleChunkVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RuleVectorMapper {
    private final JdbcTemplate jdbcTemplate;

    public RuleVectorMapper(@Qualifier("vectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteByFileId(Long ruleFileId) {
        jdbcTemplate.update("delete from rule_file_chunk where rule_file_id=?", ruleFileId);
    }

    public void insert(Long ruleFileId, String fileName, int chunkIndex, String content, float[] embedding) {
        jdbcTemplate.update("""
                insert into rule_file_chunk(rule_file_id,file_name,chunk_index,content,embedding)
                values(?,?,?,?,?::vector)
                """, ruleFileId, fileName, chunkIndex, content, vectorLiteral(embedding));
    }

    public List<RuleChunkVO> search(float[] embedding, int topK) {
        return jdbcTemplate.query("""
                select rule_file_id,file_name,chunk_index,content,
                       embedding <=> ?::vector as distance
                from rule_file_chunk
                order by embedding <=> ?::vector
                limit ?
                """, new BeanPropertyRowMapper<>(RuleChunkVO.class), vectorLiteral(embedding), vectorLiteral(embedding), topK);
    }

    private String vectorLiteral(float[] values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(values[i]);
        }
        return builder.append(']').toString();
    }
}
