package com.valueplus.persistence.entity;

import lombok.*;

import javax.persistence.*;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "kyc_verification_partner_params")
public class KycVerificationPartnerParams {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String jobId;
    private String userId;
    private String jobType;

}
