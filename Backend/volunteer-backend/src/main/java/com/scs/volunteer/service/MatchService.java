package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.vo.MatchVO;

import java.util.List;

public interface MatchService {
    List<MatchVO> top5(Long activityId, CurrentUser currentUser);
}
