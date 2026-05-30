package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;

import java.util.Map;

public interface StatisticsService {
    Map<String, Object> overview(CurrentUser currentUser);
    Map<String, Object> overview(CurrentUser currentUser, String startDate, String endDate);
}
