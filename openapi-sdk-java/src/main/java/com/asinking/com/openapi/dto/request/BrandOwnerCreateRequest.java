package com.asinking.com.openapi.dto.request;

public class BrandOwnerCreateRequest {

    private String brandCode;
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

