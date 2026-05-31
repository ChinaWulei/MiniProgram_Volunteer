package com.scs.volunteer.service;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.config.S3Properties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class S3Service {
    private static final long RULE_FILE_MAX_SIZE = 20 * 1024 * 1024;
    private static final String SERVICE = "s3";
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final S3Properties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public S3Service(S3Properties properties) {
        this.properties = properties;
    }

    public Map<String, String> uploadRuleFile(MultipartFile file) {
        return uploadRuleFile(file, file == null ? null : file.getOriginalFilename());
    }

    public Map<String, String> uploadRuleFile(MultipartFile file, String originalFilename) {
        validate(file, originalFilename);
        String extension = extension(originalFilename);
        String key = "rules/" + System.currentTimeMillis() + "-" + UUID.randomUUID() + "." + extension;
        return Map.of("key", key, "url", upload(file, key));
    }

    public String publicUrl(String key) {
        checkConfig();
        return "https://" + host() + "/" + encodeKey(key);
    }

    private String upload(MultipartFile file, String key) {
        checkConfig();
        byte[] payload;
        try {
            payload = file.getBytes();
        } catch (IOException e) {
            throw new BizException("规则文件读取失败");
        }
        String url = publicUrl(key);
        String contentType = file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType();
        SignedHeaders signed = signedHeaders("PUT", "/" + encodeKey(key), contentType, payload);
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload, signed.headers()), String.class);
        return url;
    }

    private SignedHeaders signedHeaders(String method, String uri, String contentType, byte[] payload) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = AMZ_DATE.format(now);
        String date = DATE.format(now);
        String payloadHash = sha256Hex(payload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("Host", host());
        headers.set("x-amz-acl", "public-read");
        headers.set("x-amz-content-sha256", payloadHash);
        headers.set("x-amz-date", amzDate);
        headers.set("Authorization", authorization(method, uri, contentType, payloadHash, amzDate, date));
        return new SignedHeaders(headers);
    }

    private String authorization(String method, String uri, String contentType, String payloadHash, String amzDate, String date) {
        String signedHeaders = "content-type;host;x-amz-acl;x-amz-content-sha256;x-amz-date";
        String canonicalHeaders = "content-type:" + contentType + "\n"
                + "host:" + host() + "\n"
                + "x-amz-acl:public-read\n"
                + "x-amz-content-sha256:" + payloadHash + "\n"
                + "x-amz-date:" + amzDate + "\n";
        String canonicalRequest = method + "\n" + uri + "\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;
        String credentialScope = date + "/" + properties.getRegion() + "/" + SERVICE + "/aws4_request";
        String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        String signature = hmacHex(signingKey(date), stringToSign);
        return ALGORITHM + " Credential=" + properties.getAccessKey() + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    private byte[] signingKey(String date) {
        byte[] kDate = hmac(("AWS4" + properties.getSecretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] kRegion = hmac(kDate, properties.getRegion());
        byte[] kService = hmac(kRegion, SERVICE);
        return hmac(kService, "aws4_request");
    }

    private String host() {
        return properties.getBucket() + ".s3." + properties.getRegion() + ".amazonaws.com";
    }

    private void validate(MultipartFile file, String filename) {
        if (file == null || file.isEmpty()) throw new BizException("请选择规则文件");
        if (file.getSize() > RULE_FILE_MAX_SIZE) throw new BizException("规则文件不能超过20MB");
        String extension = extension(filename);
        if (!extension.equals("pdf") && !extension.equals("docx") && !extension.equals("txt")) {
            throw new BizException("仅支持 PDF、DOCX、TXT 规则文件");
        }
    }

    private void checkConfig() {
        if (blank(properties.getBucket()) || blank(properties.getRegion()) || blank(properties.getAccessKey()) || blank(properties.getSecretKey())) {
            throw new BizException("AWS S3 配置不完整");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String encodeKey(String key) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    private String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new BizException("S3 签名失败");
        }
    }

    private String hmacHex(byte[] key, String data) {
        return HexFormat.of().formatHex(hmac(key, data));
    }

    private byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BizException("S3 签名失败");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record SignedHeaders(HttpHeaders headers) {}
}
