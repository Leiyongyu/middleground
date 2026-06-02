/**
 * 前端数据类型定义（JSDoc，提供 VS Code 智能提示而无需 TypeScript 编译）。
 *
 * @typedef {object} PageResult
 * @property {number}  total   - 总记录数
 * @property {number}  page    - 当前页码
 * @property {number}  size    - 每页条数
 * @property {Array}   records - 当前页数据
 *
 * @typedef {object} PageQuery
 * @property {number}  [page]  - 页码，默认 1
 * @property {number}  [size]  - 每页条数，默认 10
 * @property {string}  [sku]   - SKU 筛选
 *
 * @typedef {object} PurchaseSubmitRecord
 * @property {string}  id
 * @property {string}  sku
 * @property {string}  warehouseName
 * @property {number}  quantityPurchase
 * @property {number}  quantityReplenish
 * @property {number}  quantityPlan
 * @property {string}  remark
 * @property {string}  statusText
 * @property {string}  approver
 * @property {string}  approveTime
 * @property {string}  creatorOwnerName
 * @property {string}  creatorAccount
 * @property {string}  submitTime
 * @property {string}  expectArriveTime
 *
 * @typedef {object} DailyPriceTrackingRecord
 * @property {number}  id
 * @property {string}  site
 * @property {string}  skuLevel
 * @property {string}  sku
 * @property {number}  ourLowestPrice
 * @property {number}  trackingPrice
 * @property {number}  trackingProfitMargin
 * @property {number}  floorPrice
 * @property {number}  returnRate
 * @property {number}  last3DaysSales
 * @property {number}  last7DaysSales
 * @property {number}  last30DaysSales
 * @property {number}  last90DaysSales
 * @property {number}  maxMonthlySales
 * @property {string}  ebayFrontpageUrl
 * @property {string}  frontpageSoldUrl
 * @property {number}  overseasWarehouseStock
 * @property {number}  overseasWarehouseAge
 * @property {number}  stockSalesRatio
 * @property {number}  estimatedReplenish
 * @property {string}  brand
 * @property {string}  operator
 * @property {string}  remark
 *
 * @typedef {object} InventoryOverviewRecord
 * @property {string}  warehouseNames
 * @property {string}  sku
 * @property {string}  productName
 * @property {number}  last30DaysProfit
 * @property {number}  overseasOnway
 * @property {number}  overseasSellable
 * @property {number}  overseasTotal
 * @property {number}  purchasePendingDelivery
 * @property {number}  localOnway
 * @property {number}  localSellable
 * @property {string}  purchasePlan
 * @property {number}  lockNum
 * @property {number}  totalInventory
 * @property {number}  last7DaysSales
 * @property {number}  last30DaysSales
 * @property {number}  last90DaysSales
 * @property {number}  overseasInStockRatio
 * @property {number}  overseasTotalRatio
 * @property {number}  totalInventoryRatio
 * @property {number}  purchaseQuantity
 * @property {string}  owner
 */

export default {} // 仅用于类型导入，无运行时值
