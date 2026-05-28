package com.scs.volunteer.service.impl;

import com.scs.volunteer.service.AiIntent;
import com.scs.volunteer.service.AiModelClient;
import com.scs.volunteer.service.IntentRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class IntentRouterServiceImpl implements IntentRouterService {
    private static final Logger log = LoggerFactory.getLogger(IntentRouterServiceImpl.class);

    private final AiModelClient aiModelClient;

    public IntentRouterServiceImpl(AiModelClient aiModelClient) {
        this.aiModelClient = aiModelClient;
    }

    @Override
    public AiIntent route(String message, Long activityId) {
        String text = message == null ? "" : message.trim();
        if (activityId != null && containsAny(text, "适合", "匹配", "能参加", "推荐报名", "是否适合")) {
            return AiIntent.ACTIVITY_MATCH;
        }
        if (activityId != null && containsAny(text, "总结", "介绍", "主要内容", "注意事项", "新手")) {
            return AiIntent.ACTIVITY_SUMMARY;
        }
        if (containsAny(text, "签到", "补签", "时长认定", "违规", "报名规则", "规则", "制度", "扣分", "信用分规则", "认定", "处理办法")) {
            return AiIntent.RULE_QA;
        }
        if (containsAny(text, "月总结", "本月", "这个月", "月度", "志愿情况", "成长报告")) {
            return AiIntent.MONTHLY_REPORT;
        }
        if (containsAny(text, "推荐", "适合参加什么", "参加什么活动", "推荐几个", "找活动", "可报名活动")) {
            return AiIntent.ACTIVITY_RECOMMEND;
        }
        if (containsAny(text, "我报名", "报名了哪些", "多少时长", "志愿时长", "积分", "等级", "服务次数", "个人数据", "我的活动")) {
            return AiIntent.PROFILE_QUERY;
        }
        return classifyByLlm(text);
    }

    private AiIntent classifyByLlm(String message) {
        if (!aiModelClient.available()) {
            return AiIntent.GENERAL_CHAT;
        }
        try {
            String prompt = """
                    请判断用户问题属于哪个意图，只输出一个枚举值：
                    RULE_QA, ACTIVITY_RECOMMEND, ACTIVITY_MATCH, MONTHLY_REPORT, ACTIVITY_SUMMARY, PROFILE_QUERY, GENERAL_CHAT

                    判断规则：
                    RULE_QA：签到、补签、时长认定、违规处理、报名规则等制度问题
                    ACTIVITY_RECOMMEND：推荐志愿活动
                    ACTIVITY_MATCH：询问某个具体活动是否适合自己
                    MONTHLY_REPORT：月度志愿总结
                    ACTIVITY_SUMMARY：总结某个活动内容或注意事项
                    PROFILE_QUERY：查询自己的报名、时长、积分等业务数据
                    GENERAL_CHAT：问候、闲聊、非业务问题

                    用户问题：%s
                    """.formatted(message);
            String result = aiModelClient.chat(prompt);
            if (result == null) {
                return AiIntent.GENERAL_CHAT;
            }
            String normalized = result.trim().toUpperCase(Locale.ROOT);
            for (AiIntent intent : AiIntent.values()) {
                if (normalized.contains(intent.name())) {
                    return intent;
                }
            }
        } catch (Exception e) {
            log.warn("AI intent fallback classification failed: {}", e.getMessage());
        }
        return AiIntent.GENERAL_CHAT;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
