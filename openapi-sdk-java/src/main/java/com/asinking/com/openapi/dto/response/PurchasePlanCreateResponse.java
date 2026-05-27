package com.asinking.com.openapi.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class PurchasePlanCreateResponse {
    private String ppgSn;
    private List<String> planSn;
}
