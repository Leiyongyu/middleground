package com.asinking.com.openapi.dto.response;

/**
 * 仓库下拉选项，按站点标签去重（同国家合并）。
 */
public class WarehouseOptionItem {

    /** 展示标签（美国/德国/英国/成都） */
    private String label;
    /** 该标签下的 wid 列表，逗号分隔 */
    private String wids;

    public WarehouseOptionItem() {}

    public WarehouseOptionItem(String label, String wids) {
        this.label = label;
        this.wids = wids;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getWids() { return wids; }
    public void setWids(String wids) { this.wids = wids; }
}
