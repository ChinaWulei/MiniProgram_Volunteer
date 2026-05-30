package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.mapper.StatisticsMapper;
import com.scs.volunteer.service.StatisticsService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsMapper statisticsMapper;

    public StatisticsServiceImpl(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    @Override
    public Map<String, Object> overview(CurrentUser currentUser) {
        return overview(currentUser, null, null);
    }

    @Override
    public Map<String, Object> overview(CurrentUser currentUser, String startDate, String endDate) {
        if (!"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可查看统计");
        }
        return statisticsMapper.overview(startDate, endDate);
    }
}
