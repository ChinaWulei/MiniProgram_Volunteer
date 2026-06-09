package com.scs.volunteer.service;

public interface AiModelClient {
    boolean available();
    String chat(String prompt);
    String chatWithImage(String prompt, byte[] imageBytes, String mimeType);
}
