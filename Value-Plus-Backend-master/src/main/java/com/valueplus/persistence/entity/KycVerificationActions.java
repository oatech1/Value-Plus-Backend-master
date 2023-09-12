package com.valueplus.persistence.entity;

import lombok.*;

import javax.persistence.*;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "kyc_verification_actions")
public class KycVerificationActions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    String livenessCheck;
    String registerSelfie;
    String selfieProvided;
    String verifyIdNumber;
    String returnPersonalInfo;
    String selfieToIdAuthorityCompare;
    String jobId;
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;


}
