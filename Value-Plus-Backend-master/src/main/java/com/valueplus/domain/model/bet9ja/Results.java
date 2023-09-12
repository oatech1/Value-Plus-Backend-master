package com.valueplus.domain.model.bet9ja;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Results {
    @JsonProperty("activity_date")
    private String activityDate;
    @JsonProperty("vendor_id")
    private String vendorId;
    @JsonProperty("vendor_name")
    private String vendorName;
    @JsonProperty("campaign_id")
    private String campaignId;
    @JsonProperty("campaign_name")
    private String campaignName;
    @JsonProperty("affiliate_profile_id")
    private String affiliateProfileId;
    @JsonProperty("affiliate_profile_username")
    private String affiliateProfileUsername;
    @JsonProperty("affiliate_profile_site_id")
    private String affiliateProfileSiteId;
    @JsonProperty("resource_id")
    private String resourceId;
    @JsonProperty("resource_name")
    private String resourceName;
    @JsonProperty("resource_type")
    private String resourceType;
    @JsonProperty("country_of_registration")
    private String countryOfRegistration;
    @JsonProperty("player_id")
    private String playerId;
    @JsonProperty("registered_date")
    private String registeredDate;
    @JsonProperty("first_deposit_date")
    private String firstDepositDate;
    @JsonProperty("first_bet_date")
    private String firstBetDate;
    @JsonProperty("first_cpa_date")
    private String firstCpaDate;
    @JsonProperty("verification_date")
    private String verificationDate;
    @JsonProperty("deposits")
    private String deposits;
    @JsonProperty("stakes")
    private String stakes;
    @JsonProperty("chargebacks")
    private String chargebacks;
    @JsonProperty("net_revenue")
    private String netRevenue;
    @JsonProperty("total_bonus")
    private String totalBonus;
    @JsonProperty("revshare_commission")
    private String revshareCommission;
    @JsonProperty("cpa_commission")
    private String cpaCommission;
    @JsonProperty("total_commission")
    private String totalCommission;
    @JsonProperty("s1")
    private String s1;

}
