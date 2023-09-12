package com.valueplus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Slf4j
@Builder
public class AgentData {

    private Integer referredAgent;
////    private Integer activeReferredAgent;
//    private Double commission;
    private String address;
    private String agentName;
    private String agentEmail;
    private String phoneNo;
    private Boolean kycVerified;
    private Boolean enabled;
    private LocalDateTime dateCreated;
//    private BigDecimal walletBalance;
    private int betwayActiveReferrals=0;
    private int betwayTotalReferrals=0;
    private BigDecimal betwayTotalEarning= BigDecimal.ZERO;
    private String betwayAgentReferralCode;
    private int  betaCareTotalReferrals =0;
    private int  betaCareActiveReferrals =0;
    private BigDecimal betaCareTotalEarning= BigDecimal.ZERO;
    private BigDecimal totalApprovedWithdrawals;
    private  BigDecimal totalPendingWithdrawal;
    private Integer pendingWithdrawalCount;
    private BigDecimal totalProductSales;
    private BigDecimal totalProductAgentProfits;


}
