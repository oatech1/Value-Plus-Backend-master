package com.valueplus.app.controller;


import com.google.gson.JsonObject;
import com.valueplus.domain.model.BetaCareDTO;
import com.valueplus.domain.service.abstracts.ReferralCounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



@RequiredArgsConstructor
@RestController
public class BetaCareRegistrationController {
   @Autowired
    ReferralCounterService referralCounterService;

    @PostMapping("/update-beta-care-referral-count")
    public @ResponseBody Boolean updateReferral(@RequestBody BetaCareDTO betaCareDTO){
        return referralCounterService
                .addReferralCode(betaCareDTO.getReferralCode());
    }

    @GetMapping("/referral-count/{refCode}")
    public @ResponseBody String getReferralCount (@PathVariable("refCode") String refCode){

        return referralCounterService.getReferralCount(refCode);
    }

}



