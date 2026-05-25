package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityNewsDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.ActivityNewsMapper;
import com.scs.volunteer.mapper.CheckinMapper;
import com.scs.volunteer.mapper.NotificationMapper;
import com.scs.volunteer.service.ActivityNewsService;
import com.scs.volunteer.service.AiModelClient;
import com.scs.volunteer.service.S3StorageService;
import com.scs.volunteer.vo.ActivityNewsVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ActivityNewsServiceImpl implements ActivityNewsService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ActivityNewsMapper newsMapper;
    private final ActivityMapper activityMapper;
    private final CheckinMapper checkinMapper;
    private final NotificationMapper notificationMapper;
    private final S3StorageService s3StorageService;
    private final AiModelClient aiModelClient;

    public ActivityNewsServiceImpl(ActivityNewsMapper newsMapper, ActivityMapper activityMapper, CheckinMapper checkinMapper,
                                   NotificationMapper notificationMapper, S3StorageService s3StorageService, AiModelClient aiModelClient) {
        this.newsMapper = newsMapper;
        this.activityMapper = activityMapper;
        this.checkinMapper = checkinMapper;
        this.notificationMapper = notificationMapper;
        this.s3StorageService = s3StorageService;
        this.aiModelClient = aiModelClient;
    }

    @Override
    public List<String> uploadImages(MultipartFile[] files, CurrentUser currentUser) {
        requireAdmin(currentUser);
        if (files == null || files.length == 0) throw new BizException("请选择新闻图片");
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(s3StorageService.uploadActivityNewsImage(file));
        }
        return urls;
    }

    @Override
    public Map<String, String> generate(Long activityId, CurrentUser currentUser) {
        requireAdmin(currentUser);
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        Map<String, Object> stats = checkinMapper.statistics(activityId);
        double participant = number(stats.get("approvedCount"));
        double normal = number(stats.get("checkedCount"));
        double manual = number(stats.get("manualCount"));
        double absent = number(stats.get("notCheckedCount"));
        double hours = Math.round(participant * (activity.getServiceHours() == null ? 1D : activity.getServiceHours()) * 10D) / 10D;
        String title = "数计学院开展" + activity.getName() + "活动";
        String fallback = fallbackContent(activity, participant, normal, manual, absent, hours);
        String content = fallback;
        if (aiModelClient.available()) {
            String prompt = "请为校园公众号生成一篇活动新闻稿，风格正式、温暖、有学院感。"
                    + "活动名称：" + activity.getName()
                    + "；时间：" + TIME_FORMAT.format(activity.getStartTime())
                    + "；地点：" + activity.getLocation()
                    + "；参与人数：" + participant
                    + "；正常签到：" + normal
                    + "；补签：" + manual
                    + "；未签到：" + absent
                    + "；累计服务时长：" + hours
                    + "；活动简介：" + activity.getDescription()
                    + "。只输出正文，不要编造不存在的数据。";
            try {
                content = aiModelClient.chat(prompt);
            } catch (Exception ignored) {
                content = fallback;
            }
        }
        return Map.of("title", title, "content", content);
    }

    @Override
    public Long save(ActivityNewsDTO dto, CurrentUser currentUser) {
        requireAdmin(currentUser);
        if (dto == null || dto.getActivityId() == null) throw new BizException("请选择关联活动");
        if (dto.getTitle() == null || dto.getTitle().isBlank()) throw new BizException("帖子标题不能为空");
        activityMapper.findById(dto.getActivityId()).orElseThrow(() -> new BizException("活动不存在"));
        return newsMapper.save(dto, currentUser.getId());
    }

    @Override
    public void publish(Long newsId, CurrentUser currentUser) {
        requireAdmin(currentUser);
        ActivityNewsVO news = newsMapper.find(newsId).orElseThrow(() -> new BizException("新闻不存在"));
        newsMapper.publish(newsId);
        for (Long userId : activityMapper.participantUserIds(news.getActivityId())) {
            notificationMapper.insert(userId, "ACTIVITY_NEWS", "活动新闻已发布",
                    "你参与的《" + news.getActivityName() + "》活动新闻已发布，快来查看活动回顾吧。",
                    "ACTIVITY_NEWS", newsId);
        }
    }

    @Override
    public List<ActivityNewsVO> published() {
        return newsMapper.published();
    }

    @Override
    public ActivityNewsVO detail(Long newsId, CurrentUser currentUser) {
        ActivityNewsVO news = newsMapper.find(newsId).orElseThrow(() -> new BizException("新闻不存在"));
        if (!"PUBLISHED".equals(news.getStatus()) && (currentUser == null || !"ADMIN".equals(currentUser.getRole()))) {
            throw new BizException("新闻不存在");
        }
        newsMapper.increaseRead(newsId);
        return news;
    }

    @Override
    public List<ActivityNewsVO> adminList(CurrentUser currentUser) {
        requireAdmin(currentUser);
        return newsMapper.adminList();
    }

    private String fallbackContent(Activity activity, double participant, double normal, double manual, double absent, double hours) {
        return TIME_FORMAT.format(activity.getStartTime()) + "，" + activity.getName() + "在" + activity.getLocation()
                + "顺利开展。活动围绕" + activity.getCategory() + "服务需求，组织学院志愿者参与现场保障、秩序维护与协同服务。\n\n"
                + "本次活动共有" + (int) participant + "名志愿者参与，正常签到" + (int) normal + "人，补签" + (int) manual
                + "人，累计贡献服务时长约" + hours + "小时。志愿者们分工明确、响应及时，以认真负责的态度保障了活动有序推进。\n\n"
                + "通过本次志愿服务，活动现场运行效率得到提升，也进一步凝聚了数计学院内部志愿服务力量。";
    }

    private void requireAdmin(CurrentUser user) {
        if (user == null || !"ADMIN".equals(user.getRole())) throw new BizException("仅管理员可操作");
    }

    private double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0D;
    }
}
