package com.valueplus.smileIdentity;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.simple.JSONObject;

import java.sql.Timestamp;
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmileIdentityRequestUpload {

    private String file_name;
    private String signature;
    private Long timestamp;
    private String smile_client_id;
    private PartnerParams partner_params;
    private JSONObject model_parameters;
    private String callback_url;
}

