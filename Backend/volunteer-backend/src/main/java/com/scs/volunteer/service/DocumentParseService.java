package com.scs.volunteer.service;

import com.scs.volunteer.common.BizException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class DocumentParseService {
    public String parse(MultipartFile file) {
        return parse(file, file == null ? null : file.getOriginalFilename());
    }

    public String parse(MultipartFile file, String filename) {
        String extension = extension(filename);
        try {
            byte[] bytes = file.getBytes();
            if ("pdf".equals(extension)) return parsePdf(bytes);
            if ("docx".equals(extension)) return parseDocx(bytes);
            if ("txt".equals(extension)) return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException("规则文件解析失败：" + e.getMessage());
        }
        throw new BizException("不支持的规则文件格式");
    }

    private String parsePdf(byte[] bytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String parseDocx(byte[] bytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
