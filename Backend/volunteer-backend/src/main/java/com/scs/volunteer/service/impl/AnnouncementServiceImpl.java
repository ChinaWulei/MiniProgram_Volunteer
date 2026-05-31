package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.AnnouncementDTO;
import com.scs.volunteer.entity.RuleFile;
import com.scs.volunteer.mapper.AnnouncementMapper;
import com.scs.volunteer.mapper.NotificationMapper;
import com.scs.volunteer.mapper.RuleFileMapper;
import com.scs.volunteer.mapper.UserMapper;
import com.scs.volunteer.service.AnnouncementService;
import com.scs.volunteer.service.ChunkService;
import com.scs.volunteer.service.EmbeddingService;
import com.scs.volunteer.service.RuleFileService;
import com.scs.volunteer.service.S3StorageService;
import com.scs.volunteer.service.VectorSearchService;
import com.scs.volunteer.vo.AnnouncementVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {
    private static final Logger log = LoggerFactory.getLogger(AnnouncementServiceImpl.class);

    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final RuleFileMapper ruleFileMapper;
    private final S3StorageService s3StorageService;
    private final RuleFileService ruleFileService;
    private final ChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;

    public AnnouncementServiceImpl(AnnouncementMapper announcementMapper, UserMapper userMapper,
                                   NotificationMapper notificationMapper, S3StorageService s3StorageService,
                                   RuleFileService ruleFileService, RuleFileMapper ruleFileMapper,
                                   ChunkService chunkService, EmbeddingService embeddingService,
                                   VectorSearchService vectorSearchService) {
        this.announcementMapper = announcementMapper;
        this.userMapper = userMapper;
        this.notificationMapper = notificationMapper;
        this.s3StorageService = s3StorageService;
        this.ruleFileService = ruleFileService;
        this.ruleFileMapper = ruleFileMapper;
        this.chunkService = chunkService;
        this.embeddingService = embeddingService;
        this.vectorSearchService = vectorSearchService;
    }

    @Override
    public List<String> uploadImages(MultipartFile[] files, CurrentUser currentUser) {
        requireAdmin(currentUser);
        if (files == null || files.length == 0) throw new BizException("please choose announcement images");
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(s3StorageService.uploadActivityNewsImage(file));
        }
        return urls;
    }

    @Override
    public RuleFile uploadAttachment(MultipartFile file, String originalName, CurrentUser currentUser) {
        requireAdmin(currentUser);
        return ruleFileService.upload(file, currentUser, originalName);
    }

    @Override
    public Long save(AnnouncementDTO dto, CurrentUser currentUser) {
        requireAdmin(currentUser);
        if (dto == null) throw new BizException("announcement content is required");
        if (dto.getTitle() == null || dto.getTitle().isBlank()) throw new BizException("announcement title is required");
        if (dto.getContent() == null || dto.getContent().isBlank()) throw new BizException("announcement body is required");
        return announcementMapper.save(dto, currentUser.getId());
    }

    @Override
    public void publish(Long id, CurrentUser currentUser) {
        requireAdmin(currentUser);
        AnnouncementVO announcement = announcementMapper.find(id).orElseThrow(() -> new BizException("announcement not found"));
        announcementMapper.publish(id);
        indexAnnouncementContent(id, announcement);
        for (Long userId : userMapper.volunteerIds()) {
            notificationMapper.insert(userId, "ANNOUNCEMENT", "new announcement",
                    "A new announcement was published: " + announcement.getTitle(),
                    "ANNOUNCEMENT", id);
        }
    }

    @Override
    public List<AnnouncementVO> adminList(CurrentUser currentUser) {
        requireAdmin(currentUser);
        return fill(announcementMapper.adminList());
    }

    @Override
    public List<AnnouncementVO> published() {
        return fill(announcementMapper.published());
    }

    @Override
    public AnnouncementVO detail(Long id, CurrentUser currentUser) {
        AnnouncementVO vo;
        if (currentUser != null && "ADMIN".equals(currentUser.getRole())) {
            vo = announcementMapper.find(id).orElseThrow(() -> new BizException("announcement not found"));
        } else {
            vo = announcementMapper.findPublished(id).orElseThrow(() -> new BizException("announcement not found"));
        }
        fill(vo);
        return vo;
    }

    @Override
    public void delete(Long id, CurrentUser currentUser) {
        requireAdmin(currentUser);
        announcementMapper.find(id).orElseThrow(() -> new BizException("announcement not found"));
        ruleFileMapper.findByS3Key("announcement-content/" + id).ifPresent(file -> {
            vectorSearchService.deleteChunks(file.getId());
            ruleFileMapper.delete(file.getId());
        });
        announcementMapper.delete(id);
    }

    @Override
    public Map<String, Object> attachmentResult(RuleFile file) {
        return Map.of(
                "id", file.getId(),
                "fileName", file.getOriginalName(),
                "fileType", file.getFileType(),
                "url", file.getS3Url(),
                "status", file.getStatus(),
                "chunkCount", file.getChunkCount()
        );
    }

    private List<AnnouncementVO> fill(List<AnnouncementVO> list) {
        for (AnnouncementVO vo : list) fill(vo);
        return list;
    }

    private void fill(AnnouncementVO vo) {
        vo.setImageUrls(announcementMapper.imageUrls(vo.getId()));
        vo.setAttachments(announcementMapper.attachments(vo.getId()));
    }

    private void indexAnnouncementContent(Long id, AnnouncementVO announcement) {
        String text = (announcement.getTitle() == null ? "" : announcement.getTitle()) + "\n\n"
                + (announcement.getContent() == null ? "" : announcement.getContent());
        if (text.isBlank()) return;
        String sourceName = "Announcement: " + announcement.getTitle();
        String sourceKey = "announcement-content/" + id;
        try {
            RuleFile file = ruleFileMapper.findByS3Key(sourceKey).orElseGet(() -> {
                RuleFile created = new RuleFile();
                created.setOriginalName(sourceName);
                created.setFileType("txt");
                created.setFileSize((long) text.getBytes(StandardCharsets.UTF_8).length);
                created.setS3Key(sourceKey);
                created.setS3Url("announcement://" + id);
                created.setStatus("PROCESSING");
                created.setChunkCount(0);
                created.setCreatedBy(announcement.getCreatedBy());
                created.setId(ruleFileMapper.insert(created));
                return created;
            });
            List<String> chunks = chunkService.split(text);
            List<float[]> embeddings = new ArrayList<>();
            for (String chunk : chunks) {
                embeddings.add(embeddingService.embed(chunk));
            }
            vectorSearchService.replaceChunks(file.getId(), sourceName, chunks, embeddings);
            ruleFileMapper.updateStatus(file.getId(), "READY", chunks.size());
        } catch (Exception e) {
            log.warn("Announcement content RAG indexing failed, announcementId={}: {}", id, e.getMessage());
        }
    }

    private void requireAdmin(CurrentUser user) {
        if (user == null || !"ADMIN".equals(user.getRole())) throw new BizException("admin only");
    }
}
