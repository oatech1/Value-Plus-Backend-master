package com.valueplus.smileIdentity;

import lombok.Data;

@Data
public class SmileIdentityResponse {

    private String upload_url;
    private String ref_id;
    private String smile_job_id;
    private String camera_config;
    private String code;
}
