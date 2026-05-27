package com.scs.volunteer.service;

import com.scs.volunteer.mapper.RuleVectorMapper;
import com.scs.volunteer.vo.RuleChunkVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSearchService {
    private final RuleVectorMapper ruleVectorMapper;

    public VectorSearchService(RuleVectorMapper ruleVectorMapper) {
        this.ruleVectorMapper = ruleVectorMapper;
    }

    public void replaceChunks(Long fileId, String fileName, List<String> chunks, List<float[]> embeddings) {
        ruleVectorMapper.deleteByFileId(fileId);
        for (int i = 0; i < chunks.size(); i++) {
            ruleVectorMapper.insert(fileId, fileName, i, chunks.get(i), embeddings.get(i));
        }
    }

    public List<RuleChunkVO> search(float[] questionEmbedding, int topK) {
        return ruleVectorMapper.search(questionEmbedding, topK);
    }
}
