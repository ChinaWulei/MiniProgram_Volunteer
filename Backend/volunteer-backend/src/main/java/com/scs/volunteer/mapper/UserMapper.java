package com.scs.volunteer.mapper;

import com.scs.volunteer.dto.RegisterDTO;
import com.scs.volunteer.dto.UserProfileDTO;
import com.scs.volunteer.entity.User;
import com.scs.volunteer.vo.UserProfileVO;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserMapper {
    private final JdbcTemplate jdbcTemplate;

    public UserMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByUsername(String username) {
        List<User> list = jdbcTemplate.query("select * from user where username=?",
                new BeanPropertyRowMapper<>(User.class), username);
        return list.stream().findFirst();
    }

    public Optional<User> findById(Long id) {
        List<User> list = jdbcTemplate.query("select * from user where id=?",
                new BeanPropertyRowMapper<>(User.class), id);
        return list.stream().findFirst();
    }

    public Long insert(RegisterDTO dto) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into user(username,password,name,nickname,identity_no,phone,role) values(?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, dto.getUsername());
            ps.setString(2, dto.getPassword());
            ps.setString(3, dto.getName());
            ps.setString(4, dto.getName());
            ps.setString(5, dto.getIdentityNo());
            ps.setString(6, dto.getPhone());
            ps.setString(7, dto.getRole() == null ? "VOLUNTEER" : dto.getRole());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<UserProfileVO> findProfile(Long userId) {
        List<UserProfileVO> list = jdbcTemplate.query("""
                select u.id as user_id,u.avatar_url,u.nickname,u.name,u.identity_no as volunteer_no,u.phone,
                       p.college,p.major_class,p.skill_tags,p.available_time,p.bio,
                       p.total_hours,p.credit_score,p.service_count
                from user u left join volunteer_profile p on u.id=p.user_id
                where u.id=?
                """, new BeanPropertyRowMapper<>(UserProfileVO.class), userId);
        return list.stream().findFirst();
    }

    public void updateProfile(Long userId, UserProfileDTO dto) {
        jdbcTemplate.update("update user set nickname=?, phone=? where id=?",
                dto.getNickname(), dto.getPhone(), userId);
        Integer count = jdbcTemplate.queryForObject("select count(*) from volunteer_profile where user_id=?", Integer.class, userId);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                    update volunteer_profile set college=?,major_class=?,skill_tags=?,available_time=?,bio=?
                    where user_id=?
                    """, dto.getCollege(), dto.getMajorClass(), dto.getSkillTags(), dto.getAvailableTime(), dto.getBio(), userId);
        } else {
            jdbcTemplate.update("""
                    insert into volunteer_profile(user_id,college,major_class,skill_tags,available_time,bio,total_hours,credit_score,service_count)
                    values(?,?,?,?,?,?,0,100,0)
                    """, userId, dto.getCollege(), dto.getMajorClass(), dto.getSkillTags(), dto.getAvailableTime(), dto.getBio());
        }
    }

    public void updateAvatar(Long userId, String avatarUrl) {
        jdbcTemplate.update("update user set avatar_url=? where id=?", avatarUrl, userId);
    }

    public int campusRank(Long userId) {
        Integer rank = jdbcTemplate.queryForObject("""
                select count(*) + 1
                from volunteer_profile p
                join volunteer_profile me on me.user_id=?
                where p.total_hours > me.total_hours
                   or (p.total_hours = me.total_hours and p.service_count > me.service_count)
                   or (p.total_hours = me.total_hours and p.service_count = me.service_count and p.user_id < me.user_id)
                """, Integer.class, userId);
        return rank == null ? 0 : rank;
    }

    public int registrationCount(Long userId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from registration where user_id=?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    public List<Long> volunteerIds() {
        return jdbcTemplate.queryForList("select id from user where role='VOLUNTEER'", Long.class);
    }
}
