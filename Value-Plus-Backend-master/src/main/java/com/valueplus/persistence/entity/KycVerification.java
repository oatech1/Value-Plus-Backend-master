package com.valueplus.persistence.entity;

import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Table(name = "kyc_verification")
@AllArgsConstructor
@NoArgsConstructor
public class KycVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    private String resultCode;
    private String resultText;
    private String smileJobID;
    private String confidenceValue;
    private Long timestamp;

    @ManyToOne
    @JoinColumn(name = "partner_params_id")
    private KycVerificationPartnerParams partnerParams;

    @ManyToOne
    @JoinColumn(name = "actions_id")
    private KycVerificationActions actions;

    @ManyToOne
    @JoinColumn(name = "kyc_user")
    private User user;


}
