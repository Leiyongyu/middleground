package com.asinking.com.openapi.dto.request;

import lombok.Data;
import java.util.List;

/**
 * 补货页面搜索请求体。
 */
@Data
public class OverviewSearchRequest {
    private long page = 1;
    private long size = 100;
    private String sortField;
    private String sortOrder;
    private List<FilterItem> filters;

    @Data
    public static class FilterItem {
        private String field;
        private String value;
    }
}
