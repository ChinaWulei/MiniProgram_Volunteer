package com.scs.volunteer.mapper;

import com.scs.volunteer.dto.RegisterDTO;
import com.scs.volunteer.vo.VolunteerVO;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class VolunteerMapper {
    private final JdbcTemplate jdbcTemplate;

    public VolunteerMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertProfile(Long userId, RegisterDTO dto) {
        jdbcTemplate.update("insert into volunteer_profile(user_id,college,major_class,skill_tags,available_time,total_hours,credit_score,service_count) values(?,?,?,?,?,0,100,0)",
                userId, dto.getCollege(), dto.getMajorClass(), dto.getSkillTags(), dto.getAvailableTime());
    }

    public List<VolunteerVO> search(String college, String majorClass, String skillTag, String keyword, String sortBy) {
        String orderBy = "p.credit_score desc,p.total_hours desc";
        if ("hours".equalsIgnoreCase(sortBy) || "totalHours".equalsIgnoreCase(sortBy)) {
            orderBy = "p.total_hours desc,p.credit_score desc";
        } else if ("points".equalsIgnoreCase(sortBy) || "creditScore".equalsIgnoreCase(sortBy)) {
            orderBy = "p.credit_score desc,p.total_hours desc";
        }
        String sql = """
                select u.id as user_id,u.name,u.nickname,u.avatar_url,u.identity_no,null as phone,
                       p.college,p.major_class,p.skill_tags,p.available_time,p.bio,p.total_hours,
                       p.credit_score,p.service_count,
                       case when p.total_hours >= 30 then 'Lv4 先锋志愿者'
                            when p.total_hours >= 15 then 'Lv3 骨干志愿者'
                            when p.total_hours >= 5 then 'Lv2 活跃志愿者'
                            else 'Lv1 新星志愿者' end as volunteer_level,
                       p.credit_score as volunteer_points,
                       (select a.name from registration r join activity a on r.activity_id=a.id
                        where r.user_id=u.id order by r.created_at desc limit 1) as recent_activity,
                       concat_ws(',', if(p.total_hours >= 5, '服务新星', null),
                                      if(p.total_hours >= 15, '学院骨干', null),
                                      if(p.credit_score >= 95, '高分志愿者', null)) as badges
                from user u join volunteer_profile p on u.id=p.user_id
                where u.role='VOLUNTEER'
                  and (? is null or p.college=?)
                  and (? is null or p.major_class like concat('%',?,'%'))
                  and (? is null or p.skill_tags like concat('%',?,'%'))
                  and (? is null or u.name like concat('%',?,'%') or u.nickname like concat('%',?,'%') or p.major_class like concat('%',?,'%') or p.skill_tags like concat('%',?,'%'))
                order by 
                """ + orderBy;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(VolunteerVO.class),
                emptyToNull(college), emptyToNull(college),
                emptyToNull(majorClass), emptyToNull(majorClass),
                emptyToNull(skillTag), emptyToNull(skillTag),
                emptyToNull(keyword), emptyToNull(keyword), emptyToNull(keyword), emptyToNull(keyword), emptyToNull(keyword));
    }

    public Optional<VolunteerVO> findByUserId(Long userId) {
        List<VolunteerVO> list = jdbcTemplate.query("""
                select u.id as user_id,u.name,u.nickname,u.avatar_url,u.identity_no,null as phone,
                       p.college,p.major_class,p.skill_tags,p.available_time,p.bio,p.total_hours,
                       p.credit_score,p.service_count,
                       case when p.total_hours >= 30 then 'Lv4 先锋志愿者'
                            when p.total_hours >= 15 then 'Lv3 骨干志愿者'
                            when p.total_hours >= 5 then 'Lv2 活跃志愿者'
                            else 'Lv1 新星志愿者' end as volunteer_level,
                       p.credit_score as volunteer_points,
                       (select a.name from registration r join activity a on r.activity_id=a.id
                        where r.user_id=u.id order by r.created_at desc limit 1) as recent_activity,
                       concat_ws(',', if(p.total_hours >= 5, '服务新星', null),
                                      if(p.total_hours >= 15, '学院骨干', null),
                                      if(p.credit_score >= 95, '高分志愿者', null)) as badges
                from user u join volunteer_profile p on u.id=p.user_id where u.id=?
                """, new BeanPropertyRowMapper<>(VolunteerVO.class), userId);
        return list.stream().findFirst();
    }

    public List<Map<String, Object>> historyActivities(Long userId) {
        return jdbcTemplate.queryForList("""
                select a.id,a.name,a.cover_image_url,a.category,a.location,a.start_time,a.end_time,
                       a.service_hours,r.status,r.created_at
                from registration r join activity a on r.activity_id=a.id
                where r.user_id=?
                order by r.created_at desc
                """, userId);
    }

    public void addService(Long userId, double hours) {
        jdbcTemplate.update("update volunteer_profile set total_hours=total_hours+?, service_count=service_count+1 where user_id=?", hours, userId);
    }

    public void adjustService(Long userId, double hourDelta, int serviceCountDelta) {
        jdbcTemplate.update("""
                update volunteer_profile
                set total_hours=greatest(total_hours + ?, 0),
                    service_count=greatest(service_count + ?, 0)
                where user_id=?
                """, hourDelta, serviceCountDelta, userId);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
