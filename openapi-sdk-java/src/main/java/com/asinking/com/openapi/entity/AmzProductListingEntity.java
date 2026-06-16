package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("amz_product_listing")
public class AmzProductListingEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer sid;
    private String marketplace;
    private String sellerSku;
    private String asin;
    private String localSku;
    private String localName;
    private Integer reviewNum;
    private String lastStar;
    private String principalName;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
