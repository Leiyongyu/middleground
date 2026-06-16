package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("amz_inventory_overview")
public class AmzInventoryOverviewEntity {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Integer sid;
    private String sellerSku;
    private String warehouseSku;
    private String warehouseName;
    private String asin;
    private String principalName;
    private String store;
    @JsonProperty("rating")
    private BigDecimal lastStar;
    @JsonProperty("reviews")
    private Integer reviewNum;
    @JsonProperty("adRate")
    private BigDecimal adRate;
    @JsonProperty("profitRate30")
    private BigDecimal profitRate30d;
    @JsonProperty("refundRate90")
    private BigDecimal refundRate90d;
    @JsonProperty("category")
    private String productCategory;
    @JsonProperty("purchased")
    private Integer purchasedQty;
    private Integer domesticStock;
    @JsonProperty("lockNum")
    private Integer pendingShip;
    @JsonProperty("fbStock")
    private Integer fbaStock;
    @JsonProperty("fbaOnway")
    private Integer fbaInbound;
    @JsonProperty("totalStock")
    private Integer totalInventory;
    @JsonProperty("sales7")
    private Integer sales7d;
    @JsonProperty("sales14")
    private Integer sales14d;
    @JsonProperty("sales30")
    private Integer sales30d;
    @JsonProperty("sales60")
    private Integer sales60d;
    @JsonProperty("speed14")
    private BigDecimal salesSpeed14d;
    @JsonProperty("speed30")
    private BigDecimal salesSpeed30d;
    @JsonProperty("speed60")
    private BigDecimal salesSpeed60d;
    private BigDecimal safetyStock;
    @JsonProperty("avgMonthly")
    private BigDecimal avgMonthlySales;
    private BigDecimal replenishQty;
    @JsonProperty("restockDays")
    private BigDecimal restockDays;
    @JsonProperty("shipment")
    private BigDecimal shipQty;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
