package com.asinking.com.openapi.service.impl;

import com.asinking.com.openapi.common.exception.BusinessException;
import com.asinking.com.openapi.common.response.PageResult;
import com.asinking.com.openapi.common.response.ResultCode;
import com.asinking.com.openapi.dto.request.BrandOwnerCreateRequest;
import com.asinking.com.openapi.dto.request.BrandOwnerUpdateRequest;
import com.asinking.com.openapi.dto.response.BrandOwnerResponse;
import com.asinking.com.openapi.entity.BrandOwnerEntity;
import com.asinking.com.openapi.mapper.mp.BrandOwnerMapper;
import com.asinking.com.openapi.service.BrandOwnerService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 品牌归属业务实现，含 brandCode 唯一性校验和 Entity→Response 转换。
 */
@Service
public class BrandOwnerServiceImpl extends ServiceImpl<BrandOwnerMapper, BrandOwnerEntity> implements BrandOwnerService {

    @Override
    public BrandOwnerResponse create(BrandOwnerCreateRequest req) {
        if (req == null || !StringUtils.hasText(req.getBrandCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "brandCode 不能为空");
        }
        if (!StringUtils.hasText(req.getOwnerName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "ownerName 不能为空");
        }

        String brandCode = req.getBrandCode().trim();
        String ownerName = req.getOwnerName().trim();

        boolean exists = lambdaQuery().eq(BrandOwnerEntity::getBrandCode, brandCode).exists();
        if (exists) {
            throw new BusinessException(ResultCode.CONFLICT, "brandCode 已存在");
        }

        BrandOwnerEntity entity = new BrandOwnerEntity();
        entity.setBrandCode(brandCode);
        entity.setOwnerName(ownerName);

        boolean ok = save(entity);
        if (!ok) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "创建失败");
        }
        return toResponse(entity);
    }

    @Override
    public BrandOwnerResponse update(Integer id, BrandOwnerUpdateRequest req) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "id 不能为空");
        }
        if (req == null || (req.getBrandCode() == null && req.getOwnerName() == null)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少提供 brandCode 或 ownerName");
        }

        BrandOwnerEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "记录不存在");
        }

        if (req.getBrandCode() != null) {
            if (!StringUtils.hasText(req.getBrandCode())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "brandCode 不能为空");
            }
            String newBrandCode = req.getBrandCode().trim();
            if (!newBrandCode.equals(entity.getBrandCode())) {
                boolean exists = lambdaQuery()
                        .eq(BrandOwnerEntity::getBrandCode, newBrandCode)
                        .ne(BrandOwnerEntity::getId, id)
                        .exists();
                if (exists) {
                    throw new BusinessException(ResultCode.CONFLICT, "brandCode 已存在");
                }
                entity.setBrandCode(newBrandCode);
            }
        }

        if (req.getOwnerName() != null) {
            if (!StringUtils.hasText(req.getOwnerName())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "ownerName 不能为空");
            }
            entity.setOwnerName(req.getOwnerName().trim());
        }

        boolean ok = updateById(entity);
        if (!ok) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "更新失败");
        }
        return toResponse(entity);
    }

    @Override
    public BrandOwnerResponse detail(Integer id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "id 不能为空");
        }
        BrandOwnerEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "记录不存在");
        }
        return toResponse(entity);
    }

    @Override
    public PageResult<BrandOwnerResponse> page(long page, long size, String brandCode, String ownerName) {
        long p = page <= 0 ? 1 : page;
        long s = size <= 0 ? 10 : Math.min(size, 200);
        Page<BrandOwnerEntity> mpPage = new Page<>(p, s);

        Page<BrandOwnerEntity> result = page(mpPage, lambdaQuery()
                .like(StringUtils.hasText(brandCode), BrandOwnerEntity::getBrandCode, brandCode)
                .like(StringUtils.hasText(ownerName), BrandOwnerEntity::getOwnerName, ownerName)
                .getWrapper());

        List<BrandOwnerEntity> records = result.getRecords() == null ? Collections.emptyList() : result.getRecords();
        List<BrandOwnerResponse> responses = new ArrayList<>(records.size());
        for (BrandOwnerEntity r : records) {
            responses.add(toResponse(r));
        }
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), responses);
    }

    /** 将 Entity 转换为前端展示用的 Response 对象。 */
    private BrandOwnerResponse toResponse(BrandOwnerEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BrandOwnerResponse(entity.getId(), entity.getBrandCode(), entity.getOwnerName());
    }
}
