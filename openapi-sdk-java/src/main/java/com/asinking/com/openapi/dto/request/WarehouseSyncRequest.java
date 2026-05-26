package com.asinking.com.openapi.dto.request;

public class WarehouseSyncRequest {

    private Integer offset;
    private Integer length;

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }
}

