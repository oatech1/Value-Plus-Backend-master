package com.valueplus.smileIdentity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GenerateSignatureModel {

    private String signature;
    private Long timestamp;
}
