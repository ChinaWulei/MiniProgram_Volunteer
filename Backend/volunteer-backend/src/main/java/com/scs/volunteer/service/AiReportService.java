package com.scs.volunteer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ReportExportRequest;
import com.scs.volunteer.dto.ReportGenerateRequest;
import com.scs.volunteer.entity.AiReport;
import com.scs.volunteer.mapper.AiReportMapper;
import com.scs.volunteer.mapper.ReportStatsMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiReportService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    private final ReportStatsMapper statsMapper;
    private final AiReportMapper reportMapper;
    private final AiModelClient aiModelClient;
    private final ReportPdfService pdfService;
    private final ReportPdfStorageService storageService;
    private final ObjectMapper objectMapper;

    public AiReportService(ReportStatsMapper statsMapper, AiReportMapper reportMapper, AiModelClient aiModelClient,
                           ReportPdfService pdfService, ReportPdfStorageService storageService, ObjectMapper objectMapper) {
        this.statsMapper = statsMapper;
        this.reportMapper = reportMapper;
        this.aiModelClient = aiModelClient;
        this.pdfService = pdfService;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> generate(ReportGenerateRequest request, CurrentUser user) {
        requireLogin(user);
        String reportType = resolveType(request, user);
        Map<String, Object> stats = "ADMIN".equals(reportType) ? statsMapper.adminStats() : statsMapper.volunteerStats(user.getId());
        String statsJson = writeJson(stats);
        String aiAnalysis = aiAnalysis(reportType, statsJson);

        AiReport report = new AiReport();
        report.setReportNo(reportNo(reportType));
        report.setReportType(reportType);
        report.setUserId(user.getId());
        report.setPeriodStart(blank(request == null ? null : request.getPeriodStart()) ? null : request.getPeriodStart());
        report.setPeriodEnd(blank(request == null ? null : request.getPeriodEnd()) ? null : request.getPeriodEnd());
        report.setStatsJson(statsJson);
        report.setAiAnalysis(aiAnalysis);
        Long id = reportMapper.insert(report);
        report = reportMapper.find(id).orElse(report);
        report.setId(id);
        return response(report, stats);
    }

    public Map<String, Object> export(ReportExportRequest request, CurrentUser user) {
        requireLogin(user);
        if (request == null || request.getReportId() == null) throw new BizException("reportId is required");
        AiReport report = ownedReport(request.getReportId(), user);
        byte[] pdf = pdfService.build(report);
        String url = storageService.upload(pdf, report.getReportNo());
        reportMapper.updatePdf(report.getId(), url);
        report.setPdfUrl(url);
        return response(report, readStats(report));
    }

    public List<Map<String, Object>> list(CurrentUser user) {
        requireLogin(user);
        boolean admin = "ADMIN".equals(user.getRole());
        return reportMapper.list(user.getId(), admin).stream()
                .map(report -> response(report, null))
                .toList();
    }

    public Map<String, Object> detail(Long id, CurrentUser user) {
        requireLogin(user);
        AiReport report = ownedReport(id, user);
        return response(report, readStats(report));
    }

    public void delete(Long id, CurrentUser user) {
        requireLogin(user);
        ownedReport(id, user);
        reportMapper.delete(id);
    }

    private String resolveType(ReportGenerateRequest request, CurrentUser user) {
        String type = request == null || blank(request.getReportType()) ? null : request.getReportType().trim().toUpperCase();
        if (type == null) type = "ADMIN".equals(user.getRole()) ? "ADMIN" : "VOLUNTEER";
        if ("ADMIN".equals(type) && !"ADMIN".equals(user.getRole())) throw new BizException("Only admins can generate admin reports");
        return "ADMIN".equals(type) ? "ADMIN" : "VOLUNTEER";
    }

    private String aiAnalysis(String reportType, String statsJson) {
        String prompt = """
                You are an AI report analyst for a campus volunteer service mini program.
                You must only use the structured statistics JSON below. Do not invent data.
                If data is insufficient, state that clearly. Keep the tone formal, objective and specific.

                Report type: %s
                Statistics JSON:
                %s

                Please generate the following sections:
                - Overall summary
                - Activity or service performance analysis
                - Attendance analysis
                - Highlights
                - Issues and risks
                - Specific improvement suggestions
                - Follow-up recommendations
                """.formatted(reportType, statsJson);
        if (aiModelClient.available()) {
            try {
                String answer = aiModelClient.chat(prompt);
                if (!blank(answer)) return answer;
            } catch (Exception ignored) {
            }
        }
        return fallbackAnalysis(reportType);
    }

    private String fallbackAnalysis(String reportType) {
        if ("ADMIN".equals(reportType)) {
            return "AI provider is not configured. The platform report has been generated from structured statistics. Please review activity scale, attendance rate, absence rate, adjustment pass rate and ranking data to identify operational risks and optimize activity planning.";
        }
        return "AI provider is not configured. The personal report has been generated from structured statistics. Please review service hours, activity count, attendance rate, late records and adjustment records to understand service performance and improvement priorities.";
    }

    private AiReport ownedReport(Long id, CurrentUser user) {
        AiReport report = reportMapper.find(id).orElseThrow(() -> new BizException("Report not found"));
        if ("ADMIN".equals(user.getRole())) return report;
        if (!"VOLUNTEER".equals(report.getReportType()) || !user.getId().equals(report.getUserId())) {
            throw new BizException("No permission to access this report");
        }
        return report;
    }

    private Map<String, Object> response(AiReport report, Map<String, Object> stats) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", report.getId());
        data.put("reportNo", report.getReportNo());
        data.put("reportType", report.getReportType());
        data.put("periodStart", report.getPeriodStart());
        data.put("periodEnd", report.getPeriodEnd());
        data.put("stats", stats == null ? readStats(report) : stats);
        data.put("aiAnalysis", report.getAiAnalysis());
        data.put("pdfUrl", report.getPdfUrl());
        data.put("createdAt", report.getCreatedAt());
        data.put("updatedAt", report.getUpdatedAt());
        return data;
    }

    private Map<String, Object> readStats(AiReport report) {
        try {
            return objectMapper.readValue(report.getStatsJson(), new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String writeJson(Map<String, Object> stats) {
        try {
            return objectMapper.writeValueAsString(stats);
        } catch (Exception e) {
            throw new BizException("Report statistics serialization failed");
        }
    }

    private String reportNo(String reportType) {
        return reportType + "-" + LocalDate.now().format(DAY).replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void requireLogin(CurrentUser user) {
        if (user == null || user.getId() == null) throw new BizException("Please login first");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
