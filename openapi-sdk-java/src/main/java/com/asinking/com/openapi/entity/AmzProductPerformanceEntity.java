package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("amz_product_performance")
public class AmzProductPerformanceEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer sid;
    private String sellerSku;
    private BigDecimal avgStar;
    private Integer reviewsCount;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
