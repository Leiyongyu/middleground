package com.asinking.com.openapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BrandOwnerCreateRequest {

    @NotBlank(message = "品牌代码不能为空")
    @Size(max = 20, message = "品牌代码最长20字符")
    private String brandCode;

    @Size(max = 50, message = "负责人姓名最长50字符")
    private String ownerName;

    public String getBrandCode() {
        return brandCode;
    }

    public void setBrandCode(String brandCode) {
        this.brandCode = brandCode;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}

