package com.valueplus.domain.service.concretes;

import com.valueplus.app.config.audit.AuditEventPublisher;
import com.valueplus.app.exception.BadRequestException;
import com.valueplus.app.exception.NotFoundException;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.app.exception.ValuePlusRuntimeException;
import com.valueplus.betway.BetWayAgentData;
import com.valueplus.domain.enums.OrderStatus;
import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.domain.enums.State;
import com.valueplus.domain.mail.EmailService;
import com.valueplus.domain.model.*;
import com.valueplus.domain.products.ProductProviderUrlService;
import com.valueplus.domain.service.abstracts.PinUpdateService;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.persistence.entity.*;
import com.valueplus.persistence.repository.*;
import com.valueplus.persistence.specs.SearchCriteria;
import com.valueplus.persistence.specs.SearchOperation;
import com.valueplus.persistence.specs.UserSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.valueplus.domain.enums.ActionType.*;
import static com.valueplus.domain.enums.EntityType.USER;
import static com.valueplus.domain.enums.OrderStatus.*;
import static com.valueplus.domain.model.RoleType.ADMIN;
import static com.valueplus.domain.util.Constants.WHITE_LISTED_AUTHORITIES_UI;
import static com.valueplus.domain.util.FunctionUtil.emptyIfNullStream;
import static com.valueplus.domain.util.MapperUtil.copy;
import static com.valueplus.domain.util.UserUtils.*;
import static java.time.LocalTime.MAX;
import static java.time.LocalTime.MIN;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final List<PinUpdateService> pinUpdateServiceList;
    private final UserUtilService userUtilService;
    private final Clock clock;
    private final RoleRepository roleRepository;
    private final AuditEventPublisher auditEvent;
    private final EmailService emailService;
    private final AuditLogRepository auditLogRepository;
    private final WalletRepository walletRepository;
    private final ProductProviderUserRepository productProviderUserRepository;
    private final DefaultSummaryService defaultSummaryService;
    private final TransactionRepository transactionRepository;
    private final BetWayAgentDataRepository betWayAgentDataRepository;
    private final ProductOrderRepository productOrderRepository;
    private final ReferralCounterRepository referralCounterRepository;
    @Autowired
    private DigitalProductCommissionRepository commissionRepository;

    public UserService(UserRepository userRepository, List<PinUpdateService> pinUpdateServiceList, UserUtilService userUtilService, Clock clock, RoleRepository roleRepository, AuditEventPublisher auditEvent, EmailService emailService, AuditLogRepository auditLogRepository, WalletRepository walletRepository, ProductProviderUserRepository productProviderUserRepository, @Lazy DefaultSummaryService defaultSummaryService, TransactionRepository transactionRepository, BetWayAgentDataRepository betWayAgentDataRepository, ProductOrderRepository productOrderRepository, ReferralCounterRepository referralCounterRepository) {
        this.userRepository = userRepository;
        this.pinUpdateServiceList = pinUpdateServiceList;
        this.userUtilService = userUtilService;
        this.clock = clock;
        this.roleRepository = roleRepository;
        this.auditEvent = auditEvent;
        this.emailService = emailService;
        this.auditLogRepository = auditLogRepository;
        this.walletRepository = walletRepository;
        this.productProviderUserRepository = productProviderUserRepository;
        this.defaultSummaryService = defaultSummaryService;
        this.transactionRepository = transactionRepository;
        this.betWayAgentDataRepository = betWayAgentDataRepository;
        this.productOrderRepository = productOrderRepository;
        this.referralCounterRepository = referralCounterRepository;
    }

    private final static String ADMIN_ACCOUNT = "vpadmin@gmail.com";

    public Page<User> findUsers(Pageable pageable) {

        return userRepository.findUsersByDeletedFalse(pageable);
    }

    public Page<User> findSuperAgentUsers(Pageable pageable) {
       return userRepository.findUserByRole_NameAndDeletedFalse(RoleType.SUPER_AGENT.name(), pageable);
    }

    public Optional<User> find(long userId) {
        return userRepository.findByIdAndDeletedFalse(userId);
    }

    public Optional<User> findByReferralCode(String superAgentCode) {
        return userRepository.findByReferralCode(superAgentCode);
    }

    public Page<User> findAllUserBySuperAgentCode(String superAgentCode, Pageable pageable) {
        return userRepository.findUserBySuperAgent_ReferralCode(superAgentCode, pageable);

    }

    public Page<User> findAllUserByVpAgentCode(Long agentId, Pageable pageable) {
        User user = userRepository.findById(agentId).orElseThrow(() -> new BadRequestException("Invalid Agent Id"));
        return userRepository.findUserByAgent_Id(agentId, pageable);
    }
    public Page<User>findAllSubAdmin(Pageable pageable){
        return userRepository.findAllSub_Admin("SUB_ADMIN",false, pageable);
    }


    public Page<User> findAllUserBySuperAgentCode(String superAgentCode,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  Pageable pageable) {
        LocalDate todayDate = LocalDate.now(clock);
        LocalDateTime startDateTime = ofNullable(startDate)
                .map(st -> LocalDateTime.of(startDate, MIN))
                .orElseGet(() -> LocalDateTime.of(todayDate.minusDays(30), MIN));
        LocalDateTime endDateTime = ofNullable(startDate)
                .map(st -> LocalDateTime.of(endDate, MAX))
                .orElseGet(() -> LocalDateTime.of(todayDate, MAX));

        return userRepository.findActiveSuperAgentUsers(startDateTime, endDateTime, superAgentCode, pageable);
    }

    public User update(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("user not found"));

        var oldObject = copy(existingUser, User.class);

        existingUser.setLastname(user.getLastname());
        existingUser.setFirstname(user.getFirstname());
        existingUser.setPhone(user.getPhone());
        existingUser.setAddress(user.getAddress());
        existingUser.setEmail(existingUser.getEmail());

        var savedEntity = userRepository.save(existingUser);
        auditEvent.publish(oldObject, savedEntity, USER_PROFILE_UPDATE, USER);
        return savedEntity;
    }

    public User updateAdminAuthority(Long userid, Set<Long> userAuthorities) {
        User user = userRepository.findById(userid)
                .orElseThrow(() -> new NotFoundException("user not found"));
        var oldObject = copy(user, User.class);

        if (!ADMIN.name().equals(user.getRole().getName())) {
            throw new BadRequestException("Authority update only applies to admin users");
        }

        List<Authority> authorityEntity = userUtilService.getAdminAuthority(userAuthorities);
        user.setAuthorities(authorityEntity);

        var savedEntity = userRepository.save(user);
        auditEvent.publish(oldObject, savedEntity, USER_AUTHORITY_UPDATE, USER);
        return savedEntity;
    }

    public User updateUserAuthority(Long userid, Set<Long> userAuthorities) {
        User user = userRepository.findById(userid)
                .orElseThrow(() -> new NotFoundException("user not found"));
        var oldObject = copy(user, User.class);

        if (ADMIN.name().equals(user.getRole().getName())) {
            throw new BadRequestException("Authority update only applies to users");
        }

        List<Authority> authorityEntity = userUtilService.getAdminAuthority(userAuthorities);
        user.setAuthorities(authorityEntity);

        var savedEntity = userRepository.save(user);
        auditEvent.publish(oldObject, savedEntity, USER_AUTHORITY_UPDATE, USER);
        return savedEntity;
    }

    public List<AuthorityModel> getAllAuthorities() {
        return userUtilService.getAllAuthorities();
    }

    public List<AuthorityModel> getAllUIAuthorities() {
        return getAllAuthorities().stream()
                .filter(au -> WHITE_LISTED_AUTHORITIES_UI.contains(au.getAuthority().toUpperCase()))
                .sorted(Comparator.comparing(AuthorityModel::getAuthority))
                .collect(Collectors.toList());
    }

    public User pinUpdate(Long userId, PinUpdate pinUpdate) throws Exception {
        User user = getUserById(userId);
        var oldObject = copy(user, User.class);
        var pinUpdateService = getUpdateService(user);
        user = pinUpdateService.updateOrCreatePin(user, pinUpdate);

        var savedEntity = userRepository.save(user);
        auditEvent.publish(oldObject, savedEntity, USER_PIN_UPDATE, USER);

        emailService.sendPinNotification(user);
        return savedEntity;
    }

    public User enableUser(Long userId) throws ValuePlusException {
        User user = getUserById(userId);
        if (user.isEnabled()) {
            throw new ValuePlusException("User is currently enabled", BAD_REQUEST);
        }

        var oldObject = copy(user, User.class);
        user.setEnabled(true);
        var savedEntity = userRepository.save(user);

        auditEvent.publish(oldObject, savedEntity, USER_ENABLE, USER);
        return savedEntity;
    }

    public User disableUser(Long userId) throws ValuePlusException {
        User user = getUserById(userId);
        if (!user.isEnabled()) {
            throw new ValuePlusException("User is currently disabled", BAD_REQUEST);
        }

        var oldObject = copy(user, User.class);
        user.setEnabled(false);
        var savedEntity = userRepository.save(user);

        auditEvent.publish(oldObject, savedEntity, USER_DISABLE, USER);
        return savedEntity;
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found"));
    }

    public void deleteUser(Long userId) {
        userRepository.deleteUser(userId);    }

    public Optional<User> getAdminUserAccount() {
        return userRepository.findByEmailAndDeletedFalse(ADMIN_ACCOUNT);
    }

    public Long getAdminUserId() {
        return getAdminUserAccount().map(User::getId).orElse(0L);
    }

    public Page<User> findAllAgentsUnderAdmin(Long roleId, Pageable pageable) throws ValuePlusException{
        return userRepository.findAllByRoleId(roleId, pageable);
    }

    private PinUpdateService getUpdateService(User user) {
        return emptyIfNullStream(pinUpdateServiceList)
                .filter(p -> p.useStrategy(user))
                .findFirst()
                .orElseThrow(() -> new ValuePlusRuntimeException("Error retrieving implementation for PinUpdate"));
    }

    public Page<AgentDto> searchUsers(UserSearchFilter searchFilter, Pageable pageable) throws ValuePlusException {
        UserSpecification specification = buildSpecification(searchFilter);
        return userRepository.findAll(Specification.where(specification), pageable)
                .map(u -> AgentDto.valueOf(u, productUrlProvider()));
    }

    public Map<ProductProvider, ProductProviderUrlService> productUrlProvider() {
        return userUtilService.productUrlProvider();
    }

    private UserSpecification buildSpecification(UserSearchFilter searchFilter) throws ValuePlusException {
        UserSpecification specification = new UserSpecification();
        if (searchFilter.getFirstname() != null) {
            specification.add(new SearchCriteria<>("firstname", searchFilter.getFirstname(), SearchOperation.MATCH));
        }
        if (searchFilter.getLastname() != null) {
            specification.add(new SearchCriteria<>("lastname", searchFilter.getLastname(), SearchOperation.MATCH));
        }

        if (searchFilter.getRoleType() != null) {
            Role role = roleRepository.findByName(searchFilter.getRoleType().name()).get();
            specification.add(new SearchCriteria<>("role", role, SearchOperation.EQUAL));
        }
        if (searchFilter.getEmail() != null) {
            specification.add(new SearchCriteria<>("email", searchFilter.getEmail(), SearchOperation.EQUAL));
        }
        if (searchFilter.getSuperAgentCode() != null) {
            User superAgent = userRepository.findByReferralCode(searchFilter.getSuperAgentCode())
                    .orElseThrow(() -> new ValuePlusException("Invalid super Agent code ", BAD_REQUEST));
            specification.add(new SearchCriteria<>("superAgent", superAgent, SearchOperation.EQUAL));
        }

        return specification;
    }

   public MessageResponse suspendAgent(Long userId) throws ValuePlusException {

       MessageResponse messageResponse = new MessageResponse();
       Optional<User> agent = userRepository.findById(userId);
       if(agent.isEmpty()) throw new ValuePlusException("Incorrect parameter; user with id, " + userId + " does not exist");
       User retrievedAgent = agent.get();
       if(!retrievedAgent.isEnabled())
          throw new ValuePlusException("Already Suspended");


       retrievedAgent.setEnabled(false);

       if(userRepository.save(retrievedAgent) != null)
           messageResponse.setMessage("Suspended!!");
       return messageResponse;

    }

    public MessageResponse suspendSubAdmin(Long userId) throws ValuePlusException {

        MessageResponse messageResponse = new MessageResponse();
        Optional<User> agent = userRepository.findById(userId);
        if(agent.isEmpty()) throw new ValuePlusException("Incorrect parameter; user with id, " + userId + " does not exist");
        User retrievedAgent = agent.get();
        if(!retrievedAgent.isEnabled())
            throw new ValuePlusException("Already Suspended");


        retrievedAgent.setEnabled(false);

        if(userRepository.save(retrievedAgent) != null)
            messageResponse.setMessage("Suspended!!");
        return messageResponse;

    }

    public MessageResponse enableSubAdmin(Long userId) throws ValuePlusException {

        MessageResponse messageResponse = new MessageResponse();
        Optional<User> agent = userRepository.findById(userId);
        if(agent.isEmpty()) throw new ValuePlusException("Incorrect parameter; user with id, " + userId + " does not exist");
        User retrievedAgent = agent.get();
        if(retrievedAgent.isEnabled())
            throw new ValuePlusException("Already Enabled");


        retrievedAgent.setEnabled(true);

        if(userRepository.save(retrievedAgent) != null)
            messageResponse.setMessage("Enabled!!");
        return messageResponse;

    }


    public MessageResponse deleteSuperAgent(Long userId) throws ValuePlusException{
        MessageResponse messageResponse = new MessageResponse();
        try {
            if(userRepository.deleteSuperAgent(userId) < 1)
                throw  new ValuePlusException("User not found");
            messageResponse.setMessage("User Successfully Deleted!");

        }catch (ValuePlusException exception){
            messageResponse.setMessage("Something ent wrong "+ exception.getMessage());
            log.error("Something went wrong: "+ exception.getMessage());
        }catch (Exception exe){
            log.error("Something went wrong: "+ exe.getMessage());
        }
        return messageResponse;

    }
    public ActiveUsers findActiveAgentsUnderSuperAgent(User user)
    {   MessageResponse messageResponse = new MessageResponse();
        Integer number = 0;
        List<User> agents = userRepository.findUsersBySuperAgent(user);
        log.info(String.valueOf(agents.size()));
        System.out.println("this point");
        ActiveUsers activeUsers = new ActiveUsers();
        activeUsers.setUsers(agents.size());

            for (User userList:agents) {
                Boolean status = userRepository.findActiveVpAgent(userList.getId());
                if (status) {number++;}
            }
        activeUsers.setActiveUsers(number);
            activeUsers.setSuperAgentName(user.getFirstname().concat(user.getLastname()));

        return activeUsers;
    }

