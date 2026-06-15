package com.asinking.com.openapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("amz_product_listing")
public class AmzProductListingEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer sid;
    private String marketplace;
    private String sellerSku;
    private String fnsku;
    private String asin;
    private String parentAsin;
    private String itemName;
    private String localSku;
    private String localName;
    private String currencyCode;
    private BigDecimal price;
    private BigDecimal landedPrice;
    private BigDecimal listingPrice;
    private BigDecimal shipping;
    private Integer fbmQuantity;
    private Integer fbaFulfillable;
    private Integer fbaUnsellable;
    private Integer reservedFcTransfers;
    private Integer reservedFcProcessing;
    private Integer reservedCustomerorders;
    private Integer inboundShipped;
    private Integer inboundWorking;
    private Integer inboundReceiving;
    private Integer reviewNum;
    private String lastStar;
    private Integer days7Sales;
    private Integer days14Sales;
    private Integer days30Sales;
    private Integer yesterdaySales;
    private BigDecimal yesterdayAmount;
    private BigDecimal days7Amount;
    private BigDecimal days14Amount;
    private BigDecimal days30Amount;
    private String fulfillmentChannel;
    private Integer status;
    private Integer isDelete;
    private String sellerBrand;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
