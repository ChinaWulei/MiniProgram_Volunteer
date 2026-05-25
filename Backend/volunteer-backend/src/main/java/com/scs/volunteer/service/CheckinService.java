package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.CheckinRequest;
import com.scs.volunteer.dto.ManualCheckinRequest;
import com.scs.volunteer.vo.CheckinStatusVO;

import java.util.List;
import java.util.Map;

public interface CheckinService {
    CheckinStatusVO checkin(CheckinRequest request, CurrentUser currentUser);
    CheckinStatusVO status(Long activityId, CurrentUser currentUser);
    Map<String, Object> activityStatistics(Long activityId, CurrentUser currentUser);
    List<Map<String, Object>> activityList(Long activityId, String status, String keyword, CurrentUser currentUser);
    void manual(Long activityId, ManualCheckinRequest request, CurrentUser currentUser);
    Map<String, Object> volunteerStatistics(Long userId, CurrentUser currentUser);
}
