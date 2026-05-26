package com.asinking.com.openapi.dto.response;

public class SkuSalesSummary {

    private String sku;
    private String productName;
    private String storeName;
    private String platformName;
    private int last7Days;
    private int last30Days;
    private int last90Days;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public int getLast7Days() { return last7Days; }
    public void setLast7Days(int last7Days) { this.last7Days = last7Days; }
    public int getLast30Days() { return last30Days; }
    public void setLast30Days(int last30Days) { this.last30Days = last30Days; }
    public int getLast90Days() { return last90Days; }
    public void setLast90Days(int last90Days) { this.last90Days = last90Days; }
}
