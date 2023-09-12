package com.valueplus.domain.service.concretes;

import com.valueplus.app.config.audit.AuditEventPublisher;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.domain.enums.SocialMedia;
import com.valueplus.domain.enums.TransactionType;
import com.valueplus.domain.model.*;
import com.valueplus.domain.products.ProductProviderUrlService;
import com.valueplus.domain.products.RegisterProductProvider;
import com.valueplus.domain.products.ValuePlusProductProvider;
import com.valueplus.domain.service.abstracts.WalletService;
import com.valueplus.persistence.entity.*;
import com.valueplus.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.valueplus.domain.enums.ActionType.*;
import static com.valueplus.domain.enums.EntityType.USER;
import static com.valueplus.domain.model.RoleType.AGENT;
import static com.valueplus.domain.model.RoleType.SUPER_AGENT;
import static com.valueplus.domain.util.GeneratorUtils.generateRandomString;
import static com.valueplus.domain.util.UserUtils.getLoggedInUser;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

@RequiredArgsConstructor
@Slf4j
@Service
public class RegistrationService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletHistoryRepository walletHistoryRepository;
    private final ValuePlusProductProvider vpProductProvider;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final EmailVerificationService emailVerificationService;
    private final WalletService walletService;
    private final UserUtilService userUtilService;
    private final AuditEventPublisher auditEvent;
    private List<ProductProviderUser> productProviderUsers = new ArrayList<>();
    private final RegisterProductProvider productProvider;
    private final RegisterProductProvider registerProductProvider;
    private final ReferralCounterRepository referralCounterRepository;
    private final ProductProviderUserRepository productProviderUserRepository;
    private final ValuePlusProductProvider vpproductProvider;
    private final UsersSocialMediaReportRepository socialMediaReportRepository;

    @Transactional
    public User createAgent(AgentCreate agentCreate) throws Exception {
        try {
            ensureUserIsUnique(agentCreate.getEmail().toLowerCase());
            User user = userRepository.save(User.from(agentCreate)
                    .role(getRole(AGENT))
                    .password(passwordEncoder.encode(agentCreate.getPassword()))
                    .enabled(false)
                    .build());

            var superAgent = Optional.ofNullable(agentCreate.getSuperAgentCode())
                    .flatMap(userRepository::findByReferralCode);

            superAgent.ifPresent(user::setSuperAgent);

            //Register
            if (!agentCreate.getSuperAgentCode().isBlank() && agentCreate.getSuperAgentCode().startsWith("VP")){
                log.info(agentCreate.getSuperAgentCode());
                //registerUnderVpAgent(agentCreate.getSuperAgentCode(),user);
                var vpAgent = Optional.ofNullable(agentCreate.getSuperAgentCode())
                        .flatMap(userRepository::findByAgentCodeAndDeletedFalse);
                vpAgent.ifPresent(user::setAgent);}

//            userRepository.save(user);

            // betaCare, data4me registration and betWay registration
            productProvider.registerWithProductProvidersAsync(agentCreate, user, productProviderUsers);
            //vpProductProvider.create(user);

            user.setProductProviders(productProviderUsers);
            if (user.getAgentCode() == null || user.getReferralCode() == null){
                vpproductProvider.createIfFailss(user);
            }

            User savedUser = userRepository.save(user);

//            //update the SocialMediaReportTable

            updateSocialMedia(agentCreate);



            try{
                walletService.createWallet(savedUser);
                emailVerificationService.sendVerifyEmail(user);
                auditEvent.publish(new Object(), savedUser, USER_CREATE_AGENT, USER);
            }catch (Exception e){
                log.error("error", e);
            }

            return savedUser;
        } catch (Exception e) {
            log.error("error", e);
            throw e;
        }
    }

    public MessageResponse registerBetWay() throws ValuePlusException {
        var result = registerProductProvider.registerUserWithBetWaySync();

        if(result.equals("Error registering user, try again")){
            throw new ValuePlusException("Error registering user, try again", NOT_IMPLEMENTED);
        }

        return new MessageResponse(result);
    }

