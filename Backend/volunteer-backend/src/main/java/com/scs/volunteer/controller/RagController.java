package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.RagQuestionRequest;
import com.scs.volunteer.service.RagService;
import com.scs.volunteer.vo.RagAnswerVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {
    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public ApiResponse<RagAnswerVO> ask(@RequestBody RagQuestionRequest request) {
        return ApiResponse.ok(ragService.answer(request.getQuestion(), request.getTopK()));
    }
}
