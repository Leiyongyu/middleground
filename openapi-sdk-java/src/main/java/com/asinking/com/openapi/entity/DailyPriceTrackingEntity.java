package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("daily_price_tracking")
public class DailyPriceTrackingEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("site") private String site;
    @TableField("sku_level") private String skuLevel;
    @TableField("sku") private String sku;
    @TableField("our_lowest_price") private BigDecimal ourLowestPrice;
    @TableField("tracking_price") private BigDecimal trackingPrice;
    @TableField("tracking_profit_margin") private BigDecimal trackingProfitMargin;
    @TableField("floor_price") private BigDecimal floorPrice;
    @TableField("return_rate") private BigDecimal returnRate;
    @TableField("last_3_days_sales") private Integer last3DaysSales;
    @TableField("last_7_days_sales") private Integer last7DaysSales;
    @TableField("last_30_days_sales") private Integer last30DaysSales;
    @TableField("last_90_days_sales") private Integer last90DaysSales;
    @TableField("max_monthly_sales") private Integer maxMonthlySales;
    @TableField("ebay_frontpage_url") private String ebayFrontpageUrl;
    @TableField("frontpage_sold_url") private String frontpageSoldUrl;
    @TableField("overseas_warehouse_stock") private Integer overseasWarehouseStock;
    @TableField("overseas_warehouse_age") private Integer overseasWarehouseAge;
    @TableField("stock_sales_ratio") private BigDecimal stockSalesRatio;
    @TableField("estimated_replenish") private Integer estimatedReplenish;
    @TableField("brand") private String brand;
    @TableField("operator") private String operator;
    @TableField("remark") private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
