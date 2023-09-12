package com.valueplus.smileIdentity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ZipRequestResponse {

    private Actions actions;
    private String ConfidenceValue;
    private PartnerParams PartnerParams;
    private String ResultCode;
    private String ResultText;
    private String SmileJobID;
    private String Source;
    private String timeStamp;
    private String signature;



    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public class Actions{

        private String Liveness_Check;
        private String Register_Selfie;
        private String Selfie_Provided;
        private String Verify_ID_Number;
        private String Human_Review_Compare;
        private String Return_Personal_Info;
        private String Selfie_To_ID_Card_Compare;
        private String Human_Review_Update_Selfie;
        private String Human_Review_Liveness_Check;
        private String Selfie_To_ID_Authority_Compare;
        private String Update_Registered_Selfie_On_File;
        private String Selfie_To_Registered_Selfie_Compare;
    }
}
