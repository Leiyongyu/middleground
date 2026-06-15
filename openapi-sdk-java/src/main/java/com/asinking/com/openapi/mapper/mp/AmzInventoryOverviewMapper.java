package com.asinking.com.openapi.mapper.mp;

import com.asinking.com.openapi.entity.AmzInventoryOverviewEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmzInventoryOverviewMapper extends BaseMapper<AmzInventoryOverviewEntity> {

    @Insert("INSERT INTO amz_inventory_overview (sid, seller_sku, warehouse_sku, store, last_star, review_num, ad_rate, profit_rate30d, refund_rate90d) " +
            "SELECT pl.sid, pl.seller_sku, " +
            "  ANY_VALUE(COALESCE(pl.local_sku,'')), " +
            "  ANY_VALUE(COALESCE(sl.store_name,'')), " +
            "  ANY_VALUE(pp.avg_star), " +
            "  ANY_VALUE(pp.reviews_count), " +
            "  ANY_VALUE(op.spend_rate * 100), " +
            "  ANY_VALUE(op.gross_margin * 100), " +
            "  ANY_VALUE(op.refund_amount_rate * 100) " +
            "FROM amz_product_listing pl " +
            "LEFT JOIN shop_list sl ON sl.sid = pl.sid AND sl.platform_code = '10001' " +
            "LEFT JOIN amz_product_performance pp ON pp.sid = pl.sid AND pp.seller_sku = pl.seller_sku " +
            "LEFT JOIN amz_order_profit op ON op.sid = pl.sid AND op.seller_sku = pl.seller_sku " +
            "GROUP BY pl.sid, pl.seller_sku")
    void insertByListing();
}
