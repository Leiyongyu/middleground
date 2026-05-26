package com.asinking.com.openapi.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 运营组数据看板 — 库存概览汇总，每个 SKU 一行。
 */
@Data
public class InventoryOverviewItem {

    /** 站点（仓库名称，多个用逗号分隔） */
    private String warehouseNames;
    /** SKU */
    private String sku;
    /** 产品名称 */
    private String productName;
    /** 近30天毛利率（百分比，如 7.95 表示 7.95%） */
    private BigDecimal last30DaysProfit;
    /** 海外在途（调拨在途） */
    private int overseasOnway;
    /** 海外可售（可用量） */
    private int overseasSellable;
    /** 海外总库存 = 海外可售 + 海外在途 */
    private int overseasTotal;
    /** 成都在途（待到货量） */
    private int localOnway;
    /** 成都可售（可用量） */
    private int localSellable;
    /** 采购计划（暂留空） */
    private String purchasePlan;
    /** 待出库（可用锁定量） */
    private int lockNum;
    /** 整个周期总库存 = 海外在途 + 海外可售 + 成都在途 + 成都可售 */
    private int totalInventory;
    /** 近7天销量 */
    private int last7DaysSales;
    /** 近30天销量 */
    private int last30DaysSales;
    /** 近3月销量 */
    private int last90DaysSales;
    /** 历史最大月销（暂留空） */
    private Integer maxMonthlySales;
    /** 海外在库库销比 = 海外可售 / 近30天销量 */
    private BigDecimal overseasInStockRatio;
    /** 海外总库销比 = 海外总库存 / 近30天销量 */
    private BigDecimal overseasTotalRatio;
    /** 总库存库销比 = 整个周期总库存 / 近30天销量 */
    private BigDecimal totalInventoryRatio;
    /** 最近成都仓出库创建时间（暂留空） */
    private String lastLocalOutboundTime;
    /** 出库天数（暂留空） */
    private Integer outboundDays;
    /** 采购周期（暂留空） */
    private Integer purchaseCycle;
    /** 采购数量 = 近3月均销量 * (采购周期 + 出库天数) */
    private BigDecimal purchaseQuantity;
    /** 最大月销预估补货量（暂留空） */
    private Integer maxMonthlyReplenish;
    /** 负责人（根据 SKU 前缀匹配 brand_owner 表） */
    private String owner;

    // ====== getters / setters ======

    public String getWarehouseNames() { return warehouseNames; }
    public void setWarehouseNames(String warehouseNames) { this.warehouseNames = warehouseNames; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public BigDecimal getLast30DaysProfit() { return last30DaysProfit; }
    public void setLast30DaysProfit(BigDecimal last30DaysProfit) { this.last30DaysProfit = last30DaysProfit; }
    public int getOverseasOnway() { return overseasOnway; }
    public void setOverseasOnway(int overseasOnway) { this.overseasOnway = overseasOnway; }
    public int getOverseasSellable() { return overseasSellable; }
    public void setOverseasSellable(int overseasSellable) { this.overseasSellable = overseasSellable; }
    public int getOverseasTotal() { return overseasTotal; }
    public void setOverseasTotal(int overseasTotal) { this.overseasTotal = overseasTotal; }
    public int getLocalOnway() { return localOnway; }
    public void setLocalOnway(int localOnway) { this.localOnway = localOnway; }
    public int getLocalSellable() { return localSellable; }
    public void setLocalSellable(int localSellable) { this.localSellable = localSellable; }
    public String getPurchasePlan() { return purchasePlan; }
    public void setPurchasePlan(String purchasePlan) { this.purchasePlan = purchasePlan; }
    public int getLockNum() { return lockNum; }
    public void setLockNum(int lockNum) { this.lockNum = lockNum; }
    public int getTotalInventory() { return totalInventory; }
    public void setTotalInventory(int totalInventory) { this.totalInventory = totalInventory; }
    public int getLast7DaysSales() { return last7DaysSales; }
    public void setLast7DaysSales(int last7DaysSales) { this.last7DaysSales = last7DaysSales; }
    public int getLast30DaysSales() { return last30DaysSales; }
    public void setLast30DaysSales(int last30DaysSales) { this.last30DaysSales = last30DaysSales; }
    public int getLast90DaysSales() { return last90DaysSales; }
    public void setLast90DaysSales(int last90DaysSales) { this.last90DaysSales = last90DaysSales; }
    public Integer getMaxMonthlySales() { return maxMonthlySales; }
    public void setMaxMonthlySales(Integer maxMonthlySales) { this.maxMonthlySales = maxMonthlySales; }
    public BigDecimal getOverseasInStockRatio() { return overseasInStockRatio; }
    public void setOverseasInStockRatio(BigDecimal overseasInStockRatio) { this.overseasInStockRatio = overseasInStockRatio; }
    public BigDecimal getOverseasTotalRatio() { return overseasTotalRatio; }
    public void setOverseasTotalRatio(BigDecimal overseasTotalRatio) { this.overseasTotalRatio = overseasTotalRatio; }
    public BigDecimal getTotalInventoryRatio() { return totalInventoryRatio; }
    public void setTotalInventoryRatio(BigDecimal totalInventoryRatio) { this.totalInventoryRatio = totalInventoryRatio; }
    public String getLastLocalOutboundTime() { return lastLocalOutboundTime; }
    public void setLastLocalOutboundTime(String lastLocalOutboundTime) { this.lastLocalOutboundTime = lastLocalOutboundTime; }
    public Integer getOutboundDays() { return outboundDays; }
    public void setOutboundDays(Integer outboundDays) { this.outboundDays = outboundDays; }
    public Integer getPurchaseCycle() { return purchaseCycle; }
    public void setPurchaseCycle(Integer purchaseCycle) { this.purchaseCycle = purchaseCycle; }
    public BigDecimal getPurchaseQuantity() { return purchaseQuantity; }
    public void setPurchaseQuantity(BigDecimal purchaseQuantity) { this.purchaseQuantity = purchaseQuantity; }
    public Integer getMaxMonthlyReplenish() { return maxMonthlyReplenish; }
    public void setMaxMonthlyReplenish(Integer maxMonthlyReplenish) { this.maxMonthlyReplenish = maxMonthlyReplenish; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
}
