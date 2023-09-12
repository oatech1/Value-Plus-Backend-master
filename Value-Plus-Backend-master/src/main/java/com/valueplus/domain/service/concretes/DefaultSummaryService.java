package com.valueplus.domain.service.concretes;

import com.google.gson.JsonObject;
import com.valueplus.app.exception.BadRequestException;

import com.valueplus.betway.BetWayAgentData;
import com.valueplus.app.exception.ReferralCodeNotExistException;

import com.valueplus.domain.model.ProductSummary;
import com.valueplus.domain.model.TransactionSummary;
import com.valueplus.domain.products.ValuePlusService;
import com.valueplus.domain.service.abstracts.ReferralCounterService;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.persistence.entity.ReferralCounter;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.domain.model.AccountSummary;
import com.valueplus.domain.service.abstracts.SummaryService;
import com.valueplus.domain.util.FunctionUtil;
import com.valueplus.persistence.entity.ProductOrder;
import com.valueplus.persistence.entity.ProductProviderUser;
import com.valueplus.persistence.entity.Transaction;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.valueplus.domain.enums.OrderStatus.COMPLETED;
import static com.valueplus.domain.model.RoleType.AGENT;
import static com.valueplus.domain.model.RoleType.SUPER_AGENT;
import static java.time.LocalTime.MAX;
import static java.time.LocalTime.MIN;
import static java.util.Optional.ofNullable;
import static org.apache.logging.log4j.util.Strings.isNotBlank;
@RequiredArgsConstructor
@Service
public class DefaultSummaryService implements SummaryService, ReferralCounterService {
    private final ProductProviderUserRepository providerUserRepository;
    private final UserRepository userRepository;
    private final ProductOrderRepository productOrderRepository;
    private final TransactionRepository transactionRepository;
    private final DeviceReportRepository deviceReportRepository;
    private final Clock clock;
    private final ReferralCounterRepository referralCounterRepository;
    private final BetWayAgentDataRepository betWayAgentDataRepository;
    @Lazy
    private final UserService userService;
    private final ValuePlusService valuePlusService;

    @Override
    public AccountSummary getSummary(User user) {
       if (UserUtils.isAgent(user)){
        return    getSummaryAgent(user);
       }
       if (UserUtils.isSuperAgent(user)){
        return  getSummarySuperAgent(user);
       }
       return null;
    }

    public AccountSummary getSummaryAgent(User user){
        Integer totalDownloads = 0;
        Long userId = user.getId();
        Optional<ProductProviderUser> user1 = providerUserRepository.findByUserIdAndProvider(userId,ProductProvider.BETA_CARE);

        Integer activeUsers=0;
        Integer totalAgents=0;

        totalAgents = userService.getTotalDigitalProductsReferralCount(user);
        activeUsers = userService.getActiveVpAgentReferral(user);

        ProductSummary productSummary = calculateProductSummary(productOrderRepository.findByUser_idAndStatus(user.getId(), COMPLETED));

        TransactionSummary transactionSummary = calculateTransactionSummary(transactionRepository.findAllByUser_Id(user.getId()));


        return AccountSummary.builder()
                .totalAgents(totalAgents)
                .totalProductSales(productSummary.getTotalProductSales())
                .totalProductAgentProfits(productSummary.getTotalProductAgentProfits().add(valuePlusService.getVpAgentCommission(user)))
                .totalApprovedWithdrawals(transactionSummary.getTotalApprovedWithdrawals())
                .totalPendingWithdrawal(transactionSummary.getTotalPendingWithdrawal())
                .pendingWithdrawalCount(transactionSummary.getPendingWithdrawalCount())
                .totalActiveUsers(activeUsers)
                .totalDownloads(totalDownloads)
                .build();
    }
    public AccountSummary getSummarySuperAgent(User user){
        Integer totalDownloads = 0;
        Long userId = user.getId();
        Optional<ProductProviderUser> user1 = providerUserRepository.findByUserIdAndProvider(userId,ProductProvider.BETA_CARE);
        String agentReferralId = user1.isPresent() ? user1.get().getAgentCode() : "None";
        Optional<BetWayAgentData> betWayAgentData = betWayAgentDataRepository.findByUser(user);
        Integer activeUsers=0;
        Integer totalAgents=0;

        totalAgents = userService.getTotalDigitalProductsReferralCount(user);

        activeUsers = getActiveUsers(user);
        totalAgents = countTotalAgentUsers(user.getRole().getName(), user.getReferralCode(), user.getAgentCode(),agentReferralId) + userService.getBetwayReferralCount(user)+ userService.getBetacareReferralCount(user);

//        if(betWayAgentData.isPresent()){
//            BetWayAgentData newBetWayAgentData = betWayAgentData.get();
//            activeUsers = countActiveUsers(user.getRole().getName(), user.getReferralCode(), user.getAgentCode()) + newBetWayAgentData.getActiveReferrals();
//
//            totalAgents = countTotalAgentUsers(user.getRole().getName(), user.getReferralCode(), user.getAgentCode(),agentReferralId) + newBetWayAgentData.getTotalReferrals();
//
//        }else {
//            activeUsers = countActiveUsers(user.getRole().getName(), user.getReferralCode(), user.getAgentCode());
//
//            totalAgents = countTotalAgentUsers(user.getRole().getName(), user.getReferralCode(), user.getAgentCode(),agentReferralId);
//
//        }



        ProductSummary productSummary = calculateProductSummary(productOrderRepository.findByUser_idAndStatus(user.getId(), COMPLETED));

        TransactionSummary transactionSummary = calculateTransactionSummary(transactionRepository.findAllByUser_Id(user.getId()));


        return AccountSummary.builder()
                .totalAgents(totalAgents)
                .totalProductSales(productSummary.getTotalProductSales())
                .totalProductAgentProfits(productSummary.getTotalProductAgentProfits())
                .totalApprovedWithdrawals(transactionSummary.getTotalApprovedWithdrawals())
                .totalPendingWithdrawal(transactionSummary.getTotalPendingWithdrawal())
                .pendingWithdrawalCount(transactionSummary.getPendingWithdrawalCount())
                .totalActiveUsers(activeUsers)
                .totalDownloads(totalDownloads)
                .build();
    }


