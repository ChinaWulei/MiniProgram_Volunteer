package com.scs.volunteer.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.mapper.CourseScheduleMapper;
import com.scs.volunteer.service.AiModelClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course-schedules")
public class CourseScheduleController extends BaseController {
    private final CourseScheduleMapper courseScheduleMapper;
    private final AiModelClient aiModelClient;
    private final ObjectMapper objectMapper;

    public CourseScheduleController(CourseScheduleMapper courseScheduleMapper, AiModelClient aiModelClient,
                                    ObjectMapper objectMapper) {
        this.courseScheduleMapper = courseScheduleMapper;
        this.aiModelClient = aiModelClient;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(HttpServletRequest request) {
        return ApiResponse.ok(courseScheduleMapper.list(currentUser(request).getId()));
    }

    @PutMapping
    public ApiResponse<Void> replace(@RequestBody Map<String, List<Map<String, Object>>> body,
                                     HttpServletRequest request) {
        try {
            courseScheduleMapper.replace(currentUser(request).getId(), body == null ? List.of() : body.get("courses"));
        } catch (Exception e) {
            throw new BizException("课程安排格式不正确，请检查星期和上课时间");
        }
        return ApiResponse.ok(null);
    }

    @PostMapping("/parse-image")
    public ApiResponse<Map<String, Object>> parseImage(@RequestParam("file") MultipartFile file,
                                                       HttpServletRequest request) {
        currentUser(request);
        if (file == null || file.isEmpty()) throw new BizException("请选择课表图片");
        if (file.getSize() > 8 * 1024 * 1024) throw new BizException("课表图片不能超过8MB");
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new BizException("仅支持上传图片格式的课表");
        }
        if (!aiModelClient.available()) throw new BizException("AI图片识别服务未配置");
        try {
            String answer = aiModelClient.chatWithImage("""
                    请判断图片是否为大学课程表，并识别其中的课程安排。
                    只输出JSON对象，不要markdown和解释，格式固定为：
                    {"isSchedule":true,"courses":[
                      {"courseName":"课程名称","weekday":1,"startTime":"08:00","endTime":"09:40","location":"地点"}
                    ]}
                    如果图片不是课程表，必须返回：{"isSchedule":false,"courses":[]}
                    weekday取值1至7，分别表示周一至周日。
                    时间必须为24小时HH:mm格式。无法确定的课程不要编造；地点看不清可为空字符串。
                    如果同一课程一周有多个上课时段，输出多条记录。
                    本校课表中的“A节”从晚上19:15开始；识别到A节时，startTime必须填写为"19:15"。
                    A节结束时间应以图片中的节次或时间信息为准，无法确认结束时间时不要生成该条记录。
                    """, file.getBytes(), file.getContentType());
            Map<String, Object> result = objectMapper.readValue(sanitizeJson(answer), new TypeReference<>() {});
            if (!Boolean.TRUE.equals(result.get("isSchedule"))) {
                throw new BizException("上传的图片不是有效的课程表");
            }
            List<Map<String, Object>> courses = objectMapper.convertValue(
                    result.getOrDefault("courses", List.of()), new TypeReference<>() {});
            if (courses.isEmpty()) {
                throw new BizException("未从课表中识别到有效课程，请上传更清晰的图片");
            }
            return ApiResponse.ok(Map.of("courses", courses));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("课表识别失败，请重新上传清晰截图");
        }
    }

    private String sanitizeJson(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return start >= 0 && end > start ? text.substring(start, end + 1) : text;
    }
}
