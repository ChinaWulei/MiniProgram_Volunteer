package com.scs.volunteer.service;

import com.scs.volunteer.entity.AiReport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReportPdfService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ObjectMapper objectMapper;

    public ReportPdfService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] build(AiReport report) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FontPack font = loadFont(document);
            PdfWriter writer = new PdfWriter(document, font.font(), font.unicode());
            Map<String, Object> stats = objectMapper.readValue(report.getStatsJson(), new TypeReference<>() {});

            writer.title("AI Volunteer Service Analysis Report");
            writer.line("Report No: " + report.getReportNo(), 13, true);
            writer.line("Generated At: " + TIME.format(LocalDateTime.now()), 13, true);
            writer.line("Period: " + safe(report.getPeriodStart()) + " to " + safe(report.getPeriodEnd()), 13, true);
            writer.gap(18);

            writer.heading("Contents");
            writer.line("1. Data Overview", 12, false);
            writer.line("2. Chart Data Analysis", 12, false);
            writer.line("3. AI Intelligent Analysis", 12, false);
            writer.line("4. Issues Found", 12, false);
            writer.line("5. Optimization Suggestions", 12, false);
            writer.line("6. Summary", 12, false);

            writer.newPage();
            writer.heading("1. Data Overview");
            writeMap(writer, asMap(stats.get("overview")));
            if (stats.containsKey("adjustments")) {
                writer.heading("Adjustment Overview");
                writeMap(writer, asMap(stats.get("adjustments")));
            }

            writer.heading("2. Chart Data Analysis");
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                if ("overview".equals(entry.getKey()) || "adjustments".equals(entry.getKey())) continue;
                writer.subheading(entry.getKey());
                writer.line(shortValue(entry.getValue()), 10, false);
            }

            writer.heading("3. AI Intelligent Analysis");
            for (String line : splitLines(report.getAiAnalysis())) {
                writer.line(line, 11, false);
            }

            writer.heading("4. Issues Found");
            writer.line("See AI analysis for abnormal attendance, low activity participation, and data insufficiency notes.", 11, false);
            writer.heading("5. Optimization Suggestions");
            writer.line("Use the report conclusions to improve activity planning, reminder mechanisms, volunteer grouping, and follow-up review.", 11, false);
            writer.heading("6. Summary");
            writer.line("This report is generated from platform business statistics and AI analysis context. AI did not access the database directly.", 11, false);

            writer.close();
            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void writeMap(PdfWriter writer, Map<String, Object> map) throws Exception {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            writer.line(entry.getKey() + ": " + shortValue(entry.getValue()), 11, false);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        return Map.of();
    }

    private String shortValue(Object value) {
        if (value == null) return "-";
        String text = value.toString().replace("\n", " ");
        return text.length() > 520 ? text.substring(0, 520) + "..." : text;
    }

    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) return List.of("No AI analysis was generated.");
        List<String> lines = new ArrayList<>();
        for (String part : text.split("\\R")) {
            if (!part.isBlank()) lines.add(part.trim());
        }
        return lines;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "All" : value;
    }

    private FontPack loadFont(PDDocument document) {
        List<String> paths = List.of(
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/msyh.ttf",
                "C:/Windows/Fonts/simsun.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"
        );
        for (String path : paths) {
            try {
                File file = new File(path);
                if (file.exists()) return new FontPack(PDType0Font.load(document, file), true);
            } catch (Exception ignored) {
            }
        }
        return new FontPack(new PDType1Font(Standard14Fonts.FontName.HELVETICA), false);
    }

    private record FontPack(PDFont font, boolean unicode) {}

    private static class PdfWriter {
        private final PDDocument document;
        private final PDFont font;
        private final boolean unicode;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        PdfWriter(PDDocument document, PDFont font, boolean unicode) throws Exception {
            this.document = document;
            this.font = font;
            this.unicode = unicode;
            newPage();
        }

        void title(String text) throws Exception {
            line(text, 22, true);
            gap(16);
        }

        void heading(String text) throws Exception {
            gap(10);
            line(text, 15, true);
            gap(4);
        }

        void subheading(String text) throws Exception {
            line(text, 12, true);
        }

        void line(String text, int size, boolean bold) throws Exception {
            for (String line : wrap(text, size)) {
                if (y < 64) newPage();
                stream.beginText();
                stream.setFont(font, size);
                stream.newLineAtOffset(54, y);
                stream.showText(unicode ? line : line.replaceAll("[^\\x20-\\x7E]", "?"));
                stream.endText();
                y -= size + 7;
            }
        }

        void gap(float value) {
            y -= value;
        }

        void newPage() throws Exception {
            if (stream != null) stream.close();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - 58;
        }

        void close() throws Exception {
            if (stream != null) stream.close();
        }

        private List<String> wrap(String text, int size) {
            String safe = text == null ? "" : text;
            int limit = Math.max(38, 95 - size);
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : safe.split("\\s+")) {
                if (word.length() > limit) {
                    if (current.length() > 0) {
                        lines.add(current.toString());
                        current.setLength(0);
                    }
                    for (int i = 0; i < word.length(); i += limit) {
                        lines.add(word.substring(i, Math.min(i + limit, word.length())));
                    }
                    continue;
                }
                if (current.length() + word.length() + 1 > limit) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                if (current.length() > 0) current.append(' ');
                current.append(word);
            }
            if (!current.isEmpty()) lines.add(current.toString());
            if (lines.isEmpty()) lines.add("");
            return lines;
        }
    }
}