    @Override
    public AccountSummary getSummaryAllUsers() throws ValuePlusException {
        Integer totalDownloads = 0;
        Integer activeUsers = ((Long) deviceReportRepository.count()).intValue();
        Integer totalAgents = userRepository.countUserByRole_NameInAndDeleted(List.of(AGENT.name()), false).intValue();

        ProductSummary productSummary = calculateProductSummary(productOrderRepository.findByStatus(COMPLETED));
        TransactionSummary transactionSummary = calculateTransactionSummary(transactionRepository.findAll());

        return AccountSummary.builder()
                .totalAgents(totalAgents)
                .totalProductSales(productSummary.getTotalProductSales())
                .totalProductAgentProfits(productSummary.getTotalProductAgentProfits())
                .totalApprovedWithdrawals(transactionSummary.getTotalApprovedWithdrawals())
                .totalPendingWithdrawal(transactionSummary.getTotalPendingWithdrawal())
                .pendingWithdrawalCount(transactionSummary.getPendingWithdrawalCount())
                .totalActiveUsers(activeUsers)
                .totalDownloads(totalDownloads)
                .build();
    }

//    private Integer countActiveUsers(String roleName, String referralCode, String agentCode) {
//        LocalDate todayDate = LocalDate.now(clock);
//
//        if (SUPER_AGENT.name().equals(roleName)) {
//            LocalDateTime startDateTime = LocalDateTime.of(todayDate.minusDays(30), MIN);
//            LocalDateTime endDateTime = LocalDateTime.of(todayDate, MAX);
//            return userRepository.findActiveSuperAgentListUsers(startDateTime, endDateTime, referralCode).size();
//        }
//
//        return deviceReportRepository.countAllByAgentCode(agentCode).intValue();
//    }

    private Integer countTotalAgentUsers(String roleName, String referralCode, String agentCode, String agentReferralCode) {


        if (SUPER_AGENT.name().equals(roleName)) {
            return userRepository.findUsersBySuperAgent_ReferralCode(referralCode).size();
        }
        if(AGENT.name().equals(roleName)){
            Optional<ReferralCounter> count = referralCounterRepository.findByReferralCode(agentReferralCode);
            return  count.isPresent() ? count.get().getCount() : 0 ;
        }
        return ofNullable(agentCode)
                .map(a -> isNotBlank(a) ? 1 : 0)
                .orElse(0);
    }

