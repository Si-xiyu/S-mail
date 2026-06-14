package com.smartmail.user.controller;

import com.smartmail.common.response.ApiResponse;
import com.smartmail.common.security.UserContext;
import com.smartmail.user.dto.UpdateUserSettingRequest;
import com.smartmail.user.dto.UserProfileResponse;
import com.smartmail.user.dto.UserSettingResponse;
import com.smartmail.user.service.UserService;
import com.smartmail.user.service.UserSettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final UserSettingService settingService;

    public UserController(UserService userService, UserSettingService settingService) {
        this.userService = userService;
        this.settingService = settingService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        return ApiResponse.ok(userService.me());
    }

    @PatchMapping("/me/settings")
    public ApiResponse<UserSettingResponse> updateSettings(@RequestBody UpdateUserSettingRequest request) {
        return ApiResponse.ok(settingService.update(UserContext.requireUserId(), request));
    }
}
