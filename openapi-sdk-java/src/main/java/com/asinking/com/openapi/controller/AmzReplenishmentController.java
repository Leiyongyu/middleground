package com.asinking.com.openapi.controller;

import com.asinking.com.openapi.common.response.PageResult;
import com.asinking.com.openapi.common.response.Result;
import com.asinking.com.openapi.entity.AmzInventoryOverviewEntity;
import com.asinking.com.openapi.mapper.mp.AmzInventoryOverviewMapper;
import com.asinking.com.openapi.service.AmazonComputeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/amz/inventory")
public class AmzReplenishmentController {

    private final AmzInventoryOverviewMapper mapper;
    private final AmazonComputeService computeService;

    public AmzReplenishmentController(AmzInventoryOverviewMapper mapper,
                                       AmazonComputeService computeService) {
        this.mapper = mapper;
        this.computeService = computeService;
    }

    @GetMapping
    public Result<PageResult<AmzInventoryOverviewEntity>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "100") long size) {
        long total = mapper.selectCount(null);
        long from = (page - 1) * size;
        List<AmzInventoryOverviewEntity> records = mapper.selectList(
                new LambdaQueryWrapper<AmzInventoryOverviewEntity>()
                        .last("LIMIT " + from + "," + size));
        return Result.ok(new PageResult<>(total, page, size, records));
    }

    @PostMapping("/refresh")
    public Result<String> refresh() {
        computeService.refreshSnapshot();
        return Result.ok("ok");
    }
}
