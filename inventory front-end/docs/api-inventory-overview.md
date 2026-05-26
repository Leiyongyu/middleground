# 库存概览汇总接口

## 基本信息

| 项目 | 值 |
|---|---|
| 接口路径 | `GET /api/inventory-overview` |
| 请求方式 | GET |
| 认证方式 | JWT Bearer Token |

---

## 接口列表

| 序号 | 接口 | 说明 |
|---|---|---|
| 1 | `GET /api/inventory-overview` | 库存概览全量数据 |
| 2 | `GET /api/inventory-overview?sku=xxx` | SKU 模糊搜索 |
| 3 | `GET /api/inventory-overview/warehouses` | 仓库下拉选项 |

---

## 1. 库存概览 / SKU 搜索

### URL

```
GET /api/inventory-overview
GET /api/inventory-overview?sku=BMW
```

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sku` | String | 否 | SKU 模糊搜索，不传返回全部，传 `BMW` 则匹配所有含 BMW 的 SKU |

### 请求示例

```http
GET /api/inventory-overview?sku=BMW
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## 响应

### 成功响应

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "warehouseNames": "谷仓 新泽西区,谷仓 加州区",
      "sku": "OTH-230024-0103",
      "productName": "进气歧管",
      "last30DaysProfit": null,
      "overseasOnway": 0,
      "overseasSellable": 14,
      "overseasTotal": 14,
      "localOnway": 0,
      "localSellable": 0,
      "purchasePlan": null,
      "lockNum": 0,
      "totalInventory": 14,
      "last7DaysSales": 1,
      "last30DaysSales": 4,
      "last90DaysSales": 7,
      "maxMonthlySales": null,
      "overseasInStockRatio": 3.5,
      "overseasTotalRatio": 3.5,
      "totalInventoryRatio": 3.5,
      "lastLocalOutboundTime": null,
      "outboundDays": null,
      "purchaseCycle": null,
      "purchaseQuantity": 0,
      "maxMonthlyReplenish": null,
      "owner": "王玉梅"
    }
  ]
}
```

### 错误响应

```json
{
  "code": 401,
  "message": "Missing token",
  "data": null
}
```

---

---

## 2. 仓库下拉选项

### URL

```
GET /api/inventory-overview/warehouses
```

### 请求参数

无

### 响应示例

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    { "label": "成都", "wids": "18676,18675,18674" },
    { "label": "美国", "wids": "18701,18700" },
    { "label": "英国", "wids": "18702" },
    { "label": "德国", "wids": "18699" }
  ]
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `label` | String | 展示标签（同国家合并，如加州+新泽西→美国） |
| `wids` | String | 该标签下的仓库 wid，逗号分隔 |

### 前端调用示例

```js
import { apiGet } from '@/api/request'

export function getWarehouseOptions() {
  return apiGet('/api/inventory-overview/warehouses')
}
```

---

## 字段说明

### 库存相关

| 字段 | 类型 | 说明 | 数据来源 |
|---|---|---|---|
| `warehouseNames` | String | 站点名称，同一 SKU 多仓库用逗号拼接 | `warehouse.name` |
| `sku` | String | SKU | `warehouse_inventory_detail.sku` |
| `productName` | String | 产品名称 | `sale_stat.product_name` |
| `overseasOnway` | int | 海外在途（调拨在途） | 海外仓(type=3) `third_inventory.qty_onway` 汇总 |
| `overseasSellable` | int | 海外可售（可用量） | 海外仓(type=3) `third_inventory.qty_sellable` 汇总 |
| `overseasTotal` | int | 海外总库存 | `overseasSellable + overseasOnway` |
| `localOnway` | int | 成都在途（待到货量） | 本地仓(type=1) `third_inventory.qty_onway` 汇总 |
| `localSellable` | int | 成都可售（可用量） | 本地仓(type=1) `third_inventory.qty_sellable` 汇总 |
| `lockNum` | int | 待出库（可用锁定量） | `product_lock_num` 汇总 |
| `totalInventory` | int | 整个周期总库存 | `海外可售+海外在途+成都可售+成都在途` |

### 销量相关

| 字段 | 类型 | 说明 | 数据来源 |
|---|---|---|---|
| `last7DaysSales` | int | 近7天销量 | `sale_stat` 按 SKU 聚合 |
| `last30DaysSales` | int | 近30天销量 | `sale_stat` 按 SKU 聚合 |
| `last90DaysSales` | int | 近3月销量 | `sale_stat` 按 SKU 聚合 |

### 库销比（分母为0时返回0）

| 字段 | 类型 | 计算公式 |
|---|---|---|
| `overseasInStockRatio` | BigDecimal | `overseasSellable / last30DaysSales` |
| `overseasTotalRatio` | BigDecimal | `overseasTotal / last30DaysSales` |
| `totalInventoryRatio` | BigDecimal | `totalInventory / last30DaysSales` |

### 负责人

| 字段 | 类型 | 匹配规则 |
|---|---|---|
| `owner` | String | SKU 第一个 `-` 前的前缀 → `brand_owner.brand_code` |

> 例：`MCD-20252-0293-YXR` → 前缀 `MCD` → 匹配 `brand_owner.brand_code = "MCD"` → `owner = "杨萍"`

### 暂未实现（返回 null 或 0）

| 字段 | 类型 | 说明 |
|---|---|---|
| `last30DaysProfit` | BigDecimal? | 近30天利润 |
| `maxMonthlySales` | Integer? | 历史最大月销 |
| `purchasePlan` | String? | 采购计划 |
| `lastLocalOutboundTime` | String? | 最近成都仓出库时间 |
| `outboundDays` | Integer? | 出库天数 |
| `purchaseCycle` | Integer? | 采购周期 |
| `maxMonthlyReplenish` | Integer? | 最大月销预估补货量 |
| `purchaseQuantity` | BigDecimal | 采购数量 = 近3月日均销量 × (出库天数 + 采购周期)，目前两值都为0 |

---

## 前端调用示例

```js
import { apiGet } from '@/api/request'

export async function fetchInventoryOverview() {
  return apiGet('/api/inventory-overview')
}
```

```vue
<script setup>
import { ref, onMounted } from 'vue'
import { fetchInventoryOverview } from '@/api/inventory-overview'

const list = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    list.value = await fetchInventoryOverview()
  } finally {
    loading.value = false
  }
})
</script>
```
