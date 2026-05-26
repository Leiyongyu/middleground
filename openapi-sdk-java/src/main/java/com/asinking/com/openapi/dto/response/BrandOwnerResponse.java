package com.asinking.com.openapi.dto.response;

public class BrandOwnerResponse {

    private Integer id;
    private String brandCode;
    private String ownerName;

    public BrandOwnerResponse() {
    }

    public BrandOwnerResponse(Integer id, String brandCode, String ownerName) {
        this.id = id;
        this.brandCode = brandCode;
        this.ownerName = ownerName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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
