package com.scs.volunteer.service;

import com.scs.volunteer.dto.UserProfileDTO;
import com.scs.volunteer.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {
    UserProfileVO profile(Long userId);
    UserProfileVO update(Long userId, UserProfileDTO dto);
    String updateAvatar(Long userId, MultipartFile file);
}
