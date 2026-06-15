-- ============================================================
-- UUID→bigint AUTO_INCREMENT 迁移脚本（保留数据）
-- 步骤: RENAME id→old_id → ADD id BIGINT AUTO PK → DROP old_id
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. ebay_product_listing
ALTER TABLE ebay_product_listing DROP PRIMARY KEY;
ALTER TABLE ebay_product_listing CHANGE id old_id VARCHAR(32);
ALTER TABLE ebay_product_listing ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE ebay_product_listing DROP COLUMN old_id;

-- 2. ebay_sales
ALTER TABLE ebay_sales DROP PRIMARY KEY;
ALTER TABLE ebay_sales CHANGE id old_id VARCHAR(32);
ALTER TABLE ebay_sales ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE ebay_sales DROP COLUMN old_id;

-- 3. ebay_shop_list
ALTER TABLE ebay_shop_list DROP PRIMARY KEY;
ALTER TABLE ebay_shop_list CHANGE id old_id VARCHAR(32);
ALTER TABLE ebay_shop_list ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE ebay_shop_list DROP COLUMN old_id;

-- 4. goodcang_grn_detail
ALTER TABLE goodcang_grn_detail DROP PRIMARY KEY;
ALTER TABLE goodcang_grn_detail CHANGE id old_id VARCHAR(32);
ALTER TABLE goodcang_grn_detail ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE goodcang_grn_detail DROP COLUMN old_id;

-- 5. goodcang_grn_list
ALTER TABLE goodcang_grn_list DROP PRIMARY KEY;
ALTER TABLE goodcang_grn_list CHANGE id old_id VARCHAR(32);
ALTER TABLE goodcang_grn_list ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE goodcang_grn_list DROP COLUMN old_id;

-- 6. goodcang_warehouse
ALTER TABLE goodcang_warehouse DROP PRIMARY KEY;
ALTER TABLE goodcang_warehouse CHANGE id old_id VARCHAR(32);
ALTER TABLE goodcang_warehouse ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE goodcang_warehouse DROP COLUMN old_id;

-- 7. lowest_price_record
ALTER TABLE lowest_price_record DROP PRIMARY KEY;
ALTER TABLE lowest_price_record CHANGE id old_id VARCHAR(36);
ALTER TABLE lowest_price_record ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE lowest_price_record DROP COLUMN old_id;

-- 8. profit_report
ALTER TABLE profit_report DROP PRIMARY KEY;
ALTER TABLE profit_report CHANGE id old_id VARCHAR(32);
ALTER TABLE profit_report ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE profit_report DROP COLUMN old_id;

-- 9. purchase_order
ALTER TABLE purchase_order DROP PRIMARY KEY;
ALTER TABLE purchase_order CHANGE id old_id VARCHAR(32);
ALTER TABLE purchase_order ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE purchase_order DROP COLUMN old_id;

-- 10. purchase_plan
ALTER TABLE purchase_plan DROP PRIMARY KEY;
ALTER TABLE purchase_plan CHANGE id old_id VARCHAR(32);
ALTER TABLE purchase_plan ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE purchase_plan DROP COLUMN old_id;

-- 11. purchase_plan_submit
ALTER TABLE purchase_plan_submit DROP PRIMARY KEY;
ALTER TABLE purchase_plan_submit CHANGE id old_id VARCHAR(32);
ALTER TABLE purchase_plan_submit ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE purchase_plan_submit DROP COLUMN old_id;

-- 12. user
ALTER TABLE user DROP PRIMARY KEY;
ALTER TABLE user CHANGE id old_id CHAR(36);
ALTER TABLE user ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE user DROP COLUMN old_id;

-- 13. warehouse
ALTER TABLE warehouse DROP PRIMARY KEY;
ALTER TABLE warehouse CHANGE id old_id VARCHAR(32);
ALTER TABLE warehouse ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE warehouse DROP COLUMN old_id;

-- 14. warehouse_inventory_detail
ALTER TABLE warehouse_inventory_detail DROP PRIMARY KEY;
ALTER TABLE warehouse_inventory_detail CHANGE id old_id VARCHAR(32);
ALTER TABLE warehouse_inventory_detail ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE warehouse_inventory_detail DROP COLUMN old_id;

-- 15. warehouse_statement
ALTER TABLE warehouse_statement DROP PRIMARY KEY;
ALTER TABLE warehouse_statement CHANGE id old_id VARCHAR(32);
ALTER TABLE warehouse_statement ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE warehouse_statement DROP COLUMN old_id;

-- FK 引用字段同步修改
ALTER TABLE brand_owner MODIFY user_id BIGINT NULL;
ALTER TABLE ebay_product_listing MODIFY store_id BIGINT NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;
