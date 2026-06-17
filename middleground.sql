/*
 Navicat Premium Dump SQL

 Source Server         : root
 Source Server Type    : MySQL
 Source Server Version : 90700 (9.7.0)
 Source Host           : localhost:3306
 Source Schema         : middleground

 Target Server Type    : MySQL
 Target Server Version : 90700 (9.7.0)
 File Encoding         : 65001

 Date: 17/06/2026 17:29:07
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for amz_inventory_overview
-- ----------------------------
DROP TABLE IF EXISTS `amz_inventory_overview`;
CREATE TABLE `amz_inventory_overview`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sid` int NULL DEFAULT NULL,
  `seller_sku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `warehouse_sku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `warehouse_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '仓库名称',
  `asin` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'ASIN',
  `principal_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '负责人',
  `store` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '店铺',
  `last_star` decimal(3, 1) NULL DEFAULT NULL COMMENT '评分',
  `review_num` int NULL DEFAULT 0 COMMENT '评论数',
  `ad_rate` decimal(10, 4) NULL DEFAULT NULL COMMENT '广告费率',
  `profit_rate30d` decimal(10, 4) NULL DEFAULT NULL,
  `refund_rate90d` decimal(10, 4) NULL DEFAULT NULL,
  `product_category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '产品分类',
  `purchased_qty` int NULL DEFAULT 0 COMMENT '已采购数量',
  `domestic_stock` int NULL DEFAULT 0 COMMENT '国内仓数量',
  `pending_ship` int NULL DEFAULT 0 COMMENT '待出库',
  `fba_stock` int NULL DEFAULT 0 COMMENT 'FBA在库',
  `fba_inbound` int NULL DEFAULT 0 COMMENT 'FBA在途',
  `total_inventory` int NULL DEFAULT 0 COMMENT '总库存',
  `sales7d` int NULL DEFAULT 0,
  `sales14d` int NULL DEFAULT 0,
  `sales30d` int NULL DEFAULT 0,
  `sales60d` int NULL DEFAULT 0,
  `sales_speed14d` decimal(10, 2) NULL DEFAULT NULL,
  `sales_speed30d` decimal(10, 2) NULL DEFAULT NULL,
  `sales_speed60d` decimal(10, 2) NULL DEFAULT NULL,
  `safety_stock` decimal(10, 2) NULL DEFAULT NULL COMMENT '安全库存',
  `avg_monthly_sales` decimal(10, 2) NULL DEFAULT NULL COMMENT '平均月销量',
  `replenish_qty` decimal(10, 2) NULL DEFAULT NULL COMMENT '补货量',
  `restock_days` decimal(10, 2) NULL DEFAULT NULL COMMENT '补货时间(天)',
  `ship_qty` decimal(10, 2) NULL DEFAULT NULL COMMENT '发货量',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1672657 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Amazon补货数据预计算表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for amz_order_profit
-- ----------------------------
DROP TABLE IF EXISTS `amz_order_profit`;
CREATE TABLE `amz_order_profit`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sid` int NOT NULL COMMENT '店铺ID',
  `seller_sku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Seller SKU',
  `gross_margin` decimal(10, 4) NULL DEFAULT NULL COMMENT '毛利率',
  `spend_rate` decimal(10, 4) NULL DEFAULT NULL COMMENT '广告费率',
  `refund_amount_rate` decimal(10, 4) NULL DEFAULT NULL COMMENT '退款率',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sid_sku`(`sid` ASC, `seller_sku` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 91844 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Amazon订单利润表(MSKU维度)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for amz_product_category
-- ----------------------------
DROP TABLE IF EXISTS `amz_product_category`;
CREATE TABLE `amz_product_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sid` int NOT NULL COMMENT '店铺ID',
  `seller_sku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MSKU',
  `category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '产品分类',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sid_sku`(`sid` ASC, `seller_sku` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AMZ产品分类(手动维护)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for amz_product_listing
-- ----------------------------
DROP TABLE IF EXISTS `amz_product_listing`;
CREATE TABLE `amz_product_listing`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sid` int NOT NULL COMMENT '店铺sid',
  `marketplace` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '国家',
  `seller_sku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MSKU',
  `asin` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'ASIN',
  `local_sku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '本地SKU',
  `local_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '品名',
  `review_num` int NULL DEFAULT 0 COMMENT '评论数',
  `last_star` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '星级评分',
  `principal_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'Listing负责人',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sid_sku`(`sid` ASC, `seller_sku` ASC) USING BTREE,
  INDEX `idx_asin`(`asin` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 274205 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Amazon商品Listing' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for amz_restock_summary
-- ----------------------------
DROP TABLE IF EXISTS `amz_restock_summary`;
CREATE TABLE `amz_restock_summary`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `hash_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '领星唯一标识',
  `node_type` int NULL DEFAULT NULL COMMENT '节点类型:1共享库存父行,2共享库存子行,3非共享库存,4汇总行',
  `sid` int NOT NULL COMMENT '店铺ID',
  `msku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MSKU',
  `sync_time` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '数据更新时间',
  `fba_sellable` int NULL DEFAULT 0 COMMENT 'FBA可售',
  `fba_inbound` int NULL DEFAULT 0 COMMENT 'FBA在途',
  `fba_reserved` int NULL DEFAULT 0 COMMENT 'FBA预留',
  `sales_7d` int NULL DEFAULT 0 COMMENT '近7天销量',
  `sales_14d` int NULL DEFAULT 0 COMMENT '近14天销量',
  `sales_30d` int NULL DEFAULT 0 COMMENT '近30天销量',
  `sales_60d` int NULL DEFAULT 0 COMMENT '近60天销量',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `avg_sales_14d` decimal(10, 2) NULL DEFAULT NULL COMMENT '14日均销量',
  `avg_sales_30d` decimal(10, 2) NULL DEFAULT NULL COMMENT '30日均销量',
  `avg_sales_60d` decimal(10, 2) NULL DEFAULT NULL COMMENT '60日均销量',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_hash_id`(`hash_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 108873 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'amz补货建议表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for amz_warehouse_inventory_detail
-- ----------------------------
DROP TABLE IF EXISTS `amz_warehouse_inventory_detail`;
CREATE TABLE `amz_warehouse_inventory_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wid` int NOT NULL COMMENT '仓库ID',
  `seller_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '卖家ID',
  `sku` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SKU',
  `quantity_receive` decimal(10, 2) NULL DEFAULT NULL COMMENT '待收数量',
  `product_valid_num` int NULL DEFAULT 0 COMMENT '可用库存',
  `product_lock_num` int NULL DEFAULT 0 COMMENT '锁定库存',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wid_sku`(`wid` ASC, `sku` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17434 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AMZ仓库库存明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for brand_owner
-- ----------------------------
DROP TABLE IF EXISTS `brand_owner`;
CREATE TABLE `brand_owner`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `brand_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '品牌代码',
  `owner_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '负责人姓名',
  `user_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_brand_code`(`brand_code` ASC) USING BTREE,
  INDEX `idx_owner_name`(`owner_name` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '品牌负责人表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ebay_link_template
-- ----------------------------
DROP TABLE IF EXISTS `ebay_link_template`;
CREATE TABLE `ebay_link_template`  (
  `site` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点',
  `presale_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '售前链接模板',
  `sold_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '售后链接模板',
  `profit_rate` int NULL DEFAULT NULL COMMENT '目标利润率(%)',
  `exchange_rate` decimal(10, 2) NULL DEFAULT NULL COMMENT '实时汇率',
  PRIMARY KEY (`site`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ebay_product_dedup
-- ----------------------------
DROP TABLE IF EXISTS `ebay_product_dedup`;
CREATE TABLE `ebay_product_dedup`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `site` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点',
  `sku` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SKU (baseSku)',
  `oe_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'OE号',
  `product_name` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '产品名称',
  `tracking_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '跟卖价格',
  `tracking_profit_margin` decimal(10, 6) NULL DEFAULT NULL COMMENT '跟卖利润率',
  `floor_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '底线价',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `profit_rate` decimal(10, 4) NULL DEFAULT NULL COMMENT '近30天利润率',
  `return_rate` decimal(10, 4) NULL DEFAULT NULL COMMENT '退货率',
  `lowest_price` decimal(10, 2) NULL DEFAULT NULL,
  `lowest_item_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `lowest_upload_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_site_sku`(`site` ASC, `sku` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7480 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ebay商品去重表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ebay_product_listing
-- ----------------------------
DROP TABLE IF EXISTS `ebay_product_listing`;
CREATE TABLE `ebay_product_listing`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `platform` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'eBay' COMMENT '平台名称，默认eBay，可根据需要修改',
  `item_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'eBay商品ID，唯一标识',
  `item_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品链接地址',
  `picture_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品图片URL',
  `msku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '制造商SKU编码',
  `sku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '基础SKU(前两段)',
  `local_sku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '本地SKU编码',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品标题',
  `local_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品本地名称（中文名称）',
  `attribute` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '商品属性，JSON格式存储',
  `listing_type` tinyint NOT NULL COMMENT '刊登类型：2-固价',
  `listing_type_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '刊登类型名称',
  `listing_status` tinyint NOT NULL COMMENT '刊登状态：2-已下架',
  `listing_status_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '刊登状态名称',
  `price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '商品价格',
  `start_price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '起始价格',
  `accept_price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '接受价格',
  `quantity` int NOT NULL DEFAULT 0 COMMENT '商品库存数量',
  `auto_restock` tinyint NULL DEFAULT NULL COMMENT '自动补货设置，0-关闭，1-开启',
  `product_auto_restock_response` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '自动补货响应配置，JSON格式',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品所在地',
  `dispatch_time_max` tinyint NULL DEFAULT NULL COMMENT '最长发货时间（天）',
  `listing_start_time` datetime NOT NULL COMMENT '刊登开始时间',
  `listing_end_time` datetime NOT NULL COMMENT '刊登结束时间',
  `store_id` bigint NOT NULL DEFAULT 0,
  `store_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '店铺名称',
  `site_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点代码',
  `site_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点名称',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_item_id`(`item_id` ASC) USING BTREE,
  INDEX `idx_store_id`(`store_id` ASC) USING BTREE,
  INDEX `idx_listing_status`(`listing_status` ASC) USING BTREE,
  INDEX `idx_listing_start_time`(`listing_start_time` ASC) USING BTREE,
  INDEX `idx_listing_end_time`(`listing_end_time` ASC) USING BTREE,
  INDEX `idx_platform`(`platform` ASC) USING BTREE,
  INDEX `idx_quantity`(`quantity` ASC) USING BTREE,
  INDEX `idx_msku_local_sku`(`msku` ASC, `local_sku` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 42725 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'eBay商品刊登信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ebay_sales
-- ----------------------------
DROP TABLE IF EXISTS `ebay_sales`;
CREATE TABLE `ebay_sales`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `platform_order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台订单号',
  `currency` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '币种',
  `sku` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '库存SKU',
  `quantity` int NULL DEFAULT 0 COMMENT '购买数量',
  `payment_time` datetime NULL DEFAULT NULL COMMENT '付款时间',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_sku`(`platform_order_no` ASC, `sku` ASC) USING BTREE,
  INDEX `idx_sku`(`sku` ASC) USING BTREE,
  INDEX `idx_payment_time`(`payment_time` ASC) USING BTREE,
  INDEX `idx_payment_currency`(`payment_time` ASC, `currency` ASC) USING BTREE,
  INDEX `idx_sku_currency_payment`(`sku` ASC, `currency` ASC, `payment_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19014 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'eBay销量表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for goodcang_grn_detail
-- ----------------------------
DROP TABLE IF EXISTS `goodcang_grn_detail`;
CREATE TABLE `goodcang_grn_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receiving_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '入库单号',
  `product_sku` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '商品SKU',
  `box_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '箱号',
  `transit_pre_count` int NULL DEFAULT 0 COMMENT '中转预报数量',
  `transit_receiving_count` int NULL DEFAULT 0 COMMENT '中转收货数量',
  `reference_box_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '第三方箱唛号',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_receiving_code`(`receiving_code` ASC) USING BTREE,
  INDEX `idx_product_sku`(`product_sku` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 62857 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '谷仓入库单详情(中转明细)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for goodcang_grn_list
-- ----------------------------
DROP TABLE IF EXISTS `goodcang_grn_list`;
CREATE TABLE `goodcang_grn_list`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receiving_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '入库单号',
  `warehouse_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '海外仓仓库编码',
  `transit_warehouse_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '中转仓仓库编码',
  `reference_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '客户参考号',
  `receiving_status` int NULL DEFAULT 0 COMMENT '入库单状态',
  `transit_type` int NULL DEFAULT 0 COMMENT '入库单类型',
  `create_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_at` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_receiving_code`(`receiving_code` ASC) USING BTREE,
  INDEX `idx_warehouse`(`warehouse_code` ASC) USING BTREE,
  INDEX `idx_create_at`(`create_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 109 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '谷仓入库单列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for goodcang_product_info
-- ----------------------------
DROP TABLE IF EXISTS `goodcang_product_info`;
CREATE TABLE `goodcang_product_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku_middle` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '中间码',
  `product_name_cn` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '中文名称',
  `real_weight` decimal(10, 3) NULL DEFAULT NULL COMMENT '实收重量(KG)',
  `real_length` decimal(10, 2) NULL DEFAULT NULL COMMENT '实收长(CM)',
  `real_width` decimal(10, 2) NULL DEFAULT NULL COMMENT '实收宽(CM)',
  `real_height` decimal(10, 2) NULL DEFAULT NULL COMMENT '实收高(CM)',
  `volume` decimal(10, 2) NULL DEFAULT NULL COMMENT '体积(长*宽*高/6000)',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '单价',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `middle_code`(`sku_middle` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2249 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for goodcang_warehouse
-- ----------------------------
DROP TABLE IF EXISTS `goodcang_warehouse`;
CREATE TABLE `goodcang_warehouse`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '区域仓代码',
  `wid` int NULL DEFAULT 0 COMMENT '关联warehouse.wid',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '仓库名称',
  `country_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '国家代码',
  `wp_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '物理仓编码',
  `wp_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '物理仓名称',
  `state` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '州/省份',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '城市',
  `postcode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '邮编',
  `contacter` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '联系人',
  `phone` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '电话',
  `street_address1` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '地址1',
  `street_address2` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '地址2',
  `street_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '门牌号',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wp_code`(`wp_code` ASC) USING BTREE,
  INDEX `idx_warehouse_code`(`warehouse_code` ASC) USING BTREE,
  INDEX `idx_country_code`(`country_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '谷仓仓库信息(物理仓级别)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for inventory_overview
-- ----------------------------
DROP TABLE IF EXISTS `inventory_overview`;
CREATE TABLE `inventory_overview`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_names` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点',
  `sku` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SKU',
  `product_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '产品名称',
  `sku_level` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'E' COMMENT 'SKU等级',
  `last30_days_profit` decimal(10, 2) NULL DEFAULT NULL COMMENT '近30利润',
  `return_rate` decimal(10, 4) NULL DEFAULT NULL COMMENT '退货率',
  `overseas_onway` int NULL DEFAULT 0 COMMENT '海外在途',
  `overseas_sellable` int NULL DEFAULT 0 COMMENT '海外可售',
  `overseas_total` int NULL DEFAULT 0 COMMENT '海外总库存',
  `purchase_pending_delivery` int NULL DEFAULT 0 COMMENT '采购待交付',
  `local_sellable` int NULL DEFAULT 0 COMMENT '成都可售',
  `local_onway` int NULL DEFAULT 0 COMMENT '成都在途',
  `purchase_plan` int NULL DEFAULT 0 COMMENT '采购计划',
  `lock_num` int NULL DEFAULT 0 COMMENT '待出库',
  `total_inventory` int NULL DEFAULT 0 COMMENT '总库存',
  `last7_days_sales` int NULL DEFAULT 0 COMMENT '近7天销量',
  `last30_days_sales` int NULL DEFAULT 0 COMMENT '近30天销量',
  `last90_days_sales` int NULL DEFAULT 0 COMMENT '近3月销量',
  `max_monthly_sales` int NULL DEFAULT NULL COMMENT '历史最大月销',
  `overseas_in_stock_ratio` decimal(10, 1) NULL DEFAULT NULL COMMENT '海外在库库销比',
  `overseas_total_ratio` decimal(10, 1) NULL DEFAULT NULL COMMENT '海外总库销比',
  `total_inventory_ratio` decimal(10, 1) NULL DEFAULT NULL COMMENT '总库存库销比',
  `last_local_outbound_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '最近成都出库',
  `outbound_days` int NULL DEFAULT NULL COMMENT '出库天数',
  `purchase_cycle` int NULL DEFAULT NULL COMMENT '采购周期',
  `purchase_quantity` decimal(10, 0) NULL DEFAULT NULL COMMENT '采购数量',
  `max_monthly_replenish` int NULL DEFAULT NULL COMMENT '最大月销补货量',
  `owner` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '负责人',
  `brand` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `operator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `oe_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `last3_days_sales` int NULL DEFAULT 0,
  `overseas_warehouse_stock` int NULL DEFAULT 0,
  `overseas_warehouse_age` int NULL DEFAULT NULL,
  `stock_sales_ratio` decimal(10, 2) NULL DEFAULT NULL,
  `estimated_replenish` int NULL DEFAULT NULL,
  `our_lowest_price` decimal(10, 2) NULL DEFAULT NULL,
  `ebay_frontpage_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `frontpage_sold_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_site_sku`(`warehouse_names` ASC, `sku` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46801 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '库存总览预计算表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `api_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '接口路径',
  `http_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'HTTP方法',
  `operator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人账号',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `operation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作类型: 拉取/上传/同步',
  `target` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标表或接口',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '成功/失败',
  `total_count` int NULL DEFAULT NULL COMMENT '总条数',
  `success_count` int NULL DEFAULT NULL COMMENT '成功条数',
  `fail_count` int NULL DEFAULT NULL COMMENT '失败条数',
  `error_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '详细日志JSON',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_api_path`(`api_path` ASC) USING BTREE,
  INDEX `idx_operator`(`operator` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 119 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '拉取数据日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for profit_report
-- ----------------------------
DROP TABLE IF EXISTS `profit_report`;
CREATE TABLE `profit_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '上传文件名',
  `msku` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品MSKU',
  `platform` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '平台名称',
  `store_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '店铺名称',
  `country_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '国家代码',
  `currency_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '币种',
  `volume` int NULL DEFAULT 0 COMMENT '销量',
  `sales_amount` decimal(16, 4) NULL DEFAULT 0.0000 COMMENT '销售额',
  `gross_profit` decimal(16, 4) NULL DEFAULT 0.0000 COMMENT '毛利',
  `gross_margin` decimal(10, 4) NULL DEFAULT NULL COMMENT '毛利率',
  `purchase_cost` decimal(16, 4) NULL DEFAULT 0.0000 COMMENT '采购成本',
  `logistics_cost` decimal(16, 4) NULL DEFAULT 0.0000 COMMENT '物流成本',
  `ship_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '发货时间',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_msku_ship_store_country`(`msku` ASC, `ship_time` ASC, `store_name` ASC, `country_code` ASC) USING BTREE,
  INDEX `idx_msku`(`msku` ASC) USING BTREE,
  INDEX `idx_store_name`(`store_name` ASC) USING BTREE,
  INDEX `idx_ship_time`(`ship_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '利润报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for purchase_order
-- ----------------------------
DROP TABLE IF EXISTS `purchase_order`;
CREATE TABLE `purchase_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_sn` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '采购单号',
  `custom_order_sn` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '自定义单号',
  `supplier_id` int NULL DEFAULT 0 COMMENT '供应商id',
  `supplier_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '供应商',
  `opt_uid` int NULL DEFAULT 0 COMMENT '采购员id',
  `opt_realname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '操作人姓名',
  `auditor_realname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '审核人姓名',
  `last_realname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '最后操作人姓名',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `order_time` datetime NULL DEFAULT NULL COMMENT '下单时间',
  `update_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '采购单更新时间',
  `status` int NULL DEFAULT 0 COMMENT '采购单状态',
  `status_text` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '状态说明',
  `wid` int NULL DEFAULT 0 COMMENT '仓库id',
  `ware_house_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '仓库名',
  `item_sku` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '子项SKU',
  `item_product_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '子项品名',
  `item_product_id` int NULL DEFAULT 0 COMMENT '子项产品id',
  `item_quantity_real` int NULL DEFAULT 0 COMMENT '子项实际采购量',
  `item_quantity_entry` int NULL DEFAULT 0 COMMENT '子项到货入库量',
  `item_quantity_receive` int NULL DEFAULT 0 COMMENT '待到货量',
  `item_price` decimal(16, 4) NULL DEFAULT 0.0000 COMMENT '子项含税单价',
  `item_amount` decimal(16, 4) NULL DEFAULT 0.0000 COMMENT '子项价税合计',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_create`(`order_sn` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_item_sku`(`item_sku` ASC) USING BTREE,
  INDEX `idx_create_time_status`(`create_time` ASC, `status_text` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4817 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '采购单(全字段)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for purchase_plan
-- ----------------------------
DROP TABLE IF EXISTS `purchase_plan`;
CREATE TABLE `purchase_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_sn` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '采购计划编号',
  `ppg_sn` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '采购计划批次号',
  `product_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '品名',
  `sku` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'SKU',
  `fnsku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'FNSKU',
  `pic_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '产品图片',
  `supplier_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '供应商id',
  `supplier_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '供应商名称',
  `status_text` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '状态说明',
  `status` int NULL DEFAULT 0 COMMENT '状态值: 2待采购 -2已完成 121待审批 122已驳回 -3/124已作废',
  `sid` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '店铺id',
  `seller_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '店铺名称',
  `marketplace` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '国家',
  `expect_arrive_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '期望到货时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '产品备注',
  `quantity_plan` int NULL DEFAULT 0 COMMENT '计划采购量',
  `product_id` int NULL DEFAULT 0 COMMENT '商品id',
  `cg_uid` int NULL DEFAULT 0 COMMENT '采购员id',
  `cg_opt_username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '采购员名称',
  `cg_box_pcs` int NULL DEFAULT 0 COMMENT '单箱数量',
  `is_combo` int NULL DEFAULT 0 COMMENT '组合商品: 0否 1是',
  `is_aux` int NULL DEFAULT 0 COMMENT '辅料: 0否 1是',
  `is_related_process_plan` int NULL DEFAULT 0 COMMENT '关联加工计划: 0否 1是',
  `spu` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'SPU',
  `spu_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '款名',
  `creator_uid` int NULL DEFAULT 0 COMMENT '创建人id',
  `creator_real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建人名称',
  `wid` int NULL DEFAULT 0 COMMENT '仓库id',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '仓库名称',
  `purchaser_id` int NULL DEFAULT 0 COMMENT '采购方id',
  `purchaser_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '采购方名称',
  `create_time` datetime NULL DEFAULT NULL,
  `plan_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '计划备注',
  `attribute_json` json NULL COMMENT '属性',
  `file_json` json NULL COMMENT '附件',
  `msku_json` json NULL COMMENT 'MSKU',
  `perm_uid_json` json NULL COMMENT '单据负责人uid',
  `perm_username_json` json NULL COMMENT '单据负责人名称',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_plan_sku`(`plan_sn` ASC, `sku` ASC) USING BTREE,
  INDEX `idx_ppg_sn`(`ppg_sn` ASC) USING BTREE,
  INDEX `idx_sku`(`sku` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_status_upload`(`status_text` ASC, `upload_time` ASC) USING BTREE,
  INDEX `idx_upload_time`(`upload_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2235 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '采购计划(全字段)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for purchase_plan_submit
-- ----------------------------
DROP TABLE IF EXISTS `purchase_plan_submit`;
CREATE TABLE `purchase_plan_submit`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SKU',
  `wid` int NOT NULL COMMENT '仓库ID',
  `warehouse_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '仓库名称',
  `quantity_plan` int NOT NULL COMMENT '计划采购量',
  `quantity_replenish` int NULL DEFAULT 0 COMMENT '预估补货量',
  `quantity_purchase` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '采购数量',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注（等级|销量|利润）',
  `expect_arrive_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '期望到货时间',
  `plan_sn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '领星返回的采购计划单号',
  `ppg_sn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '领星返回的批次号',
  `status_text` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '已提交' COMMENT '状态',
  `creator_owner_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建人（负责人名称）',
  `creator_account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建人账号',
  `creator_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '创建人角色',
  `approver` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批人账号',
  `approve_time` datetime NULL DEFAULT NULL COMMENT '审批时间',
  `submit_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account_sku_wid`(`creator_account` ASC, `sku` ASC, `wid` ASC) USING BTREE,
  INDEX `idx_owner`(`creator_owner_name` ASC) USING BTREE,
  INDEX `idx_account`(`creator_account` ASC) USING BTREE,
  INDEX `idx_submit_time`(`submit_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '采购计划提交记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for shop_list
-- ----------------------------
DROP TABLE IF EXISTS `shop_list`;
CREATE TABLE `shop_list`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '店铺ID，eBay店铺唯一标识',
  `sid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'SID标识，默认为空字符串',
  `store_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '店铺名称',
  `platform_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台代码，10003代表eBay平台',
  `platform_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台名称',
  `currency` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '店铺使用的货币单位，如HKD港币',
  `is_sync` tinyint NOT NULL DEFAULT 0 COMMENT '是否同步数据，1表示同步，0表示未同步',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '店铺状态，1表示启用，0表示禁用',
  `country_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '国家/地区代码，如HK香港',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_store_id`(`store_id` ASC) USING BTREE,
  INDEX `idx_platform_code`(`platform_code` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_country_code`(`country_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 170 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'eBay店铺列表信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for team
-- ----------------------------
DROP TABLE IF EXISTS `team`;
CREATE TABLE `team`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `leader` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '组长（user表的owner_name）',
  `member` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '组员（user表的owner_name）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_leader_member`(`leader` ASC, `member` ASC) USING BTREE,
  INDEX `idx_leader`(`leader` ASC) USING BTREE,
  INDEX `idx_member`(`member` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号，唯一',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（加密后存储，如BCrypt、MD5等）',
  `role` tinyint NOT NULL DEFAULT 2 COMMENT '1=管理员,2=用户',
  `owner_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '负责人姓名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `account`(`account` ASC) USING BTREE,
  INDEX `idx_account`(`account` ASC) USING BTREE,
  INDEX `idx_role`(`role` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_column_config
-- ----------------------------
DROP TABLE IF EXISTS `user_column_config`;
CREATE TABLE `user_column_config`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号',
  `page_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'dashboard' COMMENT '页面标识',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '列配置JSON（key数组）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_page`(`user_account` ASC, `page_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户列配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for warehouse
-- ----------------------------
DROP TABLE IF EXISTS `warehouse`;
CREATE TABLE `warehouse`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wid` int NOT NULL COMMENT '领星仓库ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '仓库名称',
  `type` int NULL DEFAULT 0 COMMENT '仓库类型',
  `sub_type` int NULL DEFAULT 0 COMMENT '仓库子类型',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标记',
  `country_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '国家代码',
  `wp_id` int NULL DEFAULT 0 COMMENT '仓储服务商ID',
  `wp_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '仓储服务商名称',
  `t_warehouse_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '第三方仓库名称',
  `t_warehouse_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '第三方仓库编码',
  `t_country_area_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '第三方国家地区名称',
  `t_status` int NULL DEFAULT 0 COMMENT '第三方状态',
  `raw_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '原始JSON数据',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wid`(`wid` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2066377098244980756 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '海外仓仓库表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for warehouse_inventory_detail
-- ----------------------------
DROP TABLE IF EXISTS `warehouse_inventory_detail`;
CREATE TABLE `warehouse_inventory_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '仓库id',
  `product_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '本地产品id',
  `sku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'SKU',
  `seller_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '店铺id',
  `fnsku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'FNSKU',
  `product_total` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实际库存总量(可用量+次品量+待检待上架量+锁定量)',
  `product_valid_num` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '可用量',
  `product_bad_num` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '次品量',
  `product_qc_num` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '待检待上架量',
  `product_lock_num` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '锁定量',
  `good_lock_num` int NULL DEFAULT 0 COMMENT '良品锁定数',
  `bad_lock_num` int NULL DEFAULT 0 COMMENT '不良品锁定数',
  `stock_cost_total` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '库存成本',
  `quantity_receive` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '待到货量',
  `stock_cost` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '单位库存成本',
  `product_onway` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '调拨在途',
  `transit_head_cost` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '调拨在途头程成本',
  `average_age` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '平均库龄',
  `expect_valid_num` int NULL DEFAULT 0 COMMENT '海外仓预期有效数',
  `expect_pending_num` decimal(16, 4) NULL DEFAULT 0.0000 COMMENT '海外仓预期待处理数',
  `available_inventory_box_qty` int NULL DEFAULT 0 COMMENT '海外仓可用箱库存',
  `purchase_price` decimal(16, 4) NULL DEFAULT 0.0000 COMMENT '采购单价',
  `price` decimal(16, 4) NULL DEFAULT 0.0000 COMMENT '单位费用',
  `head_stock_price` decimal(16, 4) NULL DEFAULT 0.0000 COMMENT '单位头程',
  `stock_price` decimal(16, 4) NULL DEFAULT 0.0000 COMMENT '单位库存成本',
  `third_inventory_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '海外仓第三方库存信息(JSON)',
  `stock_age_list_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '库龄信息(JSON)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wid_product`(`wid` ASC, `product_id` ASC) USING BTREE,
  INDEX `idx_sku`(`sku` ASC) USING BTREE,
  INDEX `idx_wid`(`wid` ASC) USING BTREE,
  INDEX `idx_wid_sku`(`wid` ASC, `sku` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2067063737095041084 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '仓库库存明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for warehouse_statement
-- ----------------------------
DROP TABLE IF EXISTS `warehouse_statement`;
CREATE TABLE `warehouse_statement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `statement_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '流水ID',
  `wid` int NULL DEFAULT 0 COMMENT '仓库id',
  `ware_house_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '仓库名称',
  `order_sn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '操作单据号',
  `ref_order_sn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '关联单据号',
  `sku` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'SKU',
  `seller_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '店铺id',
  `fnsku` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'FNSKU',
  `opt_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  `type` int NULL DEFAULT 0 COMMENT '流水类型',
  `type_text` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '流水类型文本',
  `sub_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '子类型',
  `sub_type_text` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '子类型文本',
  `product_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '品名',
  `product_good_num` int NULL DEFAULT 0 COMMENT '可用量',
  `product_bad_num` int NULL DEFAULT 0 COMMENT '次品量',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stmt`(`statement_id` ASC) USING BTREE,
  INDEX `idx_sku`(`sku` ASC) USING BTREE,
  INDEX `idx_wid`(`wid` ASC) USING BTREE,
  INDEX `idx_opt_time`(`opt_time` ASC) USING BTREE,
  INDEX `idx_type_opt_time`(`type` ASC, `opt_time` ASC) USING BTREE,
  INDEX `idx_sku_wid_type`(`sku` ASC, `wid` ASC, `type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 874 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '仓库库存流水' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
