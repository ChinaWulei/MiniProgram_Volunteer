package com.scs.volunteer.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveUserId(session);
        if (userId != null) {
            sessions.put(userId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
    }

    public void pushToUser(Long userId, String event, Object payload) {
        WebSocketSession session = sessions.get(userId);
        if (session == null || !session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("event", event, "data", payload))));
        } catch (Exception ignored) {
        }
    }

    private Long resolveUserId(WebSocketSession session) {
        String query = session.getUri() == null ? "" : session.getUri().getQuery();
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=");
            if (kv.length == 2 && "userId".equals(kv[0])) {
                return Long.valueOf(kv[1]);
            }
            if (kv.length == 2 && "token".equals(kv[0]) && kv[1].startsWith("volunteer-token-")) {
                String[] pieces = kv[1].replace("volunteer-token-", "").split("-");
                return Long.valueOf(pieces[0]);
            }
        }
        return null;
    }
}
