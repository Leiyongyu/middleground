package com.asinking.com.openapi.mapper.mp;

import com.asinking.com.openapi.entity.AmzInventoryOverviewEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmzInventoryOverviewMapper extends BaseMapper<AmzInventoryOverviewEntity> {

    @Insert("INSERT INTO amz_inventory_overview (sid, seller_sku, warehouse_sku, warehouse_name, asin, principal_name, store, product_category, last_star, review_num, ad_rate, profit_rate30d, refund_rate90d, " +
            "  purchased_qty, domestic_stock, pending_ship, fba_stock, fba_inbound, total_inventory, sales7d, sales14d, sales30d, sales60d, sales_speed14d, sales_speed30d, sales_speed60d, avg_monthly_sales, safety_stock, ship_qty, replenish_qty, restock_days) " +
            "SELECT pl.sid, pl.seller_sku, " +
            "  ANY_VALUE(wd.sku), " +
            "  ANY_VALUE(wh.name), " +
            "  ANY_VALUE(pl.asin), " +
            "  ANY_VALUE(pl.principal_name), " +
            "  ANY_VALUE(COALESCE(sl.store_name,'')), " +
            "  ANY_VALUE(COALESCE(pc.category, '')), " +
            "  ANY_VALUE(NULLIF(pl.last_star, '')), " +
            "  ANY_VALUE(pl.review_num), " +
            "  ANY_VALUE(op.spend_rate * 100), " +
            "  ANY_VALUE(op.gross_margin * 100), " +
            "  ANY_VALUE(op.refund_amount_rate * 100), " +
            "  COALESCE(MAX(wd.product_valid_num), 0), " +
            "  COALESCE(MAX(wd.quantity_receive), 0), " +
            "  COALESCE(MAX(wd.product_lock_num), 0), " +
            "  COALESCE(MAX(rs.fba_sellable), 0), " +
            "  COALESCE(MAX(rs.fba_inbound), 0), " +
            "  COALESCE(MAX(rs.fba_sellable), 0) + COALESCE(MAX(rs.fba_inbound), 0), " +
            "  COALESCE(MAX(rs.sales_7d), 0), " +
            "  COALESCE(MAX(rs.sales_14d), 0), " +
            "  COALESCE(MAX(rs.sales_30d), 0), " +
            "  COALESCE(MAX(rs.sales_60d), 0), " +
            "  MAX(rs.avg_sales_14d), " +
            "  MAX(rs.avg_sales_30d), " +
            "  MAX(rs.avg_sales_60d), " +
            "  ROUND((COALESCE(MAX(rs.avg_sales_14d),0)*0.5 + COALESCE(MAX(rs.avg_sales_30d),0)*0.4 + COALESCE(MAX(rs.avg_sales_60d),0)*0.1) * 30, 2), " +
            "  ROUND((COALESCE(MAX(rs.avg_sales_14d),0)*0.5 + COALESCE(MAX(rs.avg_sales_30d),0)*0.4 + COALESCE(MAX(rs.avg_sales_60d),0)*0.1) * 90, 2), " +
            "  ROUND((COALESCE(MAX(rs.avg_sales_14d),0)*0.5 + COALESCE(MAX(rs.avg_sales_30d),0)*0.4 + COALESCE(MAX(rs.avg_sales_60d),0)*0.1) * 90, 2) - (COALESCE(MAX(rs.fba_sellable),0) + COALESCE(MAX(rs.fba_inbound),0)), " +
            "  ROUND((COALESCE(MAX(rs.avg_sales_14d),0)*0.5 + COALESCE(MAX(rs.avg_sales_30d),0)*0.4 + COALESCE(MAX(rs.avg_sales_60d),0)*0.1) * 120, 2) - COALESCE(MAX(wd.product_valid_num),0) - COALESCE(MAX(wd.quantity_receive),0) - (COALESCE(MAX(rs.fba_sellable),0) + COALESCE(MAX(rs.fba_inbound),0)) - COALESCE(MAX(wd.product_lock_num),0), " +
            "  ROUND(((COALESCE(MAX(rs.fba_sellable),0) + COALESCE(MAX(rs.fba_inbound),0)) - ((COALESCE(MAX(rs.avg_sales_14d),0)*0.5 + COALESCE(MAX(rs.avg_sales_30d),0)*0.4 + COALESCE(MAX(rs.avg_sales_60d),0)*0.1) * 120 - COALESCE(MAX(wd.product_valid_num),0) - COALESCE(MAX(wd.quantity_receive),0) - (COALESCE(MAX(rs.fba_sellable),0) + COALESCE(MAX(rs.fba_inbound),0)) - COALESCE(MAX(wd.product_lock_num),0))) / NULLIF((COALESCE(MAX(rs.avg_sales_14d),0)*0.5 + COALESCE(MAX(rs.avg_sales_30d),0)*0.4 + COALESCE(MAX(rs.avg_sales_60d),0)*0.1), 0), 2) " +
            "FROM amz_warehouse_inventory_detail wd " +
            "LEFT JOIN amz_product_listing pl ON pl.local_sku = wd.sku " +
            "LEFT JOIN shop_list sl ON sl.sid = pl.sid AND sl.platform_code = '10001' " +
            "LEFT JOIN amz_product_category pc ON pc.sid = pl.sid AND pc.seller_sku = pl.seller_sku " +
            "LEFT JOIN amz_order_profit op ON op.sid = pl.sid AND op.seller_sku = pl.seller_sku " +
            "LEFT JOIN amz_restock_summary rs ON rs.sid = pl.sid AND rs.msku = pl.seller_sku " +
            "LEFT JOIN warehouse wh ON wh.wid = wd.wid " +
            "GROUP BY wd.id, pl.sid, pl.seller_sku")
    void insertByListing();
}
