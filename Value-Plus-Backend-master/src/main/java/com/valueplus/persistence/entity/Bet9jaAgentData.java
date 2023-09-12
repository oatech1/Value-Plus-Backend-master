package com.valueplus.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="bet9ja_agent_data")
@Getter
@Setter
public class Bet9jaAgentData extends  BasePersistentEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int activeReferrals=0;
    private int totalReferrals=0;
    private BigDecimal totalEarning= BigDecimal.ZERO;
    private String agentReferralCode;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
