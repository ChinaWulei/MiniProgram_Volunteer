package com.scs.volunteer.service;

public interface IntentRouterService {
    AiIntent route(String message, Long activityId);
}
