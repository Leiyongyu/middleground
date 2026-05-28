package com.asinking.com.openapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class PurchasePlanCreateResponse {
    @JsonProperty("ppg_sn")
    private String ppgSn;
    @JsonProperty("plan_sn")
    private List<String> planSn;
}
