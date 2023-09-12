package com.valueplus.persistence.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "otp_verification_token")
public class OtpVerificationToken {

    @Id
    private Long userId;
    private String verificationToken;
    private LocalDateTime expireTime;
    private String status;
    private String body;
    private String errorCode;
    private String errorReason;
    private String referenceID;
    private Boolean verified;

}
