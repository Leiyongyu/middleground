package com.asinking.com.openapi.service.model;

public class EbayProductFullSyncResult {

    private int inserted;
    private int updated;
    private int processed;
    private int pages;
    private int remoteTotal;

    public EbayProductFullSyncResult() {
    }

    public EbayProductFullSyncResult(int inserted, int updated, int processed, int pages, int remoteTotal) {
        this.inserted = inserted;
        this.updated = updated;
        this.processed = processed;
        this.pages = pages;
        this.remoteTotal = remoteTotal;
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

    public int getProcessed() {
        return processed;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getRemoteTotal() {
        return remoteTotal;
    }

    public void setRemoteTotal(int remoteTotal) {
        this.remoteTotal = remoteTotal;
    }
}
