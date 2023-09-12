package com.valueplus.domain.model.bet9ja;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Params {
    @JsonProperty("start_date")
    private String startDate;
    @JsonProperty("end_date")
    private String endDate;
    @JsonProperty("requesting_account_type")
    private String requestingAccountType;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("grouping")
    private String grouping;
    @JsonProperty("show_product_columns")
    private String showProductColumns;
    @JsonProperty("paginate")
    private String paginate;
}
