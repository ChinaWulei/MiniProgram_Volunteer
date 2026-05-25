package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityNewsDTO;
import com.scs.volunteer.vo.ActivityNewsVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ActivityNewsService {
    List<String> uploadImages(MultipartFile[] files, CurrentUser currentUser);
    Map<String, String> generate(Long activityId, CurrentUser currentUser);
    Long save(ActivityNewsDTO dto, CurrentUser currentUser);
    void publish(Long newsId, CurrentUser currentUser);
    List<ActivityNewsVO> published();
    ActivityNewsVO detail(Long newsId, CurrentUser currentUser);
    List<ActivityNewsVO> adminList(CurrentUser currentUser);
}
