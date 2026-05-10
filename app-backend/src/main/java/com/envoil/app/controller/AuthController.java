package com.envoil.app.controller;

import com.envoil.app.common.ApiResponse;
import com.envoil.app.model.WechatLoginRequest;
import com.envoil.app.service.AppMockService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AppMockService appMockService;

    public AuthController(AppMockService appMockService) {
        this.appMockService = appMockService;
    }

    @PostMapping("/wechat-login")
    public ApiResponse<?> wechatLogin(@Validated @RequestBody WechatLoginRequest request) {
        return ApiResponse.ok(appMockService.loginByOpenid(request.getOpenid()));
    }
}
