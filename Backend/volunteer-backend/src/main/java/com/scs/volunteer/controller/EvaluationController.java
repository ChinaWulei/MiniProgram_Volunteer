package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityEvaluationDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.CreditMapper;
import com.scs.volunteer.mapper.EvaluationMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities/{activityId}/evaluations")
public class EvaluationController extends BaseController {
    private final ActivityMapper activityMapper;
    private final EvaluationMapper evaluationMapper;
    private final CreditMapper creditMapper;

    public EvaluationController(ActivityMapper activityMapper, EvaluationMapper evaluationMapper, CreditMapper creditMapper) {
        this.activityMapper = activityMapper;
        this.evaluationMapper = evaluationMapper;
        this.creditMapper = creditMapper;
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> submit(@PathVariable Long activityId, @RequestBody ActivityEvaluationDTO dto, HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        boolean endedByStatus = "已结束".equals(activity.getStatus());
        if (!endedByStatus && activity.getEndTime() != null && activity.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BizException("活动结束后才可评价");
        }
        validate(user, dto);
        if (evaluationMapper.exists(activityId, user.getId(), dto.getTargetType(), dto.getTargetUserId())) {
            throw new BizException("请勿重复评价");
        }
        Long id = evaluationMapper.insert(activityId, user.getId(), dto);
        applyCredit(user, activityId, dto);
        return ApiResponse.ok(Map.of("id", id));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable Long activityId, HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user == null || !"ADMIN".equals(user.getRole())) throw new BizException("仅管理员可查看评价明细");
        return ApiResponse.ok(evaluationMapper.byActivity(activityId));
    }

    private void validate(CurrentUser user, ActivityEvaluationDTO dto) {
        if (user == null) throw new BizException("请先登录");
        if (dto == null) throw new BizException("评价内容不能为空");
        if (dto.getScore() == null || dto.getScore() < 1 || dto.getScore() > 5) throw new BizException("评分范围为1到5分");
        String targetType = dto.getTargetType() == null ? "" : dto.getTargetType().trim().toUpperCase();
        if (!targetType.equals("ACTIVITY") && !targetType.equals("LEADER") && !targetType.equals("VOLUNTEER")) {
            throw new BizException("评价对象不正确");
        }
        dto.setTargetType(targetType);
        if ("VOLUNTEER".equals(targetType)) {
            if (!"ADMIN".equals(user.getRole())) throw new BizException("仅负责人可评价志愿者");
            if (dto.getTargetUserId() == null) throw new BizException("请选择志愿者");
        }
        if (("ACTIVITY".equals(targetType) || "LEADER".equals(targetType)) && !"VOLUNTEER".equals(user.getRole())) {
            throw new BizException("仅志愿者可评价活动或负责人");
        }
    }

    private void applyCredit(CurrentUser user, Long activityId, ActivityEvaluationDTO dto) {
        if (!"VOLUNTEER".equals(dto.getTargetType()) || dto.getTargetUserId() == null) return;
        if (dto.getScore() <= 2) {
            creditMapper.apply(dto.getTargetUserId(), -5, "负责人低分评价", "EVALUATION", activityId);
        } else if (dto.getScore() >= 5) {
            creditMapper.apply(dto.getTargetUserId(), 2, "负责人五星评价", "EVALUATION", activityId);
        }
    }
}
