package com.valueplus.domain.products;

import com.valueplus.app.exception.ValuePlusRuntimeException;
import com.valueplus.domain.enums.OrderStatus;
import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.domain.enums.TransactionType;
import com.valueplus.domain.service.concretes.UserService;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.entity.Wallet;
import com.valueplus.persistence.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static com.valueplus.domain.util.UserUtils.getLoggedInUser;
import static com.valueplus.domain.util.UserUtils.isAgent;

@Service
@Slf4j
@RequiredArgsConstructor
public class ValuePlusService {

//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private ReferralCounterRepository referralCounterRepository;

    private final UserRepository userRepository;
    private final WalletHistoryRepository walletHistoryRepository;
    private final WalletRepository  walletRepository;
    private final ProductOrderRepository productOrderRepository;
    private final String vpCommission = "ValuePlus Commission";

    public ProductProviderUserModel getAgentInfo() {
        User user = getLoggedInUser();

       return ProductProviderUserModel.builder().
                firstname(user.getFirstname())
                .lastname(user.getLastname())
                .agentCode(user.getAgentCode())
                .email(user.getEmail())
                .address(user.getAddress())
                .phone(user.getPhone())
                .provider(ProductProvider.VALUEPLUS)
                .referralUrl(user.getReferralCode()).build();
    }

    public VpAgentDetails getVpAgentDetails(){
        User user = getLoggedInUser();
        log.debug("passed login");
        if (!isAgent(user)) {
            throw new ValuePlusRuntimeException("Invalid credentials");
        }

        return VpAgentDetails.builder()
                .activeUser(findActiveAgentUnderVpAgent(user))
                .commission(getVpAgentCommission(user))
                .totalUser(findAgentUnderVpAgent(user)).build();
    }

    public Integer findActiveAgentUnderVpAgent(User user)
    {   Integer number = 0;
         List<User> agents = userRepository.findUsersByAgent(user);
    log.info(" before call");
        Integer count =0;
        for (User userList:agents) {
//            Integer vpAgentCount = userRepository.findUsersByAgent(userList).size();
            Integer vpAgentCount2 =  productOrderRepository.countByUserAndStatus(userList , OrderStatus.COMPLETED);
//            Boolean referred =userService.hasAgentReferrals(userList);

            if (vpAgentCount2>0) {number++;}}
        log.debug(count.toString());
        log.debug("passed active user");
        return number;
    }
        private Integer findAgentUnderVpAgent(User user){
            log.debug("passed agent under vpagent user");
          return userRepository.countAgentUnderVpAgent(user);
        }
        public BigDecimal getVpAgentCommission(User user){
            Optional<Wallet> wallet = this.walletRepository.findByUser(user);
            log.debug("passed vpagent commison");
            if (wallet.isPresent()){log.info("present");
                System.out.println(wallet.get().toModel()); }
           Optional<BigDecimal> amount ;
           amount = walletHistoryRepository.findCommissionForVpAgent(wallet.get(), TransactionType.CREDIT);
           if (amount.isPresent()){return amount.get();}
           return new BigDecimal(0.00);
        }



    @lombok.Value
    @Builder
    public static class AgentInfoModel {
        String name;
        String email;
        String referralUrl;
        String agentCode;
    }

    @lombok.Value
    public static class AgentReport {
        String agentCode;
        Integer activeUser;
    }
    @lombok.Value
    @Builder
    public static class VpAgentDetails{
        BigDecimal commission;
        Integer activeUser;
        Integer totalUser;
    }
}
