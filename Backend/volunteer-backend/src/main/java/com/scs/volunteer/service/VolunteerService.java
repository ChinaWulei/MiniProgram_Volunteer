package com.scs.volunteer.service;

import com.scs.volunteer.vo.VolunteerVO;

import java.util.List;

public interface VolunteerService {
    List<VolunteerVO> list(String college, String majorClass, String skillTag, String keyword, String sortBy);
    VolunteerVO detail(Long userId);
}
