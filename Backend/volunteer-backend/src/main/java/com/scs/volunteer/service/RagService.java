package com.scs.volunteer.service;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.config.RagProperties;
import com.scs.volunteer.vo.RagAnswerVO;
import com.scs.volunteer.vo.RuleChunkVO;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RagService {
    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;
    private final AiModelClient aiModelClient;
    private final RagProperties properties;

    public RagService(EmbeddingService embeddingService, VectorSearchService vectorSearchService,
                      AiModelClient aiModelClient, RagProperties properties) {
        this.embeddingService = embeddingService;
        this.vectorSearchService = vectorSearchService;
        this.aiModelClient = aiModelClient;
        this.properties = properties;
    }

    public RagAnswerVO answer(String question, Integer topK) {
        if (question == null || question.isBlank()) throw new BizException("请输入问题");
        if (!aiModelClient.available()) throw new BizException("AI 对话模型未配置");
        int limit = topK == null || topK <= 0 ? properties.getTopK() : Math.min(topK, 10);
        float[] questionEmbedding = embeddingService.embed(question);
        List<RuleChunkVO> chunks = vectorSearchService.search(questionEmbedding, limit);
        if (chunks.isEmpty()) {
            return new RagAnswerVO("未在规则文件中检索到相关内容，请换一种问法。", List.of());
        }
        String context = chunks.stream()
                .map(chunk -> "来源文件：" + chunk.getFileName() + "，片段" + chunk.getChunkIndex() + "\n" + chunk.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));
        String prompt = """
                你是学院志愿服务规则问答助手。请只根据给定规则上下文回答问题。
                如果上下文不足以回答，请明确说明“规则文件中未找到明确依据”。
                回答末尾必须列出引用来源文件名，格式：引用：文件A；文件B

                规则上下文：
                %s

                用户问题：%s
                """.formatted(context, question);
        String answer = aiModelClient.chat(prompt);
        Set<String> sources = chunks.stream().map(RuleChunkVO::getFileName).collect(Collectors.toCollection(LinkedHashSet::new));
        return new RagAnswerVO(answer, List.copyOf(sources));
    }
}
