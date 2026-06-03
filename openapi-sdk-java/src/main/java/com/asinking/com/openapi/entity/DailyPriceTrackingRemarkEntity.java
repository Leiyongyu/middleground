package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 每日跟价备注表 daily_price_tracking_remark。
 * 按 (site, sku) 唯一存储备注，与每日跟价页面的站点+SKU 维度一一对应。
 */
@Data
@TableName("daily_price_tracking_remark")
public class DailyPriceTrackingRemarkEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 站点（国家标签：美国/德国/英国） */
    @TableField("site")
    private String site;

    /** SKU 编码（baseSku） */
    @TableField("sku")
    private String sku;

    /** 备注内容 */
    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
