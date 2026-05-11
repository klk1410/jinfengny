package com.envoil.app.controller;

import com.envoil.app.common.ApiResponse;
import com.envoil.app.model.PromoCoopCreateRequest;
import com.envoil.app.model.PromoPrepayCreateRequest;
import com.envoil.app.model.PromoWithdrawApplyRequest;
import com.envoil.app.service.AppPromoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promo")
public class PromoController {

    private final AppPromoService promoService;

    public PromoController(AppPromoService promoService) {
        this.promoService = promoService;
    }

    @GetMapping("/coops")
    public ApiResponse<?> coops(@RequestParam String openid) {
        return ApiResponse.ok(promoService.listCoops(openid));
    }

    @PostMapping("/coops")
    public ApiResponse<?> createCoop(@Validated @RequestBody PromoCoopCreateRequest req) {
        promoService.createCoop(req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/withdraws")
    public ApiResponse<?> withdraws(@RequestParam String openid) {
        return ApiResponse.ok(promoService.listWithdraws(openid));
    }

    @PostMapping("/withdraws")
    public ApiResponse<?> applyWithdraw(@Validated @RequestBody PromoWithdrawApplyRequest req) {
        promoService.applyWithdraw(req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/withdraws/{id}/approve")
    public ApiResponse<?> approveWithdraw(@PathVariable("id") long id, @RequestParam String openid) {
        promoService.approveWithdraw(openid, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/withdraws/{id}/reject")
    public ApiResponse<?> rejectWithdraw(
            @PathVariable("id") long id,
            @RequestParam String openid,
            @RequestParam(required = false) String remark) {
        promoService.rejectWithdraw(openid, id, remark);
        return ApiResponse.ok(null);
    }

    @GetMapping("/prepaids")
    public ApiResponse<?> prepaids(@RequestParam String openid) {
        return ApiResponse.ok(promoService.listPrepaids(openid));
    }

    @PostMapping("/prepaids")
    public ApiResponse<?> createPrepay(@Validated @RequestBody PromoPrepayCreateRequest req) {
        promoService.createPrepay(req);
        return ApiResponse.ok(null);
    }
}
