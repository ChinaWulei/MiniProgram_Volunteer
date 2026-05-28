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
import java.util.Set;
import java.util.UUID;

@Service
public class S3StorageService {
    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024;
    private static final long ACTIVITY_COVER_MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final String SERVICE = "s3";
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final S3Properties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public S3StorageService(S3Properties properties) {
        this.properties = properties;
    }

    public String uploadAvatar(MultipartFile file, Long userId) {
        String extension = extension(file.getOriginalFilename());
        String key = "avatars/" + userId + "/" + UUID.randomUUID() + "." + extension;
        return upload(file, key, AVATAR_MAX_SIZE, "头像");
    }

    public String uploadActivityCover(MultipartFile file) {
        String extension = extension(file.getOriginalFilename());
        String key = "activity/activity-cover/" + System.currentTimeMillis() + "-" + UUID.randomUUID() + "." + extension;
        return upload(file, key, ACTIVITY_COVER_MAX_SIZE, "活动封面");
    }

    public String uploadActivityCoverBytes(byte[] payload, String extension, String contentType) {
        String safeExtension = extension == null || extension.isBlank() ? "png" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(safeExtension)) {
            safeExtension = "png";
        }
        String key = "activity/activity-cover/ai/" + System.currentTimeMillis() + "-" + UUID.randomUUID() + "." + safeExtension;
        String safeContentType = contentType == null || contentType.isBlank() ? MediaType.IMAGE_PNG_VALUE : contentType;
        return upload(payload, key, ACTIVITY_COVER_MAX_SIZE, safeContentType, "AI娲诲姩灏侀潰");
    }

    public String uploadActivityNewsImage(MultipartFile file) {
        String extension = extension(file.getOriginalFilename());
        String key = "activity/news/" + System.currentTimeMillis() + "-" + UUID.randomUUID() + "." + extension;
        return upload(file, key, ACTIVITY_COVER_MAX_SIZE, "活动新闻图片");
    }

    private String upload(MultipartFile file, String key, long maxSize, String label) {
        validate(file, maxSize, label);
        checkConfig();
        byte[] payload;
        try {
            payload = file.getBytes();
        } catch (IOException e) {
            throw new BizException(label + "读取失败");
        }

        String contentType = file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType();
        return upload(payload, key, maxSize, contentType, label);
    }

    private String upload(byte[] payload, String key, long maxSize, String contentType, String label) {
        if (payload == null || payload.length == 0) {
            throw new BizException(label + "鍥剧墖涓嶈兘涓虹┖");
        }
        if (payload.length > maxSize) {
            throw new BizException(label + "涓嶈兘瓒呰繃 " + (maxSize / 1024 / 1024) + "MB");
        }
        checkConfig();

        String host = properties.getBucket() + ".s3." + properties.getRegion() + ".amazonaws.com";
        String url = "https://" + host + "/" + encodeKey(key);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = AMZ_DATE.format(now);
        String date = DATE.format(now);
        String payloadHash = sha256Hex(payload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("Host", host);
        headers.set("x-amz-acl", "public-read");
        headers.set("x-amz-content-sha256", payloadHash);
        headers.set("x-amz-date", amzDate);
        headers.set("Authorization", authorization("PUT", "/" + encodeKey(key), host, contentType, payloadHash, amzDate, date));
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload, headers), String.class);
        return url;
    }

    private String authorization(String method, String uri, String host, String contentType, String payloadHash, String amzDate, String date) {
        String signedHeaders = "content-type;host;x-amz-acl;x-amz-content-sha256;x-amz-date";
        String canonicalHeaders = "content-type:" + contentType + "\n"
                + "host:" + host + "\n"
                + "x-amz-acl:public-read\n"
                + "x-amz-content-sha256:" + payloadHash + "\n"
                + "x-amz-date:" + amzDate + "\n";
        String canonicalRequest = method + "\n" + uri + "\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;
        String credentialScope = date + "/" + properties.getRegion() + "/" + SERVICE + "/aws4_request";
        String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        byte[] signingKey = signingKey(date);
        String signature = hmacHex(signingKey, stringToSign);
        return ALGORITHM + " Credential=" + properties.getAccessKey() + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    private byte[] signingKey(String date) {
        byte[] kDate = hmac(("AWS4" + properties.getSecretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] kRegion = hmac(kDate, properties.getRegion());
        byte[] kService = hmac(kRegion, SERVICE);
        return hmac(kService, "aws4_request");
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

    private String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new BizException("S3 签名失败");
        }
    }

    private String encodeKey(String key) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    private void validate(MultipartFile file, long maxSize, String label) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择" + label + "图片");
        }
        if (file.getSize() > maxSize) {
            throw new BizException(label + "不能超过 " + (maxSize / 1024 / 1024) + "MB");
        }
        if (!ALLOWED_EXTENSIONS.contains(extension(file.getOriginalFilename()))) {
            throw new BizException("仅支持 jpg、jpeg、png、webp");
        }
    }

    private void checkConfig() {
        if (blank(properties.getBucket()) || blank(properties.getRegion()) || blank(properties.getAccessKey()) || blank(properties.getSecretKey())) {
            throw new BizException("AWS S3 配置不完整");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
