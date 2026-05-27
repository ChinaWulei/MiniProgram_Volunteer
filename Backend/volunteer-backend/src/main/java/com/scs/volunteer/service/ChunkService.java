package com.scs.volunteer.service;

import com.scs.volunteer.config.RagProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkService {
    private final RagProperties properties;

    public ChunkService(RagProperties properties) {
        this.properties = properties;
    }

    public List<String> split(String text) {
        String normalized = normalize(text);
        List<String> chunks = new ArrayList<>();
        int chunkSize = Math.max(200, properties.getChunkSize());
        int overlap = Math.max(0, Math.min(properties.getChunkOverlap(), chunkSize / 2));
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            int softEnd = findSoftEnd(normalized, start, end);
            String chunk = normalized.substring(start, softEnd).trim();
            if (!chunk.isBlank()) chunks.add(chunk);
            if (softEnd >= normalized.length()) break;
            start = Math.max(softEnd - overlap, start + 1);
        }
        return chunks;
    }

    private int findSoftEnd(String text, int start, int hardEnd) {
        if (hardEnd >= text.length()) return text.length();
        for (int i = hardEnd; i > start + 200; i--) {
            char c = text.charAt(i - 1);
            if (c == '\n' || c == '。' || c == '；' || c == '.' || c == ';') {
                return i;
            }
        }
        return hardEnd;
    }

    private String normalize(String text) {
        return (text == null ? "" : text)
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
