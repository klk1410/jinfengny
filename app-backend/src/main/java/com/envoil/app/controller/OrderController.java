package com.envoil.app.controller;

import com.envoil.app.common.ApiResponse;
import com.envoil.app.model.OrderCreateRequest;
import com.envoil.app.service.AppMockService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final AppMockService appMockService;

    public OrderController(AppMockService appMockService) {
        this.appMockService = appMockService;
    }

    @PostMapping("/create")
    public ApiResponse<?> create(@Validated @RequestBody OrderCreateRequest request) {
        return ApiResponse.ok(appMockService.createOrder(request));
    }

    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam String openid) {
        return ApiResponse.ok(appMockService.listOrders(openid));
    }

    @PostMapping("/cancel/{orderNo}")
    public ApiResponse<?> cancel(@PathVariable String orderNo, @RequestParam String openid) {
        Object data = appMockService.cancelOrder(openid, orderNo);
        if (data == null) {
            return ApiResponse.fail("未找到订单或无取消权限");
        }
        return ApiResponse.ok(data);
    }
}
