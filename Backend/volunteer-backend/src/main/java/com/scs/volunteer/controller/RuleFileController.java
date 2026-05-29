package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.entity.RuleFile;
import com.scs.volunteer.service.RuleFileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rule-files")
public class RuleFileController extends BaseController {
    private final RuleFileService ruleFileService;

    public RuleFileController(RuleFileService ruleFileService) {
        this.ruleFileService = ruleFileService;
    }

    @PostMapping
    public ApiResponse<RuleFile> upload(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(ruleFileService.upload(file, currentUser(request)));
    }

    @GetMapping
    public ApiResponse<List<RuleFile>> list() {
        return ApiResponse.ok(ruleFileService.list());
    }

    @GetMapping("/{id}/download")
    public ApiResponse<Map<String, String>> download(@PathVariable Long id) {
        RuleFile file = ruleFileService.detail(id);
        return ApiResponse.ok(Map.of("url", file.getS3Url(), "fileName", file.getOriginalName()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        ruleFileService.delete(id, currentUser(request));
        return ApiResponse.ok(null);
    }
}
