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
    private String productCategory;
    private Integer purchasedQty;
    private Integer domesticStock;
    private Integer pendingShip;
    private Integer fbaStock;
    private Integer fbaInbound;
    private Integer totalInventory;
    private Integer sales7d;
    private Integer sales14d;
    private Integer sales30d;
    private Integer sales60d;
    private BigDecimal salesSpeed14d;
    private BigDecimal salesSpeed30d;
    private BigDecimal salesSpeed60d;
    private Integer safetyStock;
    private Integer avgMonthlySales;
    private Integer replenishQty;
    private Integer shipQty;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
