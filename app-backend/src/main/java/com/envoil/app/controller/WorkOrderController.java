package com.envoil.app.controller;

import com.envoil.app.common.ApiResponse;
import com.envoil.app.model.WorkOrderFinishRequest;
import com.envoil.app.service.AppBizWorkOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/work-order")
public class WorkOrderController {

    private final AppBizWorkOrderService workOrderService;

    public WorkOrderController(AppBizWorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam String openid) {
        return ApiResponse.ok(workOrderService.listWorkOrders(openid));
    }

    @PostMapping("/{workOrderNo}/receive")
    public ApiResponse<?> receive(@PathVariable String workOrderNo, @RequestParam String openid) {
        Map<String, Object> data = workOrderService.receiveWorkOrder(openid, workOrderNo);
        if (data == null) {
            return ApiResponse.fail("未找到工单或无权限");
        }
        return ApiResponse.ok(data);
    }

    @PostMapping("/{workOrderNo}/assign")
    public ApiResponse<?> assign(
            @PathVariable String workOrderNo,
            @RequestParam String openid,
            @RequestParam long salesmanId) {
        Map<String, Object> data = workOrderService.assignWorkOrder(openid, workOrderNo, salesmanId);
        if (data == null) {
            return ApiResponse.fail("未找到工单或无权限");
        }
        return ApiResponse.ok(data);
    }

    @PostMapping("/{workOrderNo}/finish")
    public ApiResponse<?> finish(
            @PathVariable String workOrderNo,
            @RequestParam String openid,
            @RequestBody(required = false) WorkOrderFinishRequest body) {
        Map<String, Object> data = workOrderService.finishWorkOrder(openid, workOrderNo, body);
        if (data == null) {
            return ApiResponse.fail("未找到工单或无权限");
        }
        return ApiResponse.ok(data);
    }
}
