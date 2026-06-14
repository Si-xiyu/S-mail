package com.smartmail.user.service;

import com.smartmail.user.dto.UpdateUserSettingRequest;
import com.smartmail.user.dto.UserSettingResponse;
import com.smartmail.user.entity.UserSetting;
import com.smartmail.user.mapper.UserSettingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserSettingService {
    private final UserSettingMapper settingMapper;

    public UserSettingService(UserSettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    @Transactional
    public UserSetting ensure(Long userId) {
        UserSetting setting = settingMapper.findByUserId(userId);
        if (setting != null) {
            return setting;
        }
        LocalDateTime now = LocalDateTime.now();
        setting = new UserSetting();
        setting.setUserId(userId);
        setting.setAiEnabled(true);
        setting.setAgentAutoWriteEnabled(false);
        setting.setCreatedAt(now);
        setting.setUpdatedAt(now);
        settingMapper.insert(setting);
        return setting;
    }

    @Transactional
    public UserSettingResponse update(Long userId, UpdateUserSettingRequest request) {
        UserSetting setting = ensure(userId);
        if (request.aiEnabled() != null) {
            setting.setAiEnabled(request.aiEnabled());
        }
        if (request.agentAutoWriteEnabled() != null) {
            setting.setAgentAutoWriteEnabled(request.agentAutoWriteEnabled());
        }
        setting.setUpdatedAt(LocalDateTime.now());
        settingMapper.updateById(setting);
        return toResponse(setting);
    }

    public UserSettingResponse toResponse(UserSetting setting) {
        return new UserSettingResponse(setting.getAiEnabled(), setting.getAgentAutoWriteEnabled());
    }
}
