package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("amz_restock_summary")
public class AmzRestockSummaryEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer sid;
    private String msku;
    private Integer fbaSellable;
    private Integer fbaInbound;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
