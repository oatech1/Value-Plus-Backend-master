package com.valueplus.domain.service.concretes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BetWayRequest {
    private String trafficSourceName;
    private String brandCode;

}
