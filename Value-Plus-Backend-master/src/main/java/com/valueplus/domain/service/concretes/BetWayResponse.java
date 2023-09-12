package com.valueplus.domain.service.concretes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BetWayResponse {

    private int trafficSourceId;
    private String trafficSourceName;
    private String trackingUrl;
}
