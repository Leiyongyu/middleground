package com.asinking.com.openapi.controller;

import com.asinking.com.openapi.common.response.Result;
import com.asinking.com.openapi.dto.response.PurchasePlanCreateResponse;
import com.asinking.com.openapi.service.LingxingPurchasePlanService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/purchase-plan")
public class PurchasePlanController {

    private final LingxingPurchasePlanService service;

    public PurchasePlanController(LingxingPurchasePlanService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public Result<PurchasePlanCreateResponse> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return Result.ok(service.uploadAndCreate(file));
    }
}
