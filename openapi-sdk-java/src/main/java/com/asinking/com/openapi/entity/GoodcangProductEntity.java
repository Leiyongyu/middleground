package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("goodcang_product_info")
public class GoodcangProductEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("sku_middle")
    private String middleCode;

    @TableField("real_weight")
    private BigDecimal realWeight;  // 预报重量

    @TableField("real_length")
    private BigDecimal realLength;  // 预报长

    @TableField("real_width")
    private BigDecimal realWidth;   // 预报宽

    @TableField("real_height")
    private BigDecimal realHeight;  // 预报高

    @TableField("product_name_cn")
    private String productNameCn;

    @TableField("price")
    private java.math.BigDecimal price;

    @TableField("volume")
    private java.math.BigDecimal volume;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
