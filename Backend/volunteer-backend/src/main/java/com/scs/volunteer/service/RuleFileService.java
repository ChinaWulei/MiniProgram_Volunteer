package com.scs.volunteer.service;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.entity.RuleFile;
import com.scs.volunteer.mapper.RuleFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RuleFileService {
    private final RuleFileMapper ruleFileMapper;
    private final S3Service s3Service;
    private final DocumentParseService documentParseService;
    private final ChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;

    public RuleFileService(RuleFileMapper ruleFileMapper, S3Service s3Service, DocumentParseService documentParseService,
                           ChunkService chunkService, EmbeddingService embeddingService, VectorSearchService vectorSearchService) {
        this.ruleFileMapper = ruleFileMapper;
        this.s3Service = s3Service;
        this.documentParseService = documentParseService;
        this.chunkService = chunkService;
        this.embeddingService = embeddingService;
        this.vectorSearchService = vectorSearchService;
    }

    public RuleFile upload(MultipartFile file, CurrentUser user) {
        return upload(file, user, null);
    }

    public RuleFile upload(MultipartFile file, CurrentUser user, String originalName) {
        requireAdmin(user);
        String displayName = displayName(originalName, file.getOriginalFilename());
        Map<String, String> stored = s3Service.uploadRuleFile(file, displayName);
        RuleFile ruleFile = new RuleFile();
        ruleFile.setOriginalName(displayName);
        ruleFile.setFileType(extension(displayName));
        ruleFile.setFileSize(file.getSize());
        ruleFile.setS3Key(stored.get("key"));
        ruleFile.setS3Url(stored.get("url"));
        ruleFile.setStatus("PROCESSING");
        ruleFile.setChunkCount(0);
        ruleFile.setCreatedBy(user.getId());
        Long id = ruleFileMapper.insert(ruleFile);
        ruleFile.setId(id);

        try {
            String text = documentParseService.parse(file, displayName);
            List<String> chunks = chunkService.split(text);
            if (chunks.isEmpty()) throw new BizException("规则文件未解析到有效文本");
            List<float[]> embeddings = new ArrayList<>();
            for (String chunk : chunks) {
                embeddings.add(embeddingService.embed(chunk));
            }
            vectorSearchService.replaceChunks(id, ruleFile.getOriginalName(), chunks, embeddings);
            ruleFileMapper.updateStatus(id, "READY", chunks.size());
            ruleFile.setStatus("READY");
            ruleFile.setChunkCount(chunks.size());
            return ruleFile;
        } catch (Exception e) {
            ruleFileMapper.updateStatus(id, "FAILED", 0);
            throw e;
        }
    }

    public List<RuleFile> list() {
        return ruleFileMapper.list();
    }

    public RuleFile detail(Long id) {
        return ruleFileMapper.findById(id).orElseThrow(() -> new BizException("Rule file not found"));
    }

    public void delete(Long id, CurrentUser user) {
        requireAdmin(user);
        detail(id);
        vectorSearchService.deleteChunks(id);
        ruleFileMapper.deleteAnnouncementRefs(id);
        ruleFileMapper.delete(id);
    }

    private void requireAdmin(CurrentUser user) {
        if (user == null || !"ADMIN".equals(user.getRole())) throw new BizException("Only admins can manage rule files");
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String displayName(String preferred, String fallback) {
        String value = preferred == null || preferred.isBlank() ? fallback : preferred;
        if (value == null) return "";
        value = value.replace("\\", "/");
        int slash = value.lastIndexOf('/');
        if (slash >= 0) value = value.substring(slash + 1);
        return value.trim();
    }
}
