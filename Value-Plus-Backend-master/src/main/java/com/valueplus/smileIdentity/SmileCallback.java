package com.valueplus.smileIdentity;

import lombok.Builder;
import lombok.Data;



@Builder
@Data
public class SmileCallback {

    private Actions Actions;
    private String ResultCode;
    private String ResultText;
    private String SmileJobID;
    private PartnerParams PartnerParams;
    private String ConfidenceValue;
    private String timestamp;


}
