package com.valueplus.domain.products;

import com.valueplus.domain.model.CurrencyConverterResponse;
import com.valueplus.domain.model.MessageResponse;
import com.valueplus.domain.model.bet9ja.Bet9jaResponse;
import com.valueplus.domain.service.abstracts.HttpApiClient;
import com.valueplus.persistence.entity.Bet9jaAgentData;
import com.valueplus.persistence.entity.Bet9jaCpaData;
import com.valueplus.persistence.entity.Bet9jaReferrals;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.Bet9jaCpaDataRepository;
import com.valueplus.persistence.repository.Bet9jaReferralsRepository;
import com.valueplus.persistence.repository.Bet9jaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class Bet9jaService extends HttpApiClient {
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    private Bet9jaReferralsRepository referralsRepository;
    @Autowired
    private Bet9jaCpaDataRepository bet9jaCpaDataRepository;
    @Autowired
    private Bet9jaRepository bet9jaRepository;

    @Value("${bet9ja.api.token}")
    private final String token;



    public Bet9jaService(RestTemplate restTemplate) {
        super("bet9ja agent report", restTemplate, "https://webaffiliates.bet9ja.com/api/reporting/player?");

        token = null;
    }

    public Bet9jaResponse getAgentReportData(String startDayOfMonth, String endDayOfMonth){

        String url = "start_date=".concat(startDayOfMonth).concat("&end_date=").concat(endDayOfMonth).concat("&currency=NGN&grouping=player_by_day&requesting_account_type=affiliate&show_product_columns=0&paginate=0");
        Map<String, String> header = new HashMap<>();
        header.put("Authorization","Bearer " +token);

        var type = new ParameterizedTypeReference<Bet9jaResponse>() {};
        Bet9jaResponse response = sendRequest(HttpMethod.GET, url, null, header, type);

        return response;

    }

    public MessageResponse registerBet9ja(User user){
        Bet9jaReferrals referralCode = referralsRepository.findFirstByUsedFalse();
        Bet9jaAgentData bet9jaAgentData = new Bet9jaAgentData();
        Bet9jaCpaData bet9jaCpaData = new Bet9jaCpaData();

        referralCode.setUserId(user.getId());
        referralCode.setUsed(true);
        bet9jaAgentData.setAgentReferralCode(referralCode.getCode());
        bet9jaAgentData.setTotalReferrals(0);
        bet9jaAgentData.setActiveReferrals(0);
        bet9jaCpaData.setActiveReferrals(0);
        bet9jaCpaData.setTotalReferrals(0);
        bet9jaCpaData.setAgentReferralCode(referralCode.getCode());

        referralsRepository.save(referralCode);
        bet9jaRepository.save(bet9jaAgentData);
        bet9jaCpaDataRepository.save(bet9jaCpaData);
        MessageResponse messageResponse = new MessageResponse();

        messageResponse.setMessage(referralCode.getCode());
        return messageResponse;
    }
}
