package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("amz_warehouse_inventory_detail")
public class AmzWarehouseInventoryDetailEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer wid;
    private String sellerId;
    private String sku;
    private Integer productValidNum;
    private BigDecimal quantityReceive;
    private Integer productLockNum;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
