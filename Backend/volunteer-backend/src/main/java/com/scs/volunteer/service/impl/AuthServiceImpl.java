package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.dto.LoginDTO;
import com.scs.volunteer.dto.RegisterDTO;
import com.scs.volunteer.entity.User;
import com.scs.volunteer.mapper.UserMapper;
import com.scs.volunteer.mapper.VolunteerMapper;
import com.scs.volunteer.service.AuthService;
import com.scs.volunteer.vo.LoginVO;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final VolunteerMapper volunteerMapper;

    public AuthServiceImpl(UserMapper userMapper, VolunteerMapper volunteerMapper) {
        this.userMapper = userMapper;
        this.volunteerMapper = volunteerMapper;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.findByUsername(dto.getUsername()).orElseThrow(() -> new BizException("账号不存在"));
        if (!user.getPassword().equals(dto.getPassword())) {
            throw new BizException("密码错误");
        }
        user.setPassword(null);
        return new LoginVO("volunteer-token-" + user.getId() + "-" + user.getRole(), user);
    }

    @Override
    public LoginVO register(RegisterDTO dto) {
        userMapper.findByUsername(dto.getUsername()).ifPresent(u -> {
            throw new BizException("账号已存在");
        });
        dto.setRole("VOLUNTEER");
        Long userId = userMapper.insert(dto);
        if ("VOLUNTEER".equals(dto.getRole())) {
            volunteerMapper.insertProfile(userId, dto);
        }
        return login(toLogin(dto));
    }

    private LoginDTO toLogin(RegisterDTO dto) {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername(dto.getUsername());
        loginDTO.setPassword(dto.getPassword());
        return loginDTO;
    }
}