//    public ActiveUsers vPAgentSummary(User user)
//    {   MessageResponse messageResponse = new MessageResponse();
//        Integer number = 0;
//        List<User> agents = userRepository.findUsersByAgent(user);
//        log.info(String.valueOf(agents.size()));
//        System.out.println("this point");
//        ActiveUsers activeUsers = new ActiveUsers();
//        activeUsers.setUsers(agents.size());
//
//        for (User userList:agents) {
//            Boolean status = userRepository.findActiveVpAgent(userList.getId());
//            if (status) {number++;}
//            getActiveSuperAgentReferral(userList);
//        }
//        activeUsers.setActiveUsers(number);
//        activeUsers.setSuperAgentName(user.getFirstname().concat(user.getLastname()));
//
//        return activeUsers;
//    }

    public ActiveUsers findActiveAgentsUnderSuperAgent(Long id)
    {
       log.info("inside service method");
        int number = 0;
        User user = this.userRepository.findById(id).orElseThrow(() -> new NotFoundException("user not found"));
        log.info("after user confirmed");
        List<User> agents = userRepository.findUsersBySuperAgent(user);
        log.info("normal agents under super agent");
        log.info(String.valueOf(agents.size()));
        System.out.println("this point");
        ActiveUsers activeUsers = new ActiveUsers();
        activeUsers.setUsers(agents.size());
        log.info(String.valueOf(agents.size()));
        for (User userList:agents) {
            log.info("inside foreach");
            Boolean status = userRepository.findActiveVpAgent(userList.getId());
            log.info("after query");
            log.info(status.toString());
            if (status) {number++;}
        }
        activeUsers.setActiveUsers(number);
        activeUsers.setSuperAgentName(user.getFirstname().concat(user.getLastname()));
        return activeUsers;

    }

    public MessageResponse deleteSubAdmin(Long userId) throws ValuePlusException{
        MessageResponse messageResponse = new MessageResponse();
        try {
            if(userRepository.deleteSuperAgent(userId) < 1)
                throw  new ValuePlusException("User not found");
            messageResponse.setMessage("User Successfully Deleted!");

        }catch (ValuePlusException exception){
            messageResponse.setMessage("Something ent wrong "+ exception.getMessage());
            log.error("Something went wrong: "+ exception.getMessage());
        }catch (Exception exe){
            log.error("Something went wrong: "+ exe.getMessage());
        }
        return messageResponse;

    }

    private void checkUserRole(RoleType role, User user){
        Role userRole = roleRepository.findByName(role.name())
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role is not found."));
        Role role1 = user.getRole();

        if(role1 != userRole){
            throw new BadRequestException("You don't have access to this link");
        }

    }

    public SuperAgentSummary getSuperAgentRefferalSummary(Long superAgentId){
        User user = getUserById(superAgentId);
        if (!isSuperAgent(user)) {
            throw new ValuePlusRuntimeException("Invalid credentials");
        }
        List<User> users = getSuperAgentReferrals(user);
        return SuperAgentSummary.builder()
                .superAgentName(user.getFirstname().concat(" ").concat(user.getLastname()))
                .totalAgent(users.size())
                .totalActiveAgent(getActiveSuperAgentReferral(users))
                .build();
    }

    public SuperAgentSummary getAgentRefferalSummary(Long agentId){

        User user = getUserById(agentId);
        if (!isAgent(user)) {
            throw new ValuePlusRuntimeException("Invalid credentials");
        }
        List<User> referrals = getAgentReferrals(user);
        return SuperAgentSummary.builder()
                .superAgentName(user.getFirstname().concat(" ").concat(user.getLastname()))
                .totalAgent( getActiveValuePlusReferralCount(user) +  getBetwayReferralCount(user)+ getBetacareReferralCount(user))
                .totalActiveAgent(getActiveSuperAgentReferral(referrals))
                .build();}



    public Page<User.AgentData> getAgentsUnderSuperAgentSummary  (Long superAgentId, Pageable pageable){
        User user = getUserById(superAgentId);
            Page<User.AgentData> data = userRepository.findAllBySuperAgent(user,pageable);
         return data;
    }
    private int getSuperAgentTotalBetwayReferrals(List<User> users){
        int number =0;
        for (User user:users) {
            number += hasRefferedBetwayAgents(user);
        }
        return number;
    }

    private int getVpAgentTotalBetwayReferrals(List<User> users){
        int number =0;
        for (User user:users) {
            number += hasRefferedBetwayAgents(user);
        }
        return number;
    }

    public List<User> getSuperAgentReferrals(User user){
        return userRepository.findUsersBySuperAgent(user);
    }

    private List<User> getAgentReferrals(User user){
        return userRepository.findUsersByAgent(user);
    }

    public Boolean hasAgentReferrals(User user ){
     List<User> list = getAgentReferrals(user);
     if (!list.isEmpty())
         return true;
     else return false;
    }

    public Integer getActiveSuperAgentReferral(List<User> users){
        Integer number = 0;
        for (User user:users) {
            boolean betway = hasRefferedActiveBetwayAgents(user);
            boolean betacare =  hasReferredBetacare(user);
            boolean productOrder =  hasPurchasedProduct(user);
//            boolean valueplus = hasAgentReferrals(user);
            if(betway || betacare || productOrder){
                number++;
            }
        }
        return number;
    }
    public Integer getActiveVpAgentReferral(User agent){
        Integer number = 0;
        List<User> referrals = getAgentReferrals(agent);

        for (User user:referrals) {
            boolean betway = hasRefferedActiveBetwayAgents(user);
            boolean betacare =  hasReferredBetacare(user);
            boolean productOrder =  hasPurchasedProduct(user);

            if(betway || betacare || productOrder){
                number++;
            }
        }
        return number;
    }


    public Boolean hasPurchasedProduct(User user) {
        boolean flag = false;
        Integer count = productOrderRepository.countByUserAndStatus(user , OrderStatus.COMPLETED);
        if(count > 0){
            flag = true;
        }
        return flag;
    }

    private boolean hasReferredBetacare(User user) {
        boolean flag = false;
        var product = productProviderUserRepository.findByUserIdAndProvider(user.getId(),ProductProvider.BETA_CARE);
        if(product.isPresent()){
            Optional<ReferralCounter> referralCounter = referralCounterRepository.findByReferralCode(product.get().getAgentCode());
            if(referralCounter.isPresent()){
                if(referralCounter.get().getCount() > 0){
                    flag = true;
                }
            }
        }

        return flag;
    }

    private boolean hasRefferedActiveBetwayAgents(User user) {
        boolean flag = false;
        var agentadata = betWayAgentDataRepository.findByUser(user);
        if(agentadata.isPresent()){
            if(agentadata.get().getActiveReferrals() > 0){
                flag = true;
            }
        }
        return flag;
    }
    private boolean hasMonthlyActiveBetwayAgents(User user){
        LocalDateTime initial = LocalDateTime.now();
        LocalDateTime start = initial.with(firstDayOfMonth());
        LocalDateTime end = initial.with(lastDayOfMonth());
        boolean flag = false;
        var agentadata = betWayAgentDataRepository.findByUser(user);
        if(agentadata.isPresent()){
            if(agentadata.get().getActiveReferrals() > 0 && agentadata.get().getUpdatedAt().compareTo(start) > 0 ){
                flag = true;
            }
        }
        return flag;
    }
    private boolean hasPurchasedProductInAMonth(User user){
        LocalDateTime initial = LocalDateTime.now();
        LocalDateTime start = initial.with(firstDayOfMonth());
        LocalDateTime end = initial.with(lastDayOfMonth());
        boolean flag = false;
        Integer count = productOrderRepository.countByUserAndStatusAndCreatedAtBetween(user, OrderStatus.COMPLETED,start,end);
        if(count > 0){
            flag = true;
        }
        return flag;
    }

    private int getActiveAgentsLastMonth(){
        int activeAgent =0;
        List<User> agents = userRepository.findUserByRole_Name("Agent");
        for (User agent:agents) {
            boolean betway = hasMonthlyActiveBetwayAgents(agent);
            boolean productOrder = hasPurchasedProductInAMonth(agent);

            if (betway|| productOrder){
                activeAgent ++;
            }
        }
        return activeAgent;
    }

    private int getCompletedOrders(){
    List<ProductOrder> completedOrders =  productOrderRepository.findByStatus(COMPLETED);
    return completedOrders.size();
    }

    private int getFailedOrders(){
        List<ProductOrder> completedOrders =  productOrderRepository.findByStatus(FAILED);
        List<ProductOrder> completedOrders2 =  productOrderRepository.findByStatus(CANCELLED);
        return completedOrders.size() + completedOrders2.size();
    }

    public int getTotalSuccessfulWithdrawal(){
        List<Transaction> transactions = transactionRepository.findSuccessfulTransactions();
        return transactions.size();
    }

    public int getTotalInactiveUsers(){
        return userRepository.countUserByActivatedFalse();
    }

    public int getTotalActiveUsers(){
        return userRepository.countUserByActivatedTrue();
    }










    @Scheduled(cron = "0 0 */3 * * ?")
    private void activeAgentsChecker(){
        Optional<Role> role = roleRepository.findByName("AGENT");
        Role role1 = role.get();
        System.out.println(role1.getName());
      List<User> inActiveAgents = userRepository.findUsersByActivatedFalseAndRole(role1);
        System.out.println(inActiveAgents.size());
        for (User user:inActiveAgents)
        { boolean a,b,c;
          a=  hasReferredBetacare(user);
           b = hasRefferedActiveBetwayAgents(user);
            c =hasPurchasedProduct(user);
            if (a||b||c){
                user.setActivated(true);
                userRepository.save(user);
            }
        }
    }
    private int hasRefferedBetwayAgents(User user) {
        int number  = 0;
        var agentadata = betWayAgentDataRepository.findByUser(user);
        if(agentadata.isPresent()){
            number = agentadata.get().getTotalReferrals();
        }
        return number;
    }

    public AgentData getIndividualAgentData(Long agentId){
        User user = userRepository.findById(agentId).orElseThrow(() -> new ResourceNotFoundException("Error: Agent is not found."));
       BetWayAgentData betWayAgentData = betWayAgentDataRepository.findByUser(user).orElseThrow(
                () -> new BadRequestException("user has not registerd for Betway promotion")
        );
       ProductSummary productSummary = defaultSummaryService.calculateProductSummary(productOrderRepository.findByUser_idAndStatus(user.getId(), COMPLETED));
        TransactionSummary transactionSummary = defaultSummaryService.calculateTransactionSummary(transactionRepository.findAllByUser_Id(user.getId()));
      Optional<DigitalProductCommission> commission =  commissionRepository.findByProductProvider(ProductProvider.BETA_CARE);
        return AgentData.builder().
                agentName(user.getFirstname().concat(" ").concat(user.getLastname()))
                        .address(user.getAddress())
                        .phoneNo(user.getPhone())
                                .agentEmail(user.getEmail())
                                        .address(user.getAddress())
                                                .enabled(user.isEnabled())
                                                        .kycVerified(user.isKycVerification())
                                                                .referredAgent(getValuePlusReferralCount(user))
                                                                .dateCreated(user.getCreatedAt())
                .betwayTotalEarning(betWayAgentData.getTotalEarning())
                .betwayActiveReferrals(betWayAgentData.getActiveReferrals())
                .betwayAgentReferralCode(betWayAgentData.getAgentReferralCode())
                .betwayTotalReferrals(betWayAgentData.getTotalReferrals())
                .betaCareTotalReferrals(getBetacareReferralCount(user))
                .betaCareActiveReferrals(0)
                .betaCareTotalEarning(BigDecimal.ZERO.multiply(commission.get().getCommissionPercentage()))
                .totalApprovedWithdrawals(transactionSummary.getTotalApprovedWithdrawals())
                .totalPendingWithdrawal(transactionSummary.getTotalPendingWithdrawal())
                .pendingWithdrawalCount(transactionSummary.getPendingWithdrawalCount())
                .totalProductSales(productSummary.getTotalProductSales())
                .totalProductAgentProfits(productSummary.getTotalProductAgentProfits())
                .build();
    }

    public Integer getBetacareReferralCount(User user){
        Integer betaCareAgentCount =0;
        Optional <ProductProviderUser> productProvider = productProviderUserRepository.findByUserIdAndProvider(user.getId(),ProductProvider.BETA_CARE);
        if (productProvider.isPresent()) { Optional<ReferralCounter> referralCounter = referralCounterRepository.findByReferralCode(productProvider.get().getAgentCode());
            if (referralCounter.isPresent()) {betaCareAgentCount = referralCounter.get().getCount();}}
        return betaCareAgentCount;
    }
    public Boolean hasActiveVpReferrals(User user){
        Boolean status = false;
    Integer count  =  getActiveValuePlusReferralCount(user);
    if (count>0){
        status = true;
    }
    return status;
    }

    public Integer getActiveValuePlusReferralCount(User user){
        Integer vpAgentCount =0;
        log.info(user.getReferralCode());
        Optional <ReferralCounter> vpAgentreferralCounter = referralCounterRepository.findByReferralCode(user.getAgentCode());
        if (vpAgentreferralCounter.isPresent()){vpAgentCount =vpAgentreferralCounter.get().getCount();}
        return vpAgentCount;
    }

    public Integer getBetwayReferralCount(User user){
        Integer betWayAgentCount =0;
        Optional <BetWayAgentData> betWayAgentData = betWayAgentDataRepository.findByUser(user);
        if (betWayAgentData.isPresent()){betWayAgentCount = betWayAgentData.get().getTotalReferrals();}
        return betWayAgentCount;
    }

    public Integer getValuePlusReferralCount(User user){
        List<User> vpAgents= getAgentReferrals(user);
      Integer vpAgentCount =vpAgents.size();
      return vpAgentCount;
    }

    public Integer getTotalDigitalProductsReferralCount(User user){
      Integer vp =  getValuePlusReferralCount(user);
//      log.info(vp.toString().concat(" vp"));
      Integer bt = getBetacareReferralCount(user);
//      log.info(bt.toString().concat("beta care"));
      Integer betway = getBetwayReferralCount(user);
//      log.info(betway.toString().concat("betway "));
      Integer sum = vp+bt+betway;
      return sum;
    }

    public Page<User> getAgentsReferral(Long userId,Pageable pageable){
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Error: Agent is not found."));
        Page<User> users = userRepository.findAllByAgent(user,pageable);
        return users;
    }
    public Page<User.subAgentData> getAgentsUnderAgentSummary  (Long agentId, Pageable pageable){
        User user = getUserById(agentId);
        Page<User.subAgentData> data = userRepository.findUsersByAgent(user,pageable);
        return data;
    }

    public Page<User> searchAgentsUnderSuperAgents( String searchString, Pageable pageable){
     User  superAgent = UserUtils.getLoggedInUser();
     searchString = searchString.toLowerCase();
     log.info(superAgent.getAgentCode());
     log.info(searchString);
       return userRepository.searchForAgentUnderSuperAgents(superAgent.getAgentCode(),searchString, pageable);
        
    }

    private UserSpecification buildUsersSpecification(String city,String active, User user) {
        UserSpecification specification = new UserSpecification();

        Boolean enable = null;
        if (active.equals("false") ){enable = false;}
        if (active.equals("true") ){enable = true; }
        if (!city.isBlank()) {
            specification.add(new SearchCriteria<>("city", city, SearchOperation.MATCH));
        }

        if (!active.isBlank()) {
            specification.add(new SearchCriteria<>("activated", enable, SearchOperation.EQUAL));
        }

        specification.add(new SearchCriteria<>("superAgent",user,SearchOperation.EQUAL));
        return specification;
    }
    private UserSpecification buildUsersSpecificationTwo(String state,String active, User user) {
        UserSpecification specificationT = new UserSpecification();
        State foundState = null;
        Boolean enableTwo = null;
        if (!state.isBlank()){
        for (State states : State.values()) {
          if (states.name().equals(state.toUpperCase())){
                foundState = states;
              System.out.println(foundState);
            }
        }}
        if (active.equals("false") ){enableTwo = false;}
        if (active.equals("true") ){enableTwo = true;}
        if (!state.isBlank()) {
            specificationT.add(new SearchCriteria<>("state", foundState, SearchOperation.EQUAL));
        }
        if (!active.isBlank()) {
            specificationT.add(new SearchCriteria<>("activated", enableTwo, SearchOperation.EQUAL));
        }
        specificationT.add(new SearchCriteria<>("superAgent",user,SearchOperation.EQUAL));
        return  specificationT;
    }

    public Page<User> filterUser(String city, String activated, Pageable pageable) {
        User superAgent = getLoggedInUser();
        if (city.isBlank() && activated.isBlank())
        {
       return userRepository.findAllBySuperAgentAndDeletedFalse(superAgent,pageable);
        }
        UserSpecification specification = buildUsersSpecification(city, activated,superAgent);
        UserSpecification specification1 = buildUsersSpecificationTwo(city,activated,superAgent);

        Page<User> userModels = userRepository.findAll(Specification.where(specification).or(specification1), pageable);


        return userModels;
    }

    public Page<User> filterActiveAgentsUnderSuperAgent(Boolean state, Pageable pageable) {
        User superAgent = getLoggedInUser();
        List<User>activeList = new ArrayList<>();
        Page<User> allAgents = userRepository.findAllBySuperAgentAndDeletedFalse(superAgent,pageable);
        List<User> list = allAgents.toList();
        if (state){
        for (User activeAgent:list) {

          Integer active =  getActiveVpAgentReferral(activeAgent);
          if (active >0){
          activeList.add(activeAgent);
          }
        }
        Page<User> pages = new PageImpl<User>(activeList, pageable, activeList.size());

        return pages;}
        else return allAgents;
    }


}
