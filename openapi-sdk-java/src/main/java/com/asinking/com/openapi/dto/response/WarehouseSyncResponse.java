package com.asinking.com.openapi.dto.response;

public class WarehouseSyncResponse {

    private int inserted;
    private int updated;
    private int total;
    private Object remote;

    public WarehouseSyncResponse() {
    }

    public WarehouseSyncResponse(int inserted, int updated, int total, Object remote) {
        this.inserted = inserted;
        this.updated = updated;
        this.total = total;
        this.remote = remote;
    }

    public int getInserted() {
        return inserted;
    }

    public void setInserted(int inserted) {
        this.inserted = inserted;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public Object getRemote() {
        return remote;
    }

    public void setRemote(Object remote) {
        this.remote = remote;
    }
}

