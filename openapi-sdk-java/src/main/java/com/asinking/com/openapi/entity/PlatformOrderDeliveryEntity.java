package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("platform_order_delivery")
public class PlatformOrderDeliveryEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("platform_order_no") private String platformOrderNo;
    @TableField("shipping_list_code") private String shippingListCode;
    @TableField("msku") private String msku;
    @TableField("sku") private String sku;
    @TableField("warehouse_id") private Integer warehouseId;
    @TableField("delivery_time") private LocalDate deliveryTime;
    @TableField("create_time") private LocalDate createTime;
    @TableField(value = "upload_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime uploadTime;
}