     ProductSummary calculateProductSummary(List<ProductOrder> productOrders) {
        AtomicReference<BigDecimal> totalProductAgentProfits = new AtomicReference<>(FunctionUtil.setScale(BigDecimal.ZERO));
        AtomicReference<BigDecimal> totalProductSales = new AtomicReference<>(FunctionUtil.setScale(BigDecimal.ZERO));

        productOrders.forEach(po -> {
            BigDecimal sellingPrice = FunctionUtil.setScale(po.getSellingPrice());
            BigDecimal productPrice = FunctionUtil.setScale(po.getProduct().getPrice());
            BigDecimal profit = FunctionUtil.setScale(sellingPrice.subtract(productPrice)).multiply(BigDecimal.valueOf(po.getQuantity()));

            BigDecimal sellingAmount = sellingPrice.multiply(BigDecimal.valueOf(po.getQuantity()));
            totalProductAgentProfits.accumulateAndGet(profit, BigDecimal::add);
            totalProductSales.accumulateAndGet(sellingAmount, BigDecimal::add);

        });

        return new ProductSummary(totalProductSales.get(), totalProductAgentProfits.get());
    }


    public TransactionSummary calculateTransactionSummary(List<Transaction> transactions) {
        AtomicReference<BigDecimal> totalApprovedWithdrawals = new AtomicReference<>(FunctionUtil.setScale(BigDecimal.ZERO));
        AtomicReference<BigDecimal> totalPendingWithdrawal = new AtomicReference<>(FunctionUtil.setScale(BigDecimal.ZERO));
        AtomicReference<Integer> pendingWithdrawalCount = new AtomicReference<>(0);

        transactions.forEach(tr -> {
            if (isSuccessfulTransaction(tr)) {
                totalApprovedWithdrawals.accumulateAndGet(tr.getAmount(), BigDecimal::add);
            } else if (isPendingTransaction(tr)) {
                totalPendingWithdrawal.accumulateAndGet(tr.getAmount(), BigDecimal::add);
                pendingWithdrawalCount.updateAndGet(v -> v + 1);
            }
        });
        TransactionSummary transactionSummary = new TransactionSummary(
                totalApprovedWithdrawals.get(),
                totalPendingWithdrawal.get(),
                pendingWithdrawalCount.get());

        return transactionSummary;
    }

    private boolean isSuccessfulTransaction(Transaction tr) {
        return "success".equalsIgnoreCase(tr.getStatus());
    }

    private boolean isPendingTransaction(Transaction tr) {
        return !isSuccessfulTransaction(tr) && !"error".equalsIgnoreCase(tr.getStatus()) && !"failed".equalsIgnoreCase(tr.getStatus());
    }

    public Integer getActiveUsers(User user){
      List <User> users =  userService.getSuperAgentReferrals(user);
      return userService.getActiveSuperAgentReferral(users);
    }

    @Override
    public Boolean addReferralCode(String referralCode)  {
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        ProductProviderUser code = providerUserRepository
                .findByAgentCodeAndProvider(referralCode, ProductProvider.BETA_CARE)
                .orElseThrow(()->new BadRequestException("Invalid Referral code"));

        referralCounterRepository.findByReferralCode(referralCode).ifPresentOrElse(refCounter ->{
            refCounter.setCount(refCounter.getCount() + 1);
            referralCounterRepository.save(refCounter);
            result.set(true);
        }, ()->{
            ReferralCounter counter = new ReferralCounter();
            counter.setReferralCode(referralCode);
            counter.setCount(1);
            referralCounterRepository.save(counter);
            result.set(true);
        } );

        return result.get();
    }

    @Override
    public Integer updateCount(String ref) {
        return null;
    }

    @Override
    public String getReferralCount(String referralCode){
      Optional<ReferralCounter> referralCounter =  referralCounterRepository.findByReferralCode(referralCode);
      if (referralCounter.isEmpty()){
          throw new ReferralCodeNotExistException("Referral code is invalid : " + referralCode);
      }
      JsonObject object = new JsonObject();
      object.addProperty("count",referralCounter.get().getCount());

       return object.toString() ;
    }

    public void getTotalWithdrawalSummary(){
        User admin = UserUtils.getLoggedInUser();
       if (UserUtils.isAdmin(admin)){


       }

    }

    public void getMonthlyActiveUsers(){

    }


}
