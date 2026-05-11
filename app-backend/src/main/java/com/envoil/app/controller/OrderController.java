package com.envoil.app.controller;

import com.envoil.app.common.ApiResponse;
import com.envoil.app.model.OrderCreateRequest;
import com.envoil.app.service.AppBizOrderService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final AppBizOrderService appBizOrderService;

    public OrderController(AppBizOrderService appBizOrderService) {
        this.appBizOrderService = appBizOrderService;
    }

    @PostMapping("/create")
    public ApiResponse<?> create(@Validated @RequestBody OrderCreateRequest request) {
        return ApiResponse.ok(appBizOrderService.createOrder(request));
    }

    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam String openid) {
        return ApiResponse.ok(appBizOrderService.listOrders(openid));
    }

    @PostMapping("/confirm/{orderNo}")
    public ApiResponse<?> confirm(@PathVariable String orderNo, @RequestParam String openid) {
        Map<String, Object> data = appBizOrderService.confirmOrder(openid, orderNo);
        if (data == null) {
            return ApiResponse.fail("未找到订单或无确认权限");
        }
        return ApiResponse.ok(data);
    }

    @PostMapping("/cancel/{orderNo}")
    public ApiResponse<?> cancel(@PathVariable String orderNo, @RequestParam String openid) {
        Map<String, Object> data = appBizOrderService.cancelOrder(openid, orderNo);
        if (data == null) {
            return ApiResponse.fail("未找到订单或无取消权限");
        }
        return ApiResponse.ok(data);
    }
}
