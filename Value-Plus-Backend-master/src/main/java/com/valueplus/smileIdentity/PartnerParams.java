package com.valueplus.smileIdentity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PartnerParams {
    private String job_type;
    private String job_id;
    private String user_id;
}
