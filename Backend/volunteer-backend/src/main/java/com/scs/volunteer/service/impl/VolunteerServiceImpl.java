package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.mapper.VolunteerMapper;
import com.scs.volunteer.service.VolunteerService;
import com.scs.volunteer.vo.VolunteerVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VolunteerServiceImpl implements VolunteerService {
    private final VolunteerMapper volunteerMapper;

    public VolunteerServiceImpl(VolunteerMapper volunteerMapper) {
        this.volunteerMapper = volunteerMapper;
    }

    @Override
    public List<VolunteerVO> list(String college, String majorClass, String skillTag, String keyword, String sortBy) {
        return volunteerMapper.search(college, majorClass, skillTag, keyword, sortBy);
    }

    @Override
    public VolunteerVO detail(Long userId) {
        VolunteerVO vo = volunteerMapper.findByUserId(userId).orElseThrow(() -> new BizException("志愿者不存在"));
        vo.setHistoryActivities(volunteerMapper.historyActivities(userId));
        return vo;
    }
}
