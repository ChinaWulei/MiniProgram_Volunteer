package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.vo.ActivityDetailVO;

import java.util.List;

public interface ActivityService {
    List<Activity> list(String category, String status, String keyword);
    Activity detail(Long id);
    ActivityDetailVO detail(Long id, CurrentUser currentUser);
    Long create(ActivityDTO dto, CurrentUser currentUser);
    void update(Long id, ActivityDTO dto, CurrentUser currentUser);
    void delete(Long id, CurrentUser currentUser);
    void finish(Long id, CurrentUser currentUser);
    String summary(Long id, CurrentUser currentUser);
}
