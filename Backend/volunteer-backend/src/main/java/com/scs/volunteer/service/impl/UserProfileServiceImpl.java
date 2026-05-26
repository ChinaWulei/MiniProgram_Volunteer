package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.dto.UserProfileDTO;
import com.scs.volunteer.mapper.CreditMapper;
import com.scs.volunteer.mapper.UserMapper;
import com.scs.volunteer.service.S3StorageService;
import com.scs.volunteer.service.UserProfileService;
import com.scs.volunteer.vo.UserProfileVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    private final UserMapper userMapper;
    private final S3StorageService s3StorageService;
    private final CreditMapper creditMapper;

    public UserProfileServiceImpl(UserMapper userMapper, S3StorageService s3StorageService, CreditMapper creditMapper) {
        this.userMapper = userMapper;
        this.s3StorageService = s3StorageService;
        this.creditMapper = creditMapper;
    }

    @Override
    public UserProfileVO profile(Long userId) {
        UserProfileVO profile = userMapper.findProfile(userId).orElseThrow(() -> new BizException("用户不存在"));
        fillVolunteerGrowth(profile, userMapper.registrationCount(userId), userMapper.campusRank(userId));
        return profile;
    }

    @Override
    public UserProfileVO update(Long userId, UserProfileDTO dto) {
        userMapper.updateProfile(userId, dto);
        return profile(userId);
    }

    @Override
    public String updateAvatar(Long userId, MultipartFile file) {
        String avatarUrl = s3StorageService.uploadAvatar(file, userId);
        userMapper.updateAvatar(userId, avatarUrl);
        return avatarUrl;
    }

    private void fillVolunteerGrowth(UserProfileVO profile, int registrationCount, int campusRank) {
        double hours = profile.getTotalHours() == null ? 0 : profile.getTotalHours();
        int serviceCount = profile.getServiceCount() == null ? 0 : profile.getServiceCount();
        int creditScore = profile.getCreditScore() == null ? 100 : profile.getCreditScore();
        profile.setTotalHours(hours);
        profile.setServiceCount(serviceCount);
        profile.setCreditScore(creditScore);
        profile.setCreditLevel(creditLevel(creditScore));
        profile.setCreditRecords(creditMapper.records(profile.getUserId()));

        profile.setVolunteerPoints((int) Math.floor(hours * 10));
        profile.setCampusRank(campusRank);

        if (hours >= 60) {
            setLevel(profile, "Lv4", "卓越志愿者", 60, null, 100);
        } else if (hours >= 30) {
            setLevel(profile, "Lv3", "星光志愿者", 30, 60.0, progress(hours, 30, 60));
        } else if (hours >= 10) {
            setLevel(profile, "Lv2", "活跃志愿者", 10, 30.0, progress(hours, 10, 30));
        } else {
            setLevel(profile, "Lv1", "新星志愿者", 0, 10.0, progress(hours, 0, 10));
        }

        List<String> badges = new ArrayList<>();
        if (registrationCount > 0) {
            badges.add("首次报名");
        }
        if (serviceCount >= 3) {
            badges.add("连续服务");
        }
        if (hours >= 30) {
            badges.add("累计30小时");
        }
        if (creditScore >= 95 && serviceCount >= 5) {
            badges.add("优秀志愿者");
        }
        profile.setBadges(badges);
    }

    private String creditLevel(int score) {
        if (score >= 90) return "优秀";
        if (score >= 80) return "良好";
        if (score >= 70) return "关注";
        return "受限";
    }

    private void setLevel(UserProfileVO profile, String level, String name, double minHours, Double nextHours, int progress) {
        profile.setVolunteerLevel(level);
        profile.setLevelName(name);
        profile.setLevelMinHours(minHours);
        profile.setNextLevelHours(nextHours);
        profile.setNextLevelRemainingHours(nextHours == null ? 0 : Math.max(0, Math.round((nextHours - profile.getTotalHours()) * 10) / 10.0));
        profile.setLevelProgress(progress);
    }

    private int progress(double hours, double min, double next) {
        return Math.max(0, Math.min(100, (int) Math.floor((hours - min) * 100 / (next - min))));
    }
}
