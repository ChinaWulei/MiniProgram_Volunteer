package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.mapper.ActivityExperienceMapper;
import com.scs.volunteer.mapper.EvaluationMapper;
import com.scs.volunteer.service.ActivityExperienceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ActivityExperienceServiceImpl implements ActivityExperienceService {
    private final ActivityExperienceMapper experienceMapper;
    private final EvaluationMapper evaluationMapper;

    public ActivityExperienceServiceImpl(ActivityExperienceMapper experienceMapper, EvaluationMapper evaluationMapper) {
        this.experienceMapper = experienceMapper;
        this.evaluationMapper = evaluationMapper;
    }

    @Override
    public List<Map<String, Object>> list(String category, Boolean enabled, CurrentUser currentUser) {
        requireAdmin(currentUser);
        return experienceMapper.list(category, enabled);
    }

    @Override
    public void adopt(Long evaluationId, String type, CurrentUser currentUser) {
        requireAdmin(currentUser);
        String normalized = normalizeType(type);
        Map<String, Object> evaluation = evaluationMapper.find(evaluationId);
        String content = switch (normalized) {
            case "ADVANTAGE" -> text(evaluation.get("parsed_advantages"));
            case "SUGGESTION" -> text(evaluation.get("parsed_suggestions"));
            default -> "";
        };
        if (content.isBlank() || "无".equals(content)) {
            throw new BizException("该评价没有可采纳的内容");
        }
        experienceMapper.adopt(
                ((Number) evaluation.get("activityId")).longValue(),
                evaluationId,
                text(evaluation.get("category")),
                normalized,
                content,
                currentUser.getId()
        );
    }

    @Override
    public void unadopt(Long evaluationId, String type, CurrentUser currentUser) {
        requireAdmin(currentUser);
        experienceMapper.unadopt(evaluationId, normalizeType(type));
    }

    @Override
    public void setEnabled(Long id, boolean enabled, CurrentUser currentUser) {
        requireAdmin(currentUser);
        experienceMapper.setEnabled(id, enabled);
    }

    @Override
    public List<Map<String, Object>> enabledByCategory(String category, int limit) {
        return experienceMapper.enabledByCategory(category, limit);
    }

    private String normalizeType(String type) {
        String value = type == null ? "" : type.trim().toUpperCase();
        if (!"ADVANTAGE".equals(value) && !"SUGGESTION".equals(value)) {
            throw new BizException("经验类型不正确");
        }
        return value;
    }

    private void requireAdmin(CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可操作");
        }
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
