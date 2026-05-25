package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.VolunteerMapper;
import com.scs.volunteer.service.MatchService;
import com.scs.volunteer.vo.MatchVO;
import com.scs.volunteer.vo.VolunteerVO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatchServiceImpl implements MatchService {
    private final ActivityMapper activityMapper;
    private final VolunteerMapper volunteerMapper;

    public MatchServiceImpl(ActivityMapper activityMapper, VolunteerMapper volunteerMapper) {
        this.activityMapper = activityMapper;
        this.volunteerMapper = volunteerMapper;
    }

    @Override
    public List<MatchVO> top5(Long activityId, CurrentUser currentUser) {
        if (!"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可查看智能匹配");
        }
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        return volunteerMapper.search(null, null, null, null, "points").stream()
                .map(v -> score(activity, v))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(5)
                .toList();
    }

    private MatchVO score(Activity activity, VolunteerVO volunteer) {
        Set<String> req = split(activity.getSkillRequirements());
        Set<String> own = split(volunteer.getSkillTags());
        long hit = req.stream().filter(own::contains).count();
        double skillScore = req.isEmpty() ? 1 : (double) hit / req.size();
        double timeScore = timeMatched(activity, volunteer.getAvailableTime()) ? 1 : 0;
        double creditScore = Math.min(100, volunteer.getCreditScore() == null ? 80 : volunteer.getCreditScore()) / 100.0;
        double total = Math.round((skillScore * 60 + timeScore * 20 + creditScore * 20) * 10.0) / 10.0;
        String reason = "技能匹配" + hit + "/" + req.size()
                + "，时间" + (timeScore > 0 ? "可服务" : "需确认")
                + "，信用评分" + volunteer.getCreditScore();
        return new MatchVO(volunteer, total, reason);
    }

    private Set<String> split(String tags) {
        if (tags == null || tags.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(tags.split("[,，、\\s]+")).filter(s -> !s.isBlank()).collect(Collectors.toSet());
    }

    private boolean timeMatched(Activity activity, String availableTime) {
        if (availableTime == null) {
            return false;
        }
        String text = availableTime.toLowerCase();
        int hour = activity.getStartTime().getHour();
        return text.contains("全天") || (hour < 12 && text.contains("上午")) || (hour >= 12 && hour < 18 && text.contains("下午")) || (hour >= 18 && text.contains("晚上"));
    }
}
