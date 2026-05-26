package com.asinking.com.openapi.service.model;

public class EbayProductSyncResult {

    private int inserted;
    private int updated;
    private int total;
    private Object remoteResponse;

    public EbayProductSyncResult() {
    }

    public EbayProductSyncResult(int inserted, int updated, int total, Object remoteResponse) {
        this.inserted = inserted;
        this.updated = updated;
        this.total = total;
        this.remoteResponse = remoteResponse;
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

    public Object getRemoteResponse() {
        return remoteResponse;
    }

    public void setRemoteResponse(Object remoteResponse) {
        this.remoteResponse = remoteResponse;
    }
}
