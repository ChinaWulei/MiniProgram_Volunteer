package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
import com.scs.volunteer.mapper.VolunteerMapper;
import com.scs.volunteer.service.ActivityService;
import com.scs.volunteer.vo.ActivityDetailVO;
import com.scs.volunteer.vo.VolunteerVO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {
    private static final DateTimeFormatter FORM_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final VolunteerMapper volunteerMapper;

    public ActivityServiceImpl(ActivityMapper activityMapper, RegistrationMapper registrationMapper, VolunteerMapper volunteerMapper) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.volunteerMapper = volunteerMapper;
    }

    @Override
    public List<Activity> list(String category, String status, String keyword) {
        activityMapper.refreshLifecycleStatus();
        return activityMapper.search(category, status, keyword);
    }

    @Override
    public List<Activity> recommend(CurrentUser currentUser) {
        if (currentUser == null || !"VOLUNTEER".equals(currentUser.getRole())) {
            return list(null, null, null).stream().limit(4).toList();
        }
        VolunteerVO profile = volunteerMapper.findByUserId(currentUser.getId()).orElse(null);
        return activityMapper.openActivities().stream()
                .sorted((a, b) -> Double.compare(score(b, profile), score(a, profile)))
                .limit(6)
                .toList();
    }

    @Override
    public Activity detail(Long id) {
        activityMapper.refreshLifecycleStatus();
        return activityMapper.findById(id).orElseThrow(() -> new BizException("活动不存在"));
    }

    @Override
    public ActivityDetailVO detail(Long id, CurrentUser currentUser) {
        Activity activity = detail(id);
        ActivityDetailVO vo = new ActivityDetailVO();
        vo.setId(activity.getId());
        vo.setName(activity.getName());
        vo.setTitle(activity.getName());
        vo.setCoverImageUrl(activity.getCoverImageUrl());
        vo.setCategory(activity.getCategory());
        vo.setLocation(activity.getLocation());
        vo.setLatitude(activity.getLatitude());
        vo.setLongitude(activity.getLongitude());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setSignupStartTime(activity.getSignupStartTime());
        vo.setSignupDeadline(activity.getSignupDeadline());
        vo.setCheckinStartTime(activity.getCheckinStartTime());
        vo.setCheckinEndTime(activity.getCheckinEndTime());
        vo.setRecruitNumber(activity.getRecruitNumber());
        vo.setRegisteredNumber(activity.getRegisteredNumber());
        vo.setRemainingNumber(Math.max(0, activity.getRecruitNumber() - activity.getRegisteredNumber()));
        vo.setSkillRequirements(activity.getSkillRequirements());
        vo.setDescription(activity.getDescription());
        vo.setSignupRequirement(activity.getSignupRequirement());
        vo.setContactName(activity.getContactName());
        vo.setContactPhone(activity.getContactPhone());
        vo.setServiceHours(activity.getServiceHours() == null ? calcHours(activity) : activity.getServiceHours());
        vo.setReviewMethod(activity.getReviewMethod());
        vo.setStatus(activity.getStatus());
        vo.setCreatedBy(activity.getCreatedBy());
        vo.setSignupStatus(currentUser == null ? null : registrationMapper.findStatus(id, currentUser.getId()));
        return vo;
    }

    @Override
    public Long create(ActivityDTO dto, CurrentUser currentUser) {
        requireAdmin(currentUser);
        Activity a = toEntity(dto);
        a.setCreatedBy(currentUser.getId());
        return activityMapper.insert(a);
    }

    @Override
    public void update(Long id, ActivityDTO dto, CurrentUser currentUser) {
        requireAdmin(currentUser);
        detail(id);
        activityMapper.update(id, toEntity(dto));
    }

    @Override
    public void delete(Long id, CurrentUser currentUser) {
        requireAdmin(currentUser);
        activityMapper.delete(id);
    }

    @Override
    public void finish(Long id, CurrentUser currentUser) {
        requireAdmin(currentUser);
        detail(id);
        activityMapper.updateStatus(id, "已结束");
    }

    @Override
    public String summary(Long id, CurrentUser currentUser) {
        requireAdmin(currentUser);
        Activity activity = detail(id);
        int participants = activityMapper.participantCount(id);
        double hours = activity.getServiceHours() == null ? calcHours(activity) : activity.getServiceHours();
        double totalHours = Math.round(participants * hours * 10.0) / 10.0;
        String time = FORM_FORMAT.format(activity.getStartTime());
        return "【数计学院志愿服务】" + activity.getName() + "圆满结束\n\n"
                + time + "，" + activity.getName() + "在" + activity.getLocation() + "顺利开展。活动围绕"
                + activity.getCategory() + "服务需求，组织学院志愿者参与现场保障、秩序维护、咨询引导与协同服务。\n\n"
                + "本次活动共有" + participants + "名志愿者参与，累计贡献服务时长约" + totalHours + "小时。志愿者们分工明确、响应及时，以认真负责的态度保障了活动有序推进，展现了数计学院青年志愿者良好的服务意识与专业素养。\n\n"
                + "通过本次志愿服务，活动现场运行效率得到提升，参与师生获得了更顺畅的服务体验，也进一步凝聚了学院内部志愿服务力量。后续，学院将继续完善志愿服务组织机制，鼓励更多同学在实践中成长、在服务中贡献力量。";
    }

    private Activity toEntity(ActivityDTO dto) {
        validate(dto);
        Activity a = new Activity();
        String name = first(dto.getName(), dto.getTitle());
        Double serviceHours = dto.getServiceHours();
        LocalDateTime startTime = parseDateTime(first(dto.getStartTime(), dto.getActivityTime()), "活动时间");
        LocalDateTime endTime = dto.getEndTime() == null || dto.getEndTime().isBlank()
                ? startTime.plusMinutes(Math.round((serviceHours == null ? 1D : serviceHours) * 60))
                : parseDateTime(dto.getEndTime(), "结束时间");
        if (serviceHours == null || serviceHours <= 0) {
            serviceHours = Math.max(1.0, Duration.between(startTime, endTime).toMinutes() / 60.0);
        }
        LocalDateTime signupStartTime = blank(dto.getSignupStartTime()) ? null : parseDateTime(dto.getSignupStartTime(), "报名开始时间");
        LocalDateTime signupDeadline = blank(dto.getSignupDeadline()) ? null : parseDateTime(dto.getSignupDeadline(), "报名截止时间");
        Integer recruitNumber = dto.getRecruitNumber() == null ? dto.getRecruitCount() : dto.getRecruitNumber();
        String status = normalizePublishStatus(first(dto.getStatus(), "已发布"), startTime, endTime, signupStartTime, signupDeadline, recruitNumber, 0);

        a.setName(name);
        a.setCoverImageUrl(dto.getCoverImageUrl());
        a.setCategory(dto.getCategory());
        a.setLocation(dto.getLocation());
        a.setLatitude(dto.getLatitude());
        a.setLongitude(dto.getLongitude());
        a.setStartTime(startTime);
        a.setEndTime(endTime);
        a.setSignupStartTime(signupStartTime);
        a.setSignupDeadline(signupDeadline);
        a.setCheckinStartTime(blank(dto.getCheckinStartTime()) ? startTime : parseDateTime(dto.getCheckinStartTime(), "签到开始时间"));
        a.setCheckinEndTime(blank(dto.getCheckinEndTime()) ? endTime : parseDateTime(dto.getCheckinEndTime(), "签到结束时间"));
        a.setRecruitNumber(recruitNumber);
        a.setSkillRequirements(dto.getSkillRequirements() == null && dto.getRequiredSkills() != null ? String.join(",", dto.getRequiredSkills()) : dto.getSkillRequirements());
        a.setDescription(dto.getDescription());
        a.setSignupRequirement(first(dto.getSignupRequirement(), dto.getRequirements()));
        a.setContactName(dto.getContactName());
        a.setContactPhone(dto.getContactPhone());
        a.setServiceHours(serviceHours);
        a.setReviewMethod(normalizeAuditMode(first(dto.getReviewMethod(), dto.getAuditMode())));
        a.setStatus(status);
        a.setPublishedAt("草稿".equals(status) ? null : LocalDateTime.now());
        return a;
    }

    private void validate(ActivityDTO dto) {
        if (dto == null) throw new BizException("活动信息不能为空");
        if (blank(first(dto.getName(), dto.getTitle()))) throw new BizException("活动标题不能为空");
        if (blank(dto.getCategory())) throw new BizException("活动类型不能为空");
        if (blank(first(dto.getStartTime(), dto.getActivityTime()))) throw new BizException("活动时间不能为空");
        if (blank(dto.getLocation())) throw new BizException("活动地点不能为空");
        Integer count = dto.getRecruitNumber() == null ? dto.getRecruitCount() : dto.getRecruitNumber();
        if (count == null || count <= 0) throw new BizException("招募人数必须大于 0");
        if ((dto.getServiceHours() == null || dto.getServiceHours() <= 0) && blank(dto.getEndTime())) throw new BizException("服务时长必须大于 0");
    }

    private LocalDateTime parseDateTime(String value, String label) {
        try {
            String normalized = value.trim();
            if (normalized.contains("T")) {
                if (normalized.length() == 16) normalized += ":00";
                return LocalDateTime.parse(normalized);
            }
            return LocalDateTime.parse(normalized, FORM_FORMAT);
        } catch (Exception e) {
            throw new BizException(label + "格式应为 yyyy-MM-dd HH:mm");
        }
    }

    private String normalizeAuditMode(String value) {
        if (blank(value)) return "人工审核";
        if ("管理员审核".equals(value)) return "人工审核";
        return value;
    }

    private String normalizePublishStatus(String status, LocalDateTime startTime, LocalDateTime endTime,
                                          LocalDateTime signupStartTime, LocalDateTime signupDeadline,
                                          Integer recruitNumber, Integer registeredNumber) {
        if ("草稿".equals(status) || "已取消".equals(status) || "已结束".equals(status) || "已满员".equals(status)) {
            return status;
        }
        LocalDateTime now = LocalDateTime.now();
        if (endTime != null && now.isAfter(endTime)) {
            return "已结束";
        }
        int recruit = recruitNumber == null ? 0 : recruitNumber;
        int registered = registeredNumber == null ? 0 : registeredNumber;
        if (recruit > 0 && registered >= recruit) {
            return "已满员";
        }
        LocalDateTime signupStart = signupStartTime == null ? now : signupStartTime;
        LocalDateTime signupEnd = signupDeadline == null ? startTime : signupDeadline;
        if ((now.isEqual(signupStart) || now.isAfter(signupStart)) && (signupEnd == null || now.isBefore(signupEnd) || now.isEqual(signupEnd))) {
            return "报名中";
        }
        return "已发布";
    }

    private String first(String value, String fallback) {
        return blank(value) ? fallback : value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private double calcHours(Activity activity) {
        return Math.max(1.0, Duration.between(activity.getStartTime(), activity.getEndTime()).toMinutes() / 60.0);
    }

    private double score(Activity activity, VolunteerVO volunteer) {
        if (volunteer == null) return activity.getRegisteredNumber() == null ? 0 : activity.getRegisteredNumber();
        double skill = overlap(activity.getSkillRequirements(), volunteer.getSkillTags());
        double time = volunteer.getAvailableTime() != null && timeMatched(activity, volunteer.getAvailableTime()) ? 1 : 0;
        double credit = Math.min(100, volunteer.getCreditScore() == null ? 80 : volunteer.getCreditScore()) / 100.0;
        return skill * 60 + time * 20 + credit * 20;
    }

    private double overlap(String required, String owned) {
        java.util.Set<String> req = split(required);
        java.util.Set<String> own = split(owned);
        if (req.isEmpty()) return 0.5;
        long hit = req.stream().filter(own::contains).count();
        return (double) hit / req.size();
    }

    private java.util.Set<String> split(String tags) {
        if (tags == null || tags.isBlank()) return java.util.Set.of();
        return java.util.Arrays.stream(tags.split("[,;|\\s]+")).filter(s -> !s.isBlank()).collect(java.util.stream.Collectors.toSet());
    }

    private boolean timeMatched(Activity activity, String availableTime) {
        String text = availableTime.toLowerCase();
        int hour = activity.getStartTime().getHour();
        return text.contains("全天") || (hour < 12 && text.contains("上午")) || (hour >= 12 && hour < 18 && text.contains("下午")) || (hour >= 18 && text.contains("晚上"));
    }

    private void requireAdmin(CurrentUser user) {
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BizException("仅管理员可操作");
        }
    }
}
