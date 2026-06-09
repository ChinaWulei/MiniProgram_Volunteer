package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.ActivityPositionMapper;
import com.scs.volunteer.mapper.ActivityPriorityRuleMapper;
import com.scs.volunteer.mapper.NotificationMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
import com.scs.volunteer.mapper.VolunteerMapper;
import com.scs.volunteer.service.ActivityService;
import com.scs.volunteer.service.ActivitySubscriptionService;
import com.scs.volunteer.service.WechatMiniProgramService;
import com.scs.volunteer.vo.ActivityDetailVO;
import com.scs.volunteer.vo.VolunteerVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {
    private static final DateTimeFormatter FORM_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ActivityMapper activityMapper;
    private final ActivityPositionMapper activityPositionMapper;
    private final ActivityPriorityRuleMapper activityPriorityRuleMapper;
    private final RegistrationMapper registrationMapper;
    private final VolunteerMapper volunteerMapper;
    private final ActivitySubscriptionService activitySubscriptionService;
    private final NotificationMapper notificationMapper;
    private final WechatMiniProgramService wechatMiniProgramService;

    public ActivityServiceImpl(ActivityMapper activityMapper, ActivityPositionMapper activityPositionMapper,
                               ActivityPriorityRuleMapper activityPriorityRuleMapper,
                               RegistrationMapper registrationMapper, VolunteerMapper volunteerMapper,
                               ActivitySubscriptionService activitySubscriptionService, NotificationMapper notificationMapper,
                               WechatMiniProgramService wechatMiniProgramService) {
        this.activityMapper = activityMapper;
        this.activityPositionMapper = activityPositionMapper;
        this.activityPriorityRuleMapper = activityPriorityRuleMapper;
        this.registrationMapper = registrationMapper;
        this.volunteerMapper = volunteerMapper;
        this.activitySubscriptionService = activitySubscriptionService;
        this.notificationMapper = notificationMapper;
        this.wechatMiniProgramService = wechatMiniProgramService;
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
        vo.setTips(activity.getTips());
        vo.setReviewMethod(activity.getReviewMethod());
        vo.setStatus(activity.getStatus());
        vo.setCreatedBy(activity.getCreatedBy());
        vo.setSignupStatus(currentUser == null ? null : registrationMapper.findStatus(id, currentUser.getId()));
        vo.setPositions(activityPositionMapper.list(id));
        vo.setPriorityRules(activityPriorityRuleMapper.list(id));
        return vo;
    }

    @Override
    @Transactional
    public Long create(ActivityDTO dto, CurrentUser currentUser) {
        requireAdmin(currentUser);
        Activity a = toEntity(dto);
        a.setCreatedBy(currentUser.getId());
        Long id = activityMapper.insert(a);
        if (dto.getPositions() != null) {
            activityPositionMapper.replace(id, dto.getPositions(), value -> parseDateTime(value, "岗位时间"));
        }
        if (dto.getPriorityRules() != null) {
            activityPriorityRuleMapper.replace(id, dto.getPriorityRules());
        }
        a.setId(id);
        if (a.getPublishedAt() != null) {
            activitySubscriptionService.notifyActivityPublished(a);
        }
        return id;
    }

    @Override
    @Transactional
    public void update(Long id, ActivityDTO dto, CurrentUser currentUser) {
        requireAdmin(currentUser);
        detail(id);
        activityMapper.update(id, toEntity(dto));
        if (dto.getPositions() != null) {
            activityPositionMapper.replace(id, dto.getPositions(), value -> parseDateTime(value, "岗位时间"));
        }
        if (dto.getPriorityRules() != null) {
            activityPriorityRuleMapper.replace(id, dto.getPriorityRules());
        }
    }

    @Override
    @Transactional
    public void delete(Long id, CurrentUser currentUser) {
        requireAdmin(currentUser);
        activityPriorityRuleMapper.deleteByActivity(id);
        activityMapper.delete(id);
    }

    @Override
    public void finish(Long id, CurrentUser currentUser) {
        requireAdmin(currentUser);
        Activity activity = detail(id);
        activityMapper.updateStatus(id, "已结束");
        registrationMapper.markActivityCompleted(id, "活动结束，系统开放评价与成长经验填写");
        notifyParticipantsToReflect(activity);
    }

    private void notifyParticipantsToReflect(Activity activity) {
        for (Long userId : activityMapper.participantUserIds(activity.getId())) {
            notificationMapper.insert(userId, "ACTIVITY_REVIEW_REMINDER", "活动已结束，请完成反馈",
                    "你参与的「" + activity.getName() + "」已结束，可以前往活动详情进行评价打分，并填写本次参与经验。",
                    "ACTIVITY", activity.getId());
        }
        for (String openid : activityMapper.participantOpenids(activity.getId())) {
            wechatMiniProgramService.sendActivityReviewReminder(openid, activity);
        }
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
        if (dto.getPositions() != null && !dto.getPositions().isEmpty()) {
            recruitNumber = dto.getPositions().stream()
                    .map(com.scs.volunteer.dto.ActivityPositionDTO::getRecruitNumber)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
        }
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
        a.setTips(dto.getTips());
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
        if (dto.getPositions() != null && !dto.getPositions().isEmpty()) {
            count = dto.getPositions().stream()
                    .map(com.scs.volunteer.dto.ActivityPositionDTO::getRecruitNumber)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
        }
        if (count == null || count <= 0) throw new BizException("招募人数必须大于 0");
        if ((dto.getServiceHours() == null || dto.getServiceHours() <= 0) && blank(dto.getEndTime())) throw new BizException("服务时长必须大于 0");
        validatePositions(dto.getPositions());
        validatePriorityRules(dto.getPriorityRules());
    }

    private void validatePriorityRules(List<com.scs.volunteer.dto.ActivityPriorityRuleDTO> rules) {
        if (rules == null) return;
        java.util.Set<String> supported = java.util.Set.of(
                "历史活动", "系别", "校区", "技能", "最低信用分", "最低服务时长");
        for (com.scs.volunteer.dto.ActivityPriorityRuleDTO rule : rules) {
            if (rule == null || blank(rule.getRuleType()) || !supported.contains(rule.getRuleType())) {
                throw new BizException("请选择正确的优先条件类型");
            }
            if (blank(rule.getRuleValue())) throw new BizException("请填写优先条件内容");
            if (rule.getWeight() == null || rule.getWeight() < 1 || rule.getWeight() > 100) {
                throw new BizException("优先分值应为1至100");
            }
            if ("最低信用分".equals(rule.getRuleType()) || "最低服务时长".equals(rule.getRuleType())) {
                try {
                    if (Double.parseDouble(rule.getRuleValue()) < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    throw new BizException("信用分和服务时长条件必须填写有效数字");
                }
            }
        }
    }

    private void validatePositions(List<com.scs.volunteer.dto.ActivityPositionDTO> positions) {
        if (positions == null) return;
        for (com.scs.volunteer.dto.ActivityPositionDTO position : positions) {
            if (blank(position.getName())) throw new BizException("岗位名称不能为空");
            if (position.getRecruitNumber() == null || position.getRecruitNumber() <= 0) {
                throw new BizException("岗位招募人数必须大于 0");
            }
            LocalDateTime start = parseDateTime(position.getStartTime(), "岗位开始时间");
            LocalDateTime end = parseDateTime(position.getEndTime(), "岗位结束时间");
            if (!end.isAfter(start)) throw new BizException("岗位结束时间必须晚于开始时间");
            if (Boolean.TRUE.equals(position.getRequiresRehearsal())) {
                if (blank(position.getRehearsalStartTime()) || blank(position.getRehearsalEndTime())) {
                    throw new BizException("开启彩排后必须填写彩排开始和结束时间");
                }
                LocalDateTime rehearsalStart = parseDateTime(position.getRehearsalStartTime(), "彩排开始时间");
                LocalDateTime rehearsalEnd = parseDateTime(position.getRehearsalEndTime(), "彩排结束时间");
                if (!rehearsalEnd.isAfter(rehearsalStart)) {
                    throw new BizException("彩排结束时间必须晚于开始时间");
                }
            }
        }
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
        if ("草稿".equals(status) || "已取消".equals(status) || "已结束".equals(status) || "已满员".equals(status) || "已发布".equals(status)) {
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
