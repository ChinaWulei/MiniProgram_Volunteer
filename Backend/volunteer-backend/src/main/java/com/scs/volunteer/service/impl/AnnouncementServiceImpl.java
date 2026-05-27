package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.AnnouncementDTO;
import com.scs.volunteer.entity.RuleFile;
import com.scs.volunteer.mapper.AnnouncementMapper;
import com.scs.volunteer.mapper.NotificationMapper;
import com.scs.volunteer.mapper.UserMapper;
import com.scs.volunteer.service.AnnouncementService;
import com.scs.volunteer.service.RuleFileService;
import com.scs.volunteer.service.S3StorageService;
import com.scs.volunteer.vo.AnnouncementVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {
    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final S3StorageService s3StorageService;
    private final RuleFileService ruleFileService;

    public AnnouncementServiceImpl(AnnouncementMapper announcementMapper, UserMapper userMapper,
                                   NotificationMapper notificationMapper, S3StorageService s3StorageService,
                                   RuleFileService ruleFileService) {
        this.announcementMapper = announcementMapper;
        this.userMapper = userMapper;
        this.notificationMapper = notificationMapper;
        this.s3StorageService = s3StorageService;
        this.ruleFileService = ruleFileService;
    }

    @Override
    public List<String> uploadImages(MultipartFile[] files, CurrentUser currentUser) {
        requireAdmin(currentUser);
        if (files == null || files.length == 0) throw new BizException("请选择公告图片");
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(s3StorageService.uploadActivityNewsImage(file));
        }
        return urls;
    }

    @Override
    public RuleFile uploadAttachment(MultipartFile file, CurrentUser currentUser) {
        requireAdmin(currentUser);
        return ruleFileService.upload(file, currentUser);
    }

    @Override
    public Long save(AnnouncementDTO dto, CurrentUser currentUser) {
        requireAdmin(currentUser);
        if (dto == null) throw new BizException("公告内容不能为空");
        if (dto.getTitle() == null || dto.getTitle().isBlank()) throw new BizException("公告标题不能为空");
        if (dto.getContent() == null || dto.getContent().isBlank()) throw new BizException("公告正文不能为空");
        return announcementMapper.save(dto, currentUser.getId());
    }

    @Override
    public void publish(Long id, CurrentUser currentUser) {
        requireAdmin(currentUser);
        AnnouncementVO announcement = announcementMapper.find(id).orElseThrow(() -> new BizException("公告不存在"));
        announcementMapper.publish(id);
        for (Long userId : userMapper.volunteerIds()) {
            notificationMapper.insert(userId, "ANNOUNCEMENT", "新公告发布",
                    "管理员发布了公告《" + announcement.getTitle() + "》，点击查看详情。",
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
            vo = announcementMapper.find(id).orElseThrow(() -> new BizException("公告不存在"));
        } else {
            vo = announcementMapper.findPublished(id).orElseThrow(() -> new BizException("公告不存在"));
        }
        fill(vo);
        return vo;
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

    private void requireAdmin(CurrentUser user) {
        if (user == null || !"ADMIN".equals(user.getRole())) throw new BizException("仅管理员可操作");
    }
}
