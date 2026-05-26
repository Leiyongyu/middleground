package com.asinking.com.openapi.dto.request;

public class SaleStatSyncRequest {

    private String startDate;
    private String endDate;
    private Integer page;
    private Integer length;

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }
}
