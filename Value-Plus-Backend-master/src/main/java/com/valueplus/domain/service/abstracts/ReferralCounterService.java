package com.valueplus.domain.service.abstracts;

import com.google.gson.JsonObject;
import com.valueplus.app.exception.ValuePlusException;

public interface ReferralCounterService {
    Boolean addReferralCode(String ref);
    Integer updateCount(String ref);

    String getReferralCount(String referralCode);
}
