package com.valueplus.smileIdentity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Actions {
       @JsonProperty("Liveness_Check")
       private String livenessCheck;
       @JsonProperty("Register_Selfie")
       private String registerSelfie;
       @JsonProperty("Selfie_Provided")
       private String selfieProvided;
       @JsonProperty("Verify_ID_Number")
       private String verifyIdNumber;
       @JsonProperty("Human_Review_Compare")
       private String humanReviewCompare;
       @JsonProperty("Return_Personal_Info")
       private String returnPersonalInfo;
       @JsonProperty("Selfie_To_ID_Card_Compare")
       private String selfieToIdCardCompare;
       @JsonProperty("Human_Review_Update_Selfie")
       private String humanReviewUpdateSelfie;
       @JsonProperty("Human_Review_Liveness_Check")
       private String humanReviewLivenessCheck;
       @JsonProperty("Selfie_To_ID_Authority_Compare")
       private String selfieToIdAuthorityCompare;
       @JsonProperty("Update_Registered_Selfie_On_File")
       private String updateRegisteredSelfieOnFile;
       @JsonProperty("Selfie_To_Registered_Selfie_Compare")
       private String selfieToRegisteredSelfieCompare;
}
