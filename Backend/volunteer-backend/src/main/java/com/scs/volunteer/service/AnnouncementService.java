package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.AnnouncementDTO;
import com.scs.volunteer.entity.RuleFile;
import com.scs.volunteer.vo.AnnouncementVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface AnnouncementService {
    List<String> uploadImages(MultipartFile[] files, CurrentUser currentUser);
    RuleFile uploadAttachment(MultipartFile file, String originalName, CurrentUser currentUser);
    Long save(AnnouncementDTO dto, CurrentUser currentUser);
    void publish(Long id, CurrentUser currentUser);
    List<AnnouncementVO> adminList(CurrentUser currentUser);
    List<AnnouncementVO> published();
    AnnouncementVO detail(Long id, CurrentUser currentUser);
    void delete(Long id, CurrentUser currentUser);
    Map<String, Object> attachmentResult(RuleFile file);
}
