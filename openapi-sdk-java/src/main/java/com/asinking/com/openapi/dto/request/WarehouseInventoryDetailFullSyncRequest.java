package com.asinking.com.openapi.dto.request;

public class WarehouseInventoryDetailFullSyncRequest {

    private Integer length;
    private String sku;

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}

