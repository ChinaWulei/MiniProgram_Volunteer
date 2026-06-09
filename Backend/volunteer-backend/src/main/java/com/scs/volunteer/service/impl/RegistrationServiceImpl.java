package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.RegistrationDTO;
import com.scs.volunteer.dto.ReviewDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.ActivityPositionMapper;
import com.scs.volunteer.mapper.ActivityPriorityRuleMapper;
import com.scs.volunteer.mapper.ExamScheduleMapper;
import com.scs.volunteer.mapper.CourseScheduleMapper;
import com.scs.volunteer.mapper.CreditMapper;
import com.scs.volunteer.mapper.EvaluationMapper;
import com.scs.volunteer.mapper.NotificationMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
import com.scs.volunteer.mapper.ServiceRecordMapper;
import com.scs.volunteer.mapper.VolunteerMapper;
import com.scs.volunteer.service.RegistrationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final ActivityPositionMapper activityPositionMapper;
    private final ActivityPriorityRuleMapper activityPriorityRuleMapper;
    private final ExamScheduleMapper examScheduleMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final VolunteerMapper volunteerMapper;
    private final ServiceRecordMapper serviceRecordMapper;
    private final NotificationMapper notificationMapper;
    private final CreditMapper creditMapper;
    private final EvaluationMapper evaluationMapper;

    public RegistrationServiceImpl(RegistrationMapper registrationMapper, ActivityMapper activityMapper,
                                   ActivityPositionMapper activityPositionMapper,
                                   ActivityPriorityRuleMapper activityPriorityRuleMapper,
                                   ExamScheduleMapper examScheduleMapper, CourseScheduleMapper courseScheduleMapper,
                                   VolunteerMapper volunteerMapper, ServiceRecordMapper serviceRecordMapper,
                                   NotificationMapper notificationMapper, CreditMapper creditMapper, EvaluationMapper evaluationMapper) {
        this.registrationMapper = registrationMapper;
        this.activityMapper = activityMapper;
        this.activityPositionMapper = activityPositionMapper;
        this.activityPriorityRuleMapper = activityPriorityRuleMapper;
        this.examScheduleMapper = examScheduleMapper;
        this.courseScheduleMapper = courseScheduleMapper;
        this.volunteerMapper = volunteerMapper;
        this.serviceRecordMapper = serviceRecordMapper;
        this.notificationMapper = notificationMapper;
        this.creditMapper = creditMapper;
        this.evaluationMapper = evaluationMapper;
    }

    @Override
    public void register(RegistrationDTO dto, CurrentUser currentUser) {
        if (currentUser == null || !"VOLUNTEER".equals(currentUser.getRole())) {
            throw new BizException("仅志愿者可报名");
        }
        Activity activity = activityMapper.findById(dto.getActivityId()).orElseThrow(() -> new BizException("活动不存在"));
        if (!"报名中".equals(activity.getStatus()) && !"已发布".equals(activity.getStatus())) {
            throw new BizException("当前活动不可报名");
        }
        if (activity.getRegisteredNumber() >= activity.getRecruitNumber()) {
            throw new BizException("活动已满员");
        }
        if (registrationMapper.exists(dto.getActivityId(), currentUser.getId())) {
            throw new BizException("不能重复报名");
        }
        if (creditMapper.score(currentUser.getId()) < 70) {
            throw new BizException("信用分低于70分，暂不能报名，请联系管理员处理");
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (activity.getSignupStartTime() != null && now.isBefore(activity.getSignupStartTime())) {
            throw new BizException("报名尚未开始");
        }
        if (activity.getSignupDeadline() != null && now.isAfter(activity.getSignupDeadline())) {
            throw new BizException("报名已截止");
        }
        java.time.LocalDateTime selectedStart = activity.getStartTime();
        java.time.LocalDateTime selectedEnd = activity.getEndTime();
        java.time.LocalDateTime rehearsalStart = null;
        java.time.LocalDateTime rehearsalEnd = null;
        List<Map<String, Object>> positions = activityPositionMapper.list(activity.getId());
        if (!positions.isEmpty()) {
            if (dto.getPositionId() == null) throw new BizException("请选择报名岗位");
            Map<String, Object> position = activityPositionMapper.find(dto.getPositionId(), activity.getId());
            if (position == null) throw new BizException("所选岗位不存在");
            if (((Number) position.get("remaining_number")).intValue() <= 0) throw new BizException("所选岗位已满员");
            selectedStart = dateTime(position.get("start_time"));
            selectedEnd = dateTime(position.get("end_time"));
            if (Boolean.TRUE.equals(booleanValue(position.get("requires_rehearsal")))) {
                rehearsalStart = dateTime(position.get("rehearsal_start_time"));
                rehearsalEnd = dateTime(position.get("rehearsal_end_time"));
            }
        }
        ensureNoConflict(currentUser.getId(), activity.getId(), selectedStart, selectedEnd, "所选岗位");
        if (rehearsalStart != null && rehearsalEnd != null) {
            ensureNoConflict(currentUser.getId(), activity.getId(), rehearsalStart, rehearsalEnd, "彩排");
        }
        String signupStatus = "自动通过".equals(activity.getReviewMethod()) ? "已通过" : "待审核";
        registrationMapper.insert(dto.getActivityId(), currentUser.getId(), dto.getPositionId(),
                Boolean.TRUE.equals(dto.getTransportRequired()), dto.getBoardingPoint(), signupStatus);
        activityMapper.increaseRegistered(dto.getActivityId());
        activityMapper.refreshRegisteredNumbers();
    }

    @Override
    public List<Map<String, Object>> my(CurrentUser currentUser) {
        return registrationMapper.my(currentUser.getId());
    }

    @Override
    public List<Map<String, Object>> adminList(String keyword, String status, Long activityId, String department,
                                               CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可查看报名列表");
        }
        List<Map<String, Object>> rows = registrationMapper.adminList(keyword, status, activityId, department);
        Map<Long, List<Map<String, Object>>> ruleCache = new HashMap<>();
        rows.forEach(row -> enrichReviewInfo(row, ruleCache));
        rows.sort(Comparator
                .comparingDouble((Map<String, Object> row) -> number(row.get("priorityScore"), 0)).reversed()
                .thenComparing(Comparator.comparingDouble(
                        (Map<String, Object> row) -> number(row.get("matchScore"), 0)).reversed())
                .thenComparing(row -> text(row.get("created_at"))));
        return rows;
    }

    @Override
    public List<String> adminDepartments(CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可查看系别列表");
        }
        return registrationMapper.departments();
    }

    @Override
    public byte[] exportApproved(Long activityId, CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可导出录取名单");
        }
        Activity activity = activityMapper.findById(activityId)
                .orElseThrow(() -> new BizException("活动不存在"));
        List<Map<String, Object>> rows = registrationMapper.approvedExportList(activityId);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("录取名单");
            String[] headers = {
                    "序号", "活动名称", "姓名", "学号", "手机号", "学院", "校区", "系别",
                    "班级", "岗位", "技能", "信用分", "累计服务时长", "需要交通", "上车点", "报名状态", "报名时间"
            };
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> item = rows.get(i);
                Row row = sheet.createRow(i + 1);
                int column = 0;
                row.createCell(column++).setCellValue(i + 1);
                row.createCell(column++).setCellValue(activity.getName());
                row.createCell(column++).setCellValue(text(item.get("userName")));
                row.createCell(column++).setCellValue(text(item.get("identityNo")));
                row.createCell(column++).setCellValue(text(item.get("phone")));
                row.createCell(column++).setCellValue(text(item.get("college")));
                row.createCell(column++).setCellValue(text(item.get("campus")));
                row.createCell(column++).setCellValue(text(item.get("department")));
                row.createCell(column++).setCellValue(text(item.get("majorClass")));
                row.createCell(column++).setCellValue(text(item.get("positionName")));
                row.createCell(column++).setCellValue(text(item.get("skillTags")));
                row.createCell(column++).setCellValue(number(item.get("creditScore"), 0));
                row.createCell(column++).setCellValue(number(item.get("totalHours"), 0));
                row.createCell(column++).setCellValue(booleanValue(item.get("transportRequired")) ? "是" : "否");
                row.createCell(column++).setCellValue(text(item.get("boardingPoint")));
                row.createCell(column++).setCellValue(text(item.get("status")));
                row.createCell(column).setCellValue(text(item.get("signupTime")));
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, Math.max(0, rows.size()), 0, headers.length - 1));
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 800, 12000));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new BizException("录取名单生成失败");
        }
    }

    private Map<String, Object> enrichReviewInfo(Map<String, Object> row,
                                                 Map<Long, List<Map<String, Object>>> ruleCache) {
        row.put("matchScore", matchScore(row));
        row.put("matchReason", matchReason(row));
        applyPriorityRules(row, ruleCache);
        row.put("aiEvaluationSummary", evaluationSummary(((Number) row.get("user_id")).longValue()));
        return row;
    }

    private void applyPriorityRules(Map<String, Object> row,
                                    Map<Long, List<Map<String, Object>>> ruleCache) {
        Long activityId = ((Number) row.get("activity_id")).longValue();
        Long userId = ((Number) row.get("user_id")).longValue();
        List<Map<String, Object>> rules = ruleCache.computeIfAbsent(
                activityId, activityPriorityRuleMapper::list);
        List<String> reasons = new ArrayList<>();
        int score = 0;
        for (Map<String, Object> rule : rules) {
            String type = text(rule.get("ruleType"));
            String value = text(rule.get("ruleValue"));
            int weight = (int) number(rule.get("weight"), 0);
            if (priorityRuleMatches(type, value, row, userId)) {
                score += weight;
                reasons.add(priorityRuleLabel(type, value) + " +" + weight);
            }
        }
        row.put("priorityScore", score);
        row.put("priorityReason", reasons.isEmpty() ? "未命中活动优先条件" : String.join("；", reasons));
    }

    private boolean priorityRuleMatches(String type, String value, Map<String, Object> row, Long userId) {
        return switch (type) {
            case "历史活动" -> registrationMapper.hasCompletedActivity(userId, value);
            case "历史活动类型" -> registrationMapper.hasCompletedActivityCategory(userId, value);
            case "系别" -> value.equals(text(row.get("department")));
            case "校区" -> value.equals(text(row.get("campus")));
            case "技能" -> split(text(row.get("skillTags"))).contains(value);
            case "最低信用分" -> number(row.get("creditScore"), 0) >= parseNumber(value);
            case "最低服务时长" -> number(row.get("totalHours"), 0) >= parseNumber(value);
            default -> false;
        };
    }

    private String priorityRuleLabel(String type, String value) {
        return switch (type) {
            case "历史活动" -> "参加过《" + value + "》";
            case "历史活动类型" -> "参加过" + value + "类活动";
            case "最低信用分" -> "信用分达到" + value;
            case "最低服务时长" -> "服务时长达到" + value + "小时";
            default -> type + "：" + value;
        };
    }

    private double parseNumber(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }

    private double matchScore(Map<String, Object> row) {
        Set<String> required = split(text(row.get("skillRequirements")));
        Set<String> skills = split(text(row.get("skillTags")));
        long hits = required.stream().filter(skills::contains).count();
        double skillScore = required.isEmpty() ? 1 : (double) hits / required.size();
        double timeScore = timeMatched(row.get("startTime"), text(row.get("availableTime"))) ? 1 : 0;
        double creditScore = Math.min(100, number(row.get("creditScore"), 80)) / 100.0;
        return Math.round((skillScore * 60 + timeScore * 20 + creditScore * 20) * 10.0) / 10.0;
    }

    private String matchReason(Map<String, Object> row) {
        Set<String> required = split(text(row.get("skillRequirements")));
        Set<String> skills = split(text(row.get("skillTags")));
        long hits = required.stream().filter(skills::contains).count();
        boolean timeMatched = timeMatched(row.get("startTime"), text(row.get("availableTime")));
        return "技能匹配 " + hits + "/" + required.size()
                + "，时间" + (timeMatched ? "可服务" : "需确认")
                + "，信用分 " + (int) number(row.get("creditScore"), 80);
    }

    private boolean timeMatched(Object startTimeValue, String availableTime) {
        if (startTimeValue == null || availableTime == null || availableTime.isBlank()) {
            return false;
        }
        java.time.LocalDateTime startTime = dateTime(startTimeValue);
        String text = availableTime.toLowerCase();
        int hour = startTime.getHour();
        return text.contains("全天")
                || (hour < 12 && text.contains("上午"))
                || (hour >= 12 && hour < 18 && text.contains("下午"))
                || (hour >= 18 && text.contains("晚上"));
    }

    private String evaluationSummary(Long userId) {
        List<Map<String, Object>> evaluations = evaluationMapper.byVolunteer(userId);
        if (evaluations.isEmpty()) {
            return "暂无历史志愿者评价，可重点参考服务时长、信用分和技能匹配度。";
        }
        double average = evaluations.stream().mapToDouble(item -> number(item.get("score"), 0)).average().orElse(0);
        String highlights = evaluations.stream()
                .map(item -> text(item.get("content")))
                .filter(content -> !content.isBlank())
                .limit(3)
                .collect(Collectors.joining("; "));
        if (highlights.isBlank()) {
            highlights = "近期评价暂无文字内容。";
        }
        return "AI摘要：历史均分 " + Math.round(average * 10.0) / 10.0 + "/5。" + highlights;
    }

    private Set<String> split(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split("[,;|，、\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private double number(Object value, double fallback) {
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private java.time.LocalDateTime dateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.time.LocalDateTime dateTime) return dateTime;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        return java.time.LocalDateTime.parse(String.valueOf(value).replace(" ", "T"));
    }

    private void ensureNoConflict(Long userId, Long activityId, java.time.LocalDateTime start,
                                  java.time.LocalDateTime end, String label) {
        List<Map<String, Object>> examConflicts = examScheduleMapper.conflicts(userId, start, end);
        if (!examConflicts.isEmpty()) {
            throw new BizException(label + "与考试《" + examConflicts.get(0).get("courseName") + "》时间冲突");
        }
        List<Map<String, Object>> courseConflicts = courseScheduleMapper.conflicts(userId, start, end);
        if (!courseConflicts.isEmpty()) {
            throw new BizException(label + "与课程《" + courseConflicts.get(0).get("courseName") + "》时间冲突");
        }
        if (registrationMapper.hasTimeConflict(userId, activityId, start, end)) {
            throw new BizException(label + "与已报名活动时间冲突");
        }
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;
        return "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    @Override
    public void review(Long id, ReviewDTO dto, CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可审核");
        }
        Map<String, Object> reg = registrationMapper.findMap(id);
        registrationMapper.review(id, dto.getStatus(), dto.getReviewRemark());
        activityMapper.refreshRegisteredNumbers();
        Long noticeUserId = ((Number) reg.get("user_id")).longValue();
        Long noticeActivityId = ((Number) reg.get("activity_id")).longValue();
        Activity noticeActivity = activityMapper.findById(noticeActivityId).orElse(null);
        if (noticeActivity != null) {
            notificationMapper.insert(noticeUserId, "REGISTRATION_REVIEW", "报名审核结果",
                    "你报名的《" + noticeActivity.getName() + "》状态已更新为：" + dto.getStatus(),
                    "ACTIVITY", noticeActivityId);
        }
        if ("已完成".equals(dto.getStatus())) {
            Long userId = ((Number) reg.get("user_id")).longValue();
            Long activityId = ((Number) reg.get("activity_id")).longValue();
            Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
            double hours = activity.getServiceHours() == null
                    ? Math.max(1.0, Duration.between(activity.getStartTime(), activity.getEndTime()).toMinutes() / 60.0)
                    : activity.getServiceHours();
            volunteerMapper.addService(userId, hours);
            serviceRecordMapper.insert(userId, activityId, hours, "管理员确认完成");
        }
    }

    @Override
    @Transactional
    public void cancel(Long id, ReviewDTO dto, CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可取消报名");
        }
        String reason = dto == null ? "" : (dto.getReason() == null ? dto.getReviewRemark() : dto.getReason());
        if (reason == null || reason.isBlank()) throw new BizException("请填写取消原因");
        Map<String, Object> reg = registrationMapper.findMap(id);
        Long userId = ((Number) reg.get("user_id")).longValue();
        Long activityId = ((Number) reg.get("activity_id")).longValue();
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        removeRegistrationAndPromote(reg, activity);
        notificationMapper.insert(userId, "REGISTRATION_CANCELLED", "报名已取消",
                "你报名的《" + activity.getName() + "》已由管理员取消。原因：" + reason,
                "ACTIVITY", activityId);
    }

    @Override
    @Transactional
    public void withdraw(Long id, CurrentUser currentUser) {
        if (currentUser == null || !"VOLUNTEER".equals(currentUser.getRole())) {
            throw new BizException("仅志愿者可取消自己的报名");
        }
        Map<String, Object> reg = registrationMapper.findMap(id);
        Long userId = ((Number) reg.get("user_id")).longValue();
        if (!currentUser.getId().equals(userId)) throw new BizException("无权取消该报名");
        String status = text(reg.get("status"));
        if (!"待审核".equals(status) && !"已通过".equals(status)) {
            throw new BizException("当前报名状态不可取消");
        }
        Long activityId = ((Number) reg.get("activity_id")).longValue();
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        if (activity.getStartTime() == null || !java.time.LocalDateTime.now().isBefore(activity.getStartTime())) {
            throw new BizException("活动已开始，无法自行取消，请联系管理员");
        }
        removeRegistrationAndPromote(reg, activity);
        notificationMapper.insert(userId, "REGISTRATION_CANCELLED", "已取消活动报名",
                "你已取消《" + activity.getName() + "》的报名。",
                "ACTIVITY", activityId);
    }

    private void removeRegistrationAndPromote(Map<String, Object> reg, Activity activity) {
        boolean shouldPromote = "已通过".equals(text(reg.get("status")))
                && activity.getStartTime() != null
                && java.time.LocalDateTime.now().isBefore(activity.getStartTime());
        Long positionId = reg.get("position_id") instanceof Number number ? number.longValue() : null;
        registrationMapper.delete(((Number) reg.get("id")).longValue());
        activityMapper.decreaseRegistered(activity.getId());
        activityMapper.refreshRegisteredNumbers();
        if (shouldPromote) promoteNextCandidate(activity, positionId);
    }

    private void promoteNextCandidate(Activity activity, Long positionId) {
        registrationMapper.lockPendingCandidates(activity.getId(), positionId);
        List<Map<String, Object>> candidates = registrationMapper.pendingCandidates(activity.getId(), positionId);
        Map<Long, List<Map<String, Object>>> ruleCache = new HashMap<>();
        candidates.forEach(candidate -> enrichReviewInfo(candidate, ruleCache));
        candidates.sort(Comparator
                .comparingDouble((Map<String, Object> row) -> number(row.get("priorityScore"), 0)).reversed()
                .thenComparing(Comparator.comparingDouble(
                        (Map<String, Object> row) -> number(row.get("matchScore"), 0)).reversed())
                .thenComparing(row -> text(row.get("created_at"))));

        for (Map<String, Object> candidate : candidates) {
            Long candidateUserId = ((Number) candidate.get("user_id")).longValue();
            try {
                java.time.LocalDateTime start = dateTime(candidate.get("positionStartTime"));
                java.time.LocalDateTime end = dateTime(candidate.get("positionEndTime"));
                if (start == null || end == null) {
                    start = activity.getStartTime();
                    end = activity.getEndTime();
                }
                ensureNoConflict(candidateUserId, activity.getId(), start, end, "递补岗位");
                if (Boolean.TRUE.equals(booleanValue(candidate.get("requiresRehearsal")))) {
                    java.time.LocalDateTime rehearsalStart = dateTime(candidate.get("rehearsalStartTime"));
                    java.time.LocalDateTime rehearsalEnd = dateTime(candidate.get("rehearsalEndTime"));
                    if (rehearsalStart != null && rehearsalEnd != null) {
                        ensureNoConflict(candidateUserId, activity.getId(), rehearsalStart, rehearsalEnd, "递补彩排");
                    }
                }
            } catch (BizException ignored) {
                continue;
            }

            Long registrationId = ((Number) candidate.get("id")).longValue();
            registrationMapper.review(registrationId, "已通过", "录取人员取消，系统按优先分和匹配分自动递补");
            activityMapper.refreshRegisteredNumbers();
            notificationMapper.insert(candidateUserId, "REGISTRATION_REVIEW", "报名自动递补成功",
                    "《" + activity.getName() + "》出现空缺，你已按活动优先条件和综合匹配分自动递补录取，请及时确认活动安排。",
                    "ACTIVITY", activity.getId());
            return;
        }
    }
}
