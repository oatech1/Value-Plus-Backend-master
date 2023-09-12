package com.valueplus.domain.products;

import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.domain.model.AgentReport;
import com.valueplus.domain.model.data4Me.ProductProviderAgentDto;
import com.valueplus.persistence.entity.ProductProviderUser;
import com.valueplus.persistence.entity.ReferralCounter;
import com.valueplus.persistence.entity.Role;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.ProductProviderUserRepository;
import com.valueplus.persistence.repository.ReferralCounterRepository;
import com.valueplus.persistence.repository.RoleRepository;
import com.valueplus.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.valueplus.domain.util.FunctionUtil.emptyIfNullStream;
import static com.valueplus.domain.util.GeneratorUtils.generateRandomString;
import static com.valueplus.domain.util.UserUtils.getLoggedInUser;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
@Slf4j
@RequiredArgsConstructor
@Service
public class ValuePlusProductProvider implements ProductProviderService, ProductProviderUrlService {

    private final ValuePlusService valuePlusService;
    @Autowired
    ProductProviderUserRepository productProviderUserRepository;
    @Autowired
    ReferralCounterRepository referralCounterRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleRepository roleRepository;

    public String create(User user) {
        String referralUrl ="";
       boolean code = Optional.ofNullable(user.getAgentCode()).isEmpty();
//        Optional<ProductProviderUser> data4me =  productProviderUserRepository.findByUserIdAndProvider(user.getId(),ProductProvider.DATA4ME);
//        if (data4me.isPresent()){productProviderUserRepository.delete(data4me.get());}
        if (code){
        String referralCode = "VP".concat(user.getId().toString()).concat(generateRandomString(3).toLowerCase());
        log.info(referralCode.concat(" referral code"));
        String signup = "https://app.valueplusagency.com/signup/";
        referralUrl = signup.concat(referralCode);
        ProductProviderUser productProviderUser = new ProductProviderUser();
        productProviderUser.setProvider(ProductProvider.VALUEPLUS);
        productProviderUser.setUser(user);
        productProviderUser.setAgentCode(referralCode);
        productProviderUser.setAgentUrl(referralUrl);
        this.productProviderUserRepository.save(productProviderUser);
        user.setAgentCode(referralCode);
        user.setReferralCode(referralUrl);
        userRepository.saveAndFlush(user);

        ReferralCounter referralCounter = new ReferralCounter();
        referralCounter.setReferralCode(referralCode);
        referralCounter.setCount(0);
        this.referralCounterRepository.save(referralCounter);}
        else {referralUrl = user.getReferralCode();}
        return referralUrl;}


    @Override
    public ProductProvider provider() {
        return ProductProvider.VALUEPLUS;
    }

    @Override
    public String getReferralUrl(String agentCode, String agentUrl) {
        return agentUrl;
    }

    @Override
    public ProductProviderUserModel create(Object authDetails, ProductProviderUserModel userModel) {
        String referralUrl ="";
            var request = ProductProviderAgentDto.from(userModel);
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            String referralCode = generateVpReferralCode(user);
            referralUrl = generateVpReferralUrl(referralCode);
            ProductProviderUser productProviderUser = new ProductProviderUser();
            productProviderUser.setProvider(ProductProvider.VALUEPLUS);
            productProviderUser.setUser(user);
            productProviderUser.setAgentCode(referralCode);
            productProviderUser.setAgentUrl(referralUrl);
            this.productProviderUserRepository.save(productProviderUser);
            user.setAgentCode(referralCode);
            user.setReferralCode(referralUrl);
            userRepository.saveAndFlush(user);

            ReferralCounter referralCounter = new ReferralCounter();
            referralCounter.setReferralCode(referralCode);
            referralCounter.setCount(0);
            this.referralCounterRepository.save(referralCounter);


        userModel.setAgentCode(referralCode);
        userModel.setReferralUrl(referralUrl);
        return userModel;
    }

    public void createIfFailss(User userAgent) {
        String referralUrl ="";

        User user = userRepository.findByEmail(userAgent.getEmail()).orElse(null);
        String referralCode = generateVpReferralCode(user);
        referralUrl = generateVpReferralUrl(referralCode);
        ProductProviderUser productProviderUser = new ProductProviderUser();
        productProviderUser.setProvider(ProductProvider.VALUEPLUS);
        productProviderUser.setUser(user);
        productProviderUser.setAgentCode(referralCode);
        productProviderUser.setAgentUrl(referralUrl);
        this.productProviderUserRepository.save(productProviderUser);
        user.setAgentCode(referralCode);
        user.setReferralCode(referralUrl);
        userRepository.saveAndFlush(user);

        ReferralCounter referralCounter = new ReferralCounter();
        referralCounter.setReferralCode(referralCode);
        referralCounter.setCount(0);
        this.referralCounterRepository.save(referralCounter);

    }

    public String generateVpReferralCode(User user){
        String referralCode = "VP".concat(user.getId().toString()).concat(generateRandomString(3).toLowerCase());
        log.info(referralCode.concat(" referral code"));
        return referralCode;
    }


    public void generateVpReferral() {
        Optional<Role> role = roleRepository.findByName("AGENT");
        List<User>allUsers = userRepository.findUsersByAgentCodeIsNullAndRole(role.get());
        for (User eachUser:allUsers) {
            Integer count=0;
            count++;
            log.info(count.toString());
        String referralCode = "VP".concat(eachUser.getId().toString()).concat(generateRandomString(3).toLowerCase());
        log.info(referralCode.concat(" referral code"));
        String referralUrl;
        String signup = "https://app.valueplusagency.com/signup/";
        referralUrl = signup.concat(referralCode);
        eachUser.setAgentCode(referralCode);
        eachUser.setReferralCode(referralUrl);
        userRepository.save(eachUser);
        log.info(eachUser.toModel().toString());
        }
    }

    private String generateVpReferralUrl(String referralCode){
        String referralUrl;
        String signup = "https://app.valueplusagency.com/signup/";
        referralUrl = signup.concat(referralCode);
        return referralUrl;
    }

    @Override
    public Optional<ProductProviderUserModel> get(Object authDetails, String email) {
        return Optional.empty();
    }

    @Override
    public Object authenticate() {
        return null;
    }

    @Override
    public Set<AgentReport> downloadAgentReport(LocalDate reportDate) {
        return null;
    }
    private ProductProviderUserModel toUserModel(ValuePlusService.AgentInfoModel infoModel) {
        return ProductProviderUserModel.builder()
                .email(infoModel.getEmail())
                .provider(provider())
                .agentCode(infoModel.getAgentCode())
                .referralUrl(infoModel.getReferralUrl())
                .build();
    }
}
