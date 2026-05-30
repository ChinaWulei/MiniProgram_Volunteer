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
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.awt.Color;

@Service
public class ReportPdfService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Color TEAL = new Color(15, 118, 110);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color AMBER = new Color(245, 158, 11);
    private static final Color RED = new Color(239, 68, 68);
    private static final Color PURPLE = new Color(124, 58, 237);
    private static final Color CYAN = new Color(8, 145, 178);
    private static final Color GREEN = new Color(34, 197, 94);
    private static final Color SLATE = new Color(100, 116, 139);
    private static final Color[] CHART_COLORS = {TEAL, BLUE, AMBER, RED, PURPLE, CYAN, GREEN, SLATE};
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
            boolean admin = "ADMIN".equalsIgnoreCase(report.getReportType());
            List<Map<String, Object>> trend = asList(stats.get("monthlyTrend"));
            if (!trend.isEmpty()) {
                writer.subheading("Monthly trend");
                writer.barChart(trend, "label", admin ? "activityCount" : "serviceHours", admin ? "Activity count" : "Service hours", BLUE);
                writer.gap(10);
            }

            List<Map<String, Object>> categories = asList(stats.get(admin ? "activityTypeDistribution" : "categoryStats"));
            if (!categories.isEmpty()) {
                writer.subheading(admin ? "Activity type distribution" : "Service type distribution");
                writer.pieChart(categories, "category", "count");
                writer.gap(10);
            }

            List<Map<String, Object>> rank = asList(stats.get(admin ? "hotActivities" : "recentActivities"));
            if (!rank.isEmpty()) {
                writer.subheading(admin ? "Hot activities ranking" : "Recent service ranking");
                writer.horizontalBarChart(rank, "name", admin ? "registrationCount" : "serviceHours", admin ? "Registrations" : "Hours");
                writer.gap(10);
            }

            writer.subheading("Raw chart data");
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                if ("overview".equals(entry.getKey()) || "adjustments".equals(entry.getKey())) continue;
                writer.line(entry.getKey() + ": " + shortValue(entry.getValue()), 9, false);
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) result.add((Map<String, Object>) map);
            }
            return result;
        }
        return List.of();
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
        List<String> paths = new ArrayList<>();
        String configuredFont = System.getenv("REPORT_PDF_FONT_PATH");
        if (configuredFont != null && !configuredFont.isBlank()) {
            paths.add(configuredFont);
        }
        paths.addAll(List.of(
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/Deng.ttf",
                "C:/Windows/Fonts/simfang.ttf",
                "C:/Windows/Fonts/simkai.ttf",
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/msyh.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
                "/usr/share/fonts/truetype/arphic/uming.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.ttc"
        ));
        paths.addAll(discoverFontPaths());
        for (String path : paths) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    PDFont loaded = loadPdfFont(document, file);
                    if (loaded != null) return new FontPack(loaded, true);
                }
            } catch (Exception ignored) {
            }
        }
        return new FontPack(new PDType1Font(Standard14Fonts.FontName.HELVETICA), false);
    }

    private PDFont loadPdfFont(PDDocument document, File file) throws Exception {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".ttc")) {
            TrueTypeFont[] selected = new TrueTypeFont[1];
            try (TrueTypeCollection collection = new TrueTypeCollection(file)) {
                collection.processAllFonts(font -> {
                    if (selected[0] == null) {
                        selected[0] = font;
                    }
                });
            }
            if (selected[0] != null) {
                return PDType0Font.load(document, selected[0], false);
            }
            return null;
        }
        try (InputStream input = Files.newInputStream(file.toPath())) {
            return PDType0Font.load(document, input, false);
        }
    }

    private List<String> discoverFontPaths() {
        List<String> roots = List.of(
                "C:/Windows/Fonts",
                "/usr/share/fonts",
                "/usr/local/share/fonts",
                "/app/fonts"
        );
        List<String> keywords = List.of(
                "noto", "cjk", "sourcehan", "wqy", "wenquanyi",
                "simhei", "simsun", "simfang", "deng", "msyh"
        );
        List<String> result = new ArrayList<>();
        for (String root : roots) {
            Path rootPath = Path.of(root);
            if (!Files.exists(rootPath)) continue;
            try (var stream = Files.walk(rootPath, 5)) {
                stream
                        .filter(Files::isRegularFile)
                        .map(Path::toString)
                        .filter(path -> {
                            String lower = path.toLowerCase();
                            return (lower.endsWith(".ttf") || lower.endsWith(".ttc"))
                                    && keywords.stream().anyMatch(lower::contains);
                        })
                        .forEach(result::add);
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private record FontPack(PDFont font, boolean unicode) {}

    private static class PdfWriter {
        private final PDDocument document;
        private final PDFont font;
        private final boolean unicode;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;
        private final float left = 54;
        private final float right = 54;
        private final float pageWidth = PDRectangle.A4.getWidth();

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
                stream.newLineAtOffset(left, y);
                stream.showText(renderText(line));
                stream.endText();
                y -= size + 7;
            }
        }

        void barChart(List<Map<String, Object>> rows, String labelKey, String valueKey, String unit, Color color) throws Exception {
            if (rows == null || rows.isEmpty()) return;
            ensureSpace(185);
            List<Map<String, Object>> view = rows.size() > 8 ? rows.subList(rows.size() - 8, rows.size()) : rows;
            float chartX = left;
            float chartY = y - 150;
            float chartW = pageWidth - left - right;
            float chartH = 120;
            double max = max(view, valueKey);
            drawAxis(chartX, chartY, chartW, chartH);

            float gap = 10;
            float barW = Math.max(16, (chartW - gap * (view.size() + 1)) / view.size());
            for (int i = 0; i < view.size(); i++) {
                Map<String, Object> row = view.get(i);
                double value = number(row.get(valueKey));
                float h = (float) Math.max(3, value * chartH / max);
                float x = chartX + gap + i * (barW + gap);
                stream.setNonStrokingColor(color);
                stream.addRect(x, chartY, barW, h);
                stream.fill();
                tinyText(shortLabel(row.get(labelKey)), x - 3, chartY - 14, 7, Color.DARK_GRAY);
                tinyText(trimNumber(value), x, chartY + h + 6, 7, color);
            }
            tinyText(unit, chartX, chartY + chartH + 16, 8, Color.DARK_GRAY);
            y = chartY - 34;
        }

        void horizontalBarChart(List<Map<String, Object>> rows, String labelKey, String valueKey, String unit) throws Exception {
            if (rows == null || rows.isEmpty()) return;
            ensureSpace(205);
            List<Map<String, Object>> view = rows.size() > 6 ? rows.subList(0, 6) : rows;
            float chartX = left;
            float chartY = y - 18;
            float labelW = 155;
            float barW = pageWidth - left - right - labelW - 60;
            double max = max(view, valueKey);

            for (int i = 0; i < view.size(); i++) {
                Map<String, Object> row = view.get(i);
                double value = number(row.get(valueKey));
                float rowY = chartY - i * 24;
                tinyText(chartLabel(row.get(labelKey), i, "Activity"), chartX, rowY, 8, Color.DARK_GRAY);
                stream.setNonStrokingColor(new Color(229, 231, 235));
                stream.addRect(chartX + labelW, rowY - 2, barW, 10);
                stream.fill();
                stream.setNonStrokingColor(CHART_COLORS[i % CHART_COLORS.length]);
                stream.addRect(chartX + labelW, rowY - 2, (float) Math.max(4, value * barW / max), 10);
                stream.fill();
                tinyText(trimNumber(value) + " " + unit, chartX + labelW + barW + 8, rowY, 8, CHART_COLORS[i % CHART_COLORS.length]);
            }
            y = chartY - view.size() * 24 - 16;
        }

        void pieChart(List<Map<String, Object>> rows, String labelKey, String valueKey) throws Exception {
            if (rows == null || rows.isEmpty()) return;
            ensureSpace(185);
            List<Map<String, Object>> view = rows.size() > 6 ? rows.subList(0, 6) : rows;
            double total = view.stream().mapToDouble(row -> number(row.get(valueKey))).sum();
            if (total <= 0) return;
            float cx = left + 95;
            float cy = y - 82;
            float radius = 62;
            double start = 0;
            for (int i = 0; i < view.size(); i++) {
                double value = number(view.get(i).get(valueKey));
                double extent = value * 360 / total;
                drawPieSlice(cx, cy, radius, start, extent, CHART_COLORS[i % CHART_COLORS.length]);
                start += extent;
            }
            stream.setNonStrokingColor(Color.WHITE);
            stream.addRect(cx - 1, cy - 1, 2, 2);
            stream.fill();

            float legendX = left + 210;
            float legendY = y - 22;
            for (int i = 0; i < view.size(); i++) {
                Map<String, Object> row = view.get(i);
                double value = number(row.get(valueKey));
                double percent = value * 100 / total;
                float itemY = legendY - i * 20;
                stream.setNonStrokingColor(CHART_COLORS[i % CHART_COLORS.length]);
                stream.addRect(legendX, itemY - 8, 9, 9);
                stream.fill();
                tinyText(chartLabel(row.get(labelKey), i, "Category") + "  " + trimNumber(value) + " (" + trimNumber(percent) + "%)", legendX + 16, itemY - 1, 8, Color.DARK_GRAY);
            }
            y -= 166;
        }

        private void drawAxis(float x, float y, float w, float h) throws Exception {
            stream.setStrokingColor(new Color(209, 213, 219));
            stream.setLineWidth(0.7f);
            stream.moveTo(x, y);
            stream.lineTo(x + w, y);
            stream.moveTo(x, y);
            stream.lineTo(x, y + h);
            stream.stroke();
        }

        private void drawPieSlice(float cx, float cy, float r, double startDeg, double extentDeg, Color color) throws Exception {
            stream.setNonStrokingColor(color);
            stream.moveTo(cx, cy);
            int steps = Math.max(6, (int) Math.ceil(Math.abs(extentDeg) / 8));
            for (int i = 0; i <= steps; i++) {
                double angle = Math.toRadians(startDeg + extentDeg * i / steps - 90);
                float x = cx + (float) Math.cos(angle) * r;
                float y2 = cy + (float) Math.sin(angle) * r;
                stream.lineTo(x, y2);
            }
            stream.closePath();
            stream.fill();
        }

        private void tinyText(String text, float x, float y, int size, Color color) throws Exception {
            if (y < 48) return;
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, y);
            stream.showText(renderText(text));
            stream.endText();
            stream.setNonStrokingColor(Color.BLACK);
        }

        private void ensureSpace(float height) throws Exception {
            if (y - height < 58) newPage();
        }

        private double max(List<Map<String, Object>> rows, String key) {
            return Math.max(1, rows.stream().mapToDouble(row -> number(row.get(key))).max().orElse(1));
        }

        private double number(Object value) {
            if (value instanceof Number number) return number.doubleValue();
            if (value == null) return 0;
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (Exception ignored) {
                return 0;
            }
        }

        private String trimNumber(double value) {
            if (Math.abs(value - Math.round(value)) < 0.01) return String.valueOf(Math.round(value));
            return String.format(java.util.Locale.US, "%.1f", value);
        }

        private String shortLabel(Object value) {
            String text = shortText(value, 8);
            return text.replace("2026-", "");
        }

        private String chartLabel(Object value, int index, String prefix) {
            String text = shortText(value, 18);
            if (!unicode && containsNonAscii(text)) {
                return prefix + " " + (index + 1);
            }
            return text;
        }

        private String shortText(Object value, int limit) {
            String text = value == null ? "-" : String.valueOf(value);
            return text.length() > limit ? text.substring(0, limit - 1) + "." : text;
        }

        private String renderText(String text) {
            if (unicode) return text == null ? "" : text;
            return asciiFallback(text);
        }

        private boolean containsNonAscii(String text) {
            if (text == null) return false;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) > 0x7E) return true;
            }
            return false;
        }

        private String asciiFallback(String text) {
            if (text == null || text.isBlank()) return "";
            return text.replaceAll("[^\\x20-\\x7E]+", " ").replaceAll("\\s+", " ").trim();
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
