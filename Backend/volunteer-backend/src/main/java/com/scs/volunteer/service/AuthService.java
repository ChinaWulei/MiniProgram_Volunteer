package com.scs.volunteer.service;

import com.scs.volunteer.dto.LoginDTO;
import com.scs.volunteer.dto.RegisterDTO;
import com.scs.volunteer.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO dto);
    LoginVO register(RegisterDTO dto);
}
