package com.valueplus.domain.products;

import com.valueplus.domain.enums.ProductProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BetWayProductProvider implements ProductProviderUrlService{
    @Override
    public ProductProvider provider() {
        return ProductProvider.BETWAY;
    }

    @Override
    public String getReferralUrl(String agentCode, String agentUrl) {
        return agentUrl;
    }
}
