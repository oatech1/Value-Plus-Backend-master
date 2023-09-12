package com.valueplus.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class RevShareReportData extends  BasePersistentEntity{
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
    private String brand;
    private String chargebacks;
    private String country;
    private String currency;
    private String earnings;
    private String earningsAfterTax;
    private String firstPurchaseDate;
    private String gamingRevenue;
    private String lockedPlayers;
    private String monthToDateRegistrations;
    private String platform;
    private String playerId;
    private String product;
    private String progressives;
    private String promotion;
    private String purchases;
    private String qualifiedPlayers;
    private String queryString;
    private String referringUrl;
    private String registrationDate;
    private String tag;
    private String tax;
    private String tier;
    private String totalCommisionableRevenue;
    private String trafficSource;

}
