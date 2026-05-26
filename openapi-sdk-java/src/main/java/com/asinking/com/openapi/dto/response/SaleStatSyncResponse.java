package com.asinking.com.openapi.dto.response;

import java.util.List;
import java.util.Map;

public class SaleStatSyncResponse {

    private int inserted;
    private int total;
    private List<Map<String, Object>> remote;

    public SaleStatSyncResponse() {}

    public SaleStatSyncResponse(int inserted, int total, List<Map<String, Object>> remote) {
        this.inserted = inserted;
        this.total = total;
        this.remote = remote;
    }

    public int getInserted() { return inserted; }
    public void setInserted(int inserted) { this.inserted = inserted; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public List<Map<String, Object>> getRemote() { return remote; }
    public void setRemote(List<Map<String, Object>> remote) { this.remote = remote; }
}
