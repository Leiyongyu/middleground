package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("amz_order_profit")
public class AmzOrderProfitEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer sid;
    private String sellerSku;
    private BigDecimal grossMargin;
    private BigDecimal spendRate;
    private BigDecimal refundAmountRate;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
