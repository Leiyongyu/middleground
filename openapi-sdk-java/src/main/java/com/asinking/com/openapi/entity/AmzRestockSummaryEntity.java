package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("amz_restock_summary")
public class AmzRestockSummaryEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String hashId;
    private Integer nodeType;
    private Integer sid;
    private String msku;
    private String syncTime;
    private Integer fbaSellable;
    private Integer fbaInbound;
    private Integer fbaReserved;
    @TableField("sales_7d")
    private Integer sales7d;
    @TableField("sales_14d")
    private Integer sales14d;
    @TableField("sales_30d")
    private Integer sales30d;
    @TableField("sales_60d")
    private Integer sales60d;
    @TableField("avg_sales_14d")
    private BigDecimal avgSales14d;
    @TableField("avg_sales_30d")
    private BigDecimal avgSales30d;
    @TableField("avg_sales_60d")
    private BigDecimal avgSales60d;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
