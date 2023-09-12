package com.valueplus.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "BetwayCpaReortData")
public class CpaReportData extends BasePersistentEntity {

 @Id()
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 private String brand;

 private String country;

 private String currency;

 private String earnings;

 private String firstPurchaseDate;

 private String lockedPlayers;

 private String monthToDateRegistration;

 private String playerId;

 private String product;

 private String promotion;

 private String qualifiedPlayers;

 private String queryString;

 private String registrationDate;

 private String tag;

 private String trafficSource;

 private String platform;

 private String referringUrl;

}