//    public MessageResponse registerData4me() throws ValuePlusException {
//        var result = registerProductProvider.registerUserData4meSync();
//
//        if(result.equals("Error registering user, try again")){
//            throw new ValuePlusException("Error registering user, try again", NOT_IMPLEMENTED);
//        }
//
//        return new MessageResponse(result);
//    }

    public MessageResponse registerBetaCare() throws ValuePlusException {
        var result = registerProductProvider.registerUserWithBetaCareSync();

        if(result.equals("Error registering user, try again")){
            throw new ValuePlusException("Error registering user, try again,", NOT_IMPLEMENTED);
        }

        return new MessageResponse(result);
    }

    public MessageResponse registerValuePlus(User user)throws ValuePlusException{
        var result = registerProductProvider.registerUserWithValuePlusSync(user);

        if(result.equals("Error registering user, try again")){
            throw new ValuePlusException("Error registering user, try again,", NOT_IMPLEMENTED);
        }

        return new MessageResponse(result);
    }

    public User createAdmin(UserCreate userCreate, RoleType roleType) throws Exception {
        ensureAuthorityIsPresent(userCreate);
        ensureUserIsUnique(userCreate.getEmail());
        List<Authority> authorities = userUtilService.getAdminAuthority(userCreate.getAuthorityIds());

        String password = generateRandomString(10);
        User user = newUserWithGeneratedPassword(userCreate, roleType, password);
        user.setAuthorities(authorities);

        user = userRepository.save(user);
        walletService.createWallet(user);
        emailVerificationService.sendAdminAccountCreationNotification(user, password);
        auditEvent.publish(new Object(), user, USER_CREATE_ADMIN, USER);
        return user;
    }
    public User createSubAdmin(UserCreate userCreate, RoleType roleType) throws Exception {
        ensureAuthorityIsPresent(userCreate);
        ensureUserIsUnique(userCreate.getEmail());
        List<Authority> authorities = userUtilService.getAdminAuthority(userCreate.getAuthorityIds());

        String password = generateRandomString(10);
        User user = newUserWithGeneratedPassword(userCreate, roleType, password);
        user.setAuthorities(authorities);

       User savedUser = userRepository.save(user);

        walletService.createWallet(user);
        emailVerificationService.sendSubAdminAccountCreationNotification(user, password);
        auditEvent.publish(new Object(), savedUser, USER_CREATE_SUB_ADMIN, USER);
        return user;
    }

    private void ensureAuthorityIsPresent(UserCreate userCreate) throws ValuePlusException {
        if (userCreate.getAuthorityIds() == null || userCreate.getAuthorityIds().size() == 0) {
            throw new ValuePlusException("Authority is required for an admin user", BAD_REQUEST);
        }
    }

    public User createSuperAgent(UserCreate userCreate) throws Exception {
        ensureUserIsUnique(userCreate.getEmail());

        String password = generateRandomString(10);
        String referralCode = generateRandomString(8);

        User user = newUserWithGeneratedPassword(userCreate, SUPER_AGENT, password);
        user.setReferralCode(referralCode.toLowerCase());

        user = userRepository.save(user);
        walletService.createWallet(user);
        emailVerificationService.sendSuperAgentAccountCreationNotification(user, password);
        auditEvent.publish(new Object(), user, USER_CREATE_SUPER_AGENT, USER);
        return user;
    }

    public User createAgentBySuperAgent(UserCreate userCreate) throws Exception {
        try {
            User superAgent = getLoggedInUser();
            String password = generateRandomString(10);
            ensureUserIsUnique(userCreate.getEmail().toLowerCase());
            User user = userRepository.save(User.from(userCreate)
                    .role(getRole(AGENT))
                    .password(passwordEncoder.encode(password))
                    .enabled(false)
                    .build());

            user.setSuperAgent(superAgent);
            AgentCreate agentCreate = new AgentCreate();
            agentCreate.setAddress(userCreate.getAddress());
            agentCreate.setEmail(userCreate.getEmail());
            agentCreate.setFirstname(userCreate.getFirstname());
            agentCreate.setLastname(userCreate.getLastname());
            agentCreate.setPhone(userCreate.getPhone());
            agentCreate.setCity(userCreate.getCity());
            agentCreate.setState(userCreate.getState());
            agentCreate.setAuthorityIds(userCreate.getAuthorityIds());
            agentCreate.builder().superAgentCode(superAgent.getAgentCode())
                    .password(password).build();




//            userRepository.save(user);

            // betaCare, data4me registration and betWay registration
            productProvider.registerWithProductProvidersAsync(agentCreate, user, productProviderUsers);
//            vpProductProvider.create(user);

            user.setProductProviders(productProviderUsers);
            User savedUser = userRepository.save(user);

            try{
                walletService.createWallet(savedUser);
                emailVerificationService.sendVerifyEmailForAgentBySuperAgent(user,password);
                auditEvent.publish(new Object(), savedUser, SUPER_AGENT_CREATE_AGENT, USER);
            }catch (Exception e){
                log.error("error", e);
            }

            return savedUser;
        } catch (Exception e) {
            log.error("error", e);
            throw e;
        }
    }

    public Map<ProductProvider, ProductProviderUrlService> productUrlProvider() {
        return userUtilService.productUrlProvider();
    }

    private User newUserWithGeneratedPassword(UserCreate userCreate, RoleType roleType, String password) {
        return userRepository.save(User.from(userCreate)
                .role(getRole(roleType))
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .emailVerified(true)
                .superAgent(null)
                .build());
    }

    private void ensureUserIsUnique(String email) throws ValuePlusException {
        if (userRepository.findByEmailAndDeletedFalse(email)
                .isPresent()) {
            throw new ValuePlusException("User profile exists", HttpStatus.BAD_REQUEST);
        }
    }

    private Role getRole(RoleType roleType) {
        Optional<Role> optionalRole = roleRepository.findByName(roleType.name());
        return optionalRole
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(roleType.name())
                        .build()));
    }

    private void registerUnderVpAgent(String vpAgentCode,User agent){
        Optional<Role> role = this.roleRepository.findByName("AGENT");
        Optional <User> user = this.userRepository.findByAgentCodeAndDeletedFalseAndRole(vpAgentCode,role.get());
        if (user.isPresent()){
            updateCommission(user.get());
        }
    }

    @Transactional
    public List<String> integrateAgent(AgentCreate agentCreate) throws Exception {
        try {
            ensureUserIsUnique(agentCreate.getEmail().toLowerCase());

            User user = userRepository.save(User.from(agentCreate)
                    .role(getRole(AGENT))
                    .password(passwordEncoder.encode(agentCreate.getPassword()))
                    .enabled(false)
                    .build());

            var superAgent = Optional.ofNullable(agentCreate.getSuperAgentCode())
                    .flatMap(userRepository::findByReferralCode);

            superAgent.ifPresent(user::setSuperAgent);

            userRepository.save(user);

            // betaCare, data4me registration and betWay registration
            productProvider.registerWithProductProviders(agentCreate, user, productProviderUsers);

            user.setProductProviders(productProviderUsers);
            User savedUser = userRepository.save(user);

            try {
                walletService.createWallet(savedUser);
                auditEvent.publish(new Object(), savedUser, USER_CREATE_AGENT, USER);
            } catch (Exception e) {
                log.error("error", e);
            }

            List<ProductProviderUser> productProviderUsers = productProviderUserRepository.findByUserId(savedUser.getId());
            return productProviderUsers.stream()
                    .map(productProviderUser -> productProviderUser.getAgentUrl())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("error", e);
            throw e;
        }
    }
    public void updateCommission(User referralAgent){
        if (referralAgent.getAgent()!=null){
            User commissionedUser = referralAgent.getAgent();
            createVpReferralCommission(referralAgent,commissionedUser);
            updateReferralCount(commissionedUser);
        }
    }

    private void createVpReferralCommission(User referralAgent, User commissionedUser){
        Optional<Wallet> wallet = this.walletRepository.findByUser(commissionedUser);
        BigDecimal vpComission = BigDecimal.valueOf(240);
        wallet.get().setAmount(wallet.get().getAmount().add(vpComission));
        walletRepository.save(wallet.get());
        WalletHistory walletHistory = new WalletHistory();
        walletHistory.setWallet(wallet.get());
        walletHistory.setAmount(vpComission);
        walletHistory.setDescription("ValuePlus Commission for "+referralAgent.getFirstname()+" ".concat(referralAgent.getLastname()));
        walletHistory.setType(TransactionType.CREDIT);
        walletHistoryRepository.save(walletHistory);
    }

    private void updateReferralCount(User user) {
        Optional<ReferralCounter> referralCounter = this.referralCounterRepository.findByReferralCode(user.getAgentCode());
        referralCounter.get().setCount(referralCounter.get().getCount() + 1);
        referralCounterRepository.save(referralCounter.get());
    }

    public void updateSocialMedia(AgentCreate agentCreate){

        SocialMedia socialMedia = SocialMedia.valueOf(agentCreate.getPlatform().toUpperCase());

        Optional<UsersSocialMediaReport> optionalMediaReport = Optional.ofNullable(socialMediaReportRepository.findBySocialMedia(socialMedia));
        if(optionalMediaReport.isPresent()) {
            UsersSocialMediaReport mediaReport = optionalMediaReport.get();
            mediaReport.setNumberOfUsers(mediaReport.getNumberOfUsers() + 1L);
            socialMediaReportRepository.save(mediaReport);
        }
        else{
            UsersSocialMediaReport mediaReport = UsersSocialMediaReport.builder()
                    .socialMedia(socialMedia)
                    .numberOfUsers(1L)
                    .build();
            socialMediaReportRepository.save(mediaReport);
        }

    }

}
