package com.valueplus.domain.products;

import com.valueplus.betway.BetWayAgentData;
import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.domain.model.AgentCreate;
import com.valueplus.domain.service.concretes.BetWayRequest;
import com.valueplus.domain.service.concretes.BetWayResponse;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.persistence.entity.ProductProviderUser;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.BetWayAgentDataRepository;
import com.valueplus.persistence.repository.ProductProviderUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import static com.valueplus.domain.util.FunctionUtil.emptyIfNullStream;
@Slf4j
@RequiredArgsConstructor
@Service
public class RegisterProductProvider {

    private final List<ProductProviderService> providerServices;
    private final ProductProviderUserRepository providerRepository;
    private  final BetWayAgentDataRepository betWayAgentDataRepository;
    private final ValuePlusProductProvider productProvider;

    public ProductProviderUser registerUserWithBetWay(User user){

        try{
            BetWayRequest betWayRequest = new BetWayRequest();
            betWayRequest.setBrandCode("BWN");
            betWayRequest.setTrafficSourceName(user.getId().toString());

            WebClient.Builder builder = WebClient.builder();

            BetWayResponse betWayResponse = builder.build()
                    .post()
                    .uri("https://api.betwaypartnersafrica.com/v1/media/trafficsource/BPA89076")
                    .header("ClientId","value-plus.apps.betwaypartnersafrica.com")
                    .header("ApiKey","5OsirI2titSQgoANSu4KW942skuxTeLhJIrF8XePrCI=")
                    .bodyValue(betWayRequest)
                    .retrieve()
                    .bodyToMono(BetWayResponse.class)
                    .block();

            ProductProviderUser productProviderUser = providerRepository.findByUserIdAndProvider(user.getId(), ProductProvider.BETWAY)
                    .orElse(new ProductProviderUser());

            productProviderUser.setUser(user);
            productProviderUser.setProvider(ProductProvider.BETWAY);
            productProviderUser.setAgentUrl(betWayResponse.getTrackingUrl());
            productProviderUser.setAgentCode(betWayResponse.getTrackingUrl());

            return productProviderUser;
        }catch (Exception e){
            System.err.println(e.getMessage());
            return null;
        }

    }

    public String registerUserWithBetWaySync(){

        User userDetails = UserUtils.getLoggedInUser();
        var betWay = registerUserWithBetWay(userDetails);

        String response;

        if(betWay != null) {
            providerRepository.save(betWay);
            Optional<BetWayAgentData> existingBetwayAgent=  betWayAgentDataRepository.findByAgentReferralCode(betWay.getAgentCode().split("=")[2]);
            if(existingBetwayAgent.isEmpty()){
                BetWayAgentData betWayAgentData = new BetWayAgentData();
                betWayAgentData.setUser(userDetails);
                betWayAgentData.setAgentReferralCode(betWay.getAgentCode().split("=")[2]);
                betWayAgentDataRepository.save(betWayAgentData);
            }
            response = betWay.getAgentUrl();
        }
        else response = "Error registering user, try again";

        return response;
    }

    public String registerUserWithBetaCareSync(){
        String message =  "";

        try{
            User userDetails = UserUtils.getLoggedInUser();

            var result = providerRepository.findByUserIdAndProvider(userDetails.getId(), ProductProvider.BETA_CARE).orElse(null);
            if(result != null) return result.getAgentUrl();

            var productProviderUser = ProductProviderUserModel.from(userDetails);

            ProductProviderUser productUser =  emptyIfNullStream(providerServices)
                    .map(p -> p.provider() == ProductProvider.BETA_CARE ? p.register(productProviderUser): null)
                    .filter(Objects::nonNull)
                    .map(ProductProviderUser::toNewEntity)
                    .map(p->p.setUser(userDetails))
                    .collect(Collectors.toList())
                    .get(0);

            providerRepository.save(productUser);

            message = productProviderUser.getReferralUrl();
        }catch (Exception e){
            System.err.println(e.getMessage());
            message = "Error registering user, try again";
        }

        return message;
    }

    public String registerUserWithValuePlusSync(User userDetails){
        String message =  "";

        try{
            var result = providerRepository.findByUserIdAndProvider(userDetails.getId(), ProductProvider.VALUEPLUS).orElse(null);
            System.out.println(result);
            if(result != null) {
                log.info("iss not null");
                return result.getAgentUrl();}
            message =  productProvider.create(userDetails);

        }catch (Exception e){
            System.err.println(e.getMessage());
            message = "Error registering user, try again";
        }

        return message;
    }

    public String registerUserWithValuePlusS(){
        String message =  "";

        try{
            User userDetails = UserUtils.getLoggedInUser();
            message =  productProvider.create(userDetails);
            var result = providerRepository.findByUserIdAndProvider(userDetails.getId(), ProductProvider.VALUEPLUS).orElse(null);
            System.out.println(result);
            if(result != null) return result.getAgentUrl();

        }catch (Exception e){
            System.err.println(e.getMessage());
            message = "Error registering user, try again";
        }

        return message;
    }


//    public String registerUserData4meSync(){
//        String response = "";
//
//        try{
//            User userDetails = UserUtils.getLoggedInUser();
//
//            var result = providerRepository.findByUserIdAndProvider(userDetails.getId(), ProductProvider.DATA4ME).orElse(null);
//            if(result != null) return result.getAgentUrl();
//
//            var productProviderUser = ProductProviderUserModel.from(userDetails);
//
//            ProductProviderUser productUser =  emptyIfNullStream(providerServices)
//                    .map(p -> p.provider() == ProductProvider.DATA4ME ? p.register(productProviderUser): null)
//                    .filter(Objects::nonNull)
//                    .map(ProductProviderUser::toNewEntity)
//                    .map(p->p.setUser(userDetails))
//                    .collect(Collectors.toList())
//                    .get(0);
//
//            providerRepository.save(productUser);
//
//            response = productProviderUser.getReferralUrl();
//        }catch (Exception e){
//            System.err.println(e.getMessage());
//            response = "Error registering user";
//        }
//
//        return response;
//    }

    @Async
    public void registerWithProductProvidersAsync(AgentCreate agentCreate, User user, List<ProductProviderUser> productProviderUsers) {
      try {
          var productProviderUser = ProductProviderUserModel.from(agentCreate);

        System.out.println(productProviderUser+"   user");
        log.debug("inside product provider anysc");
        List<ProductProviderUser> productProviders =  emptyIfNullStream(providerServices)
                .map(p -> p.register(productProviderUser))
                .filter(p->p.getReferralUrl()!=null)
                .map(ProductProviderUser::toNewEntity)
                .map(p->p.setUser(user))
                .collect(Collectors.toList());

        var betWay = registerUserWithBetWay(user);
        if(betWay != null) {
            productProviders.add(betWay);
            Optional<BetWayAgentData> existingBetwayAgent=  betWayAgentDataRepository.findByAgentReferralCode(betWay.getAgentCode().split("=")[2]);
            if(existingBetwayAgent.isEmpty()){
                BetWayAgentData betWayAgentData = new BetWayAgentData();
                betWayAgentData.setUser(user);
                betWayAgentData.setAgentReferralCode(betWay.getAgentCode().split("=")[2]);
                betWayAgentDataRepository.save(betWayAgentData);
            }
        }
        productProviderUsers.addAll(productProviders);

        providerRepository.saveAll(productProviders);}
       catch (Exception e){System.err.println(e.getMessage());}
    }

    public void registerWithProductProviders(AgentCreate agentCreate, User user, List<ProductProviderUser> productProviderUsers) {
        var productProviderUser = ProductProviderUserModel.from(agentCreate);

        List<ProductProviderUser> productProviders =  emptyIfNullStream(providerServices)
                .map(p -> p.register(productProviderUser))
                .filter(p->p.getReferralUrl()!=null)
                .map(ProductProviderUser::toNewEntity)
                .map(p->p.setUser(user))
                .collect(Collectors.toList());

        var betWay = registerUsersWithBetWay(user);
        if(betWay != null) {
            productProviders.add(betWay);
            Optional<BetWayAgentData> existingBetwayAgent=  betWayAgentDataRepository.findByAgentReferralCode(betWay.getAgentCode().split("=")[2]);
            if(existingBetwayAgent.isEmpty()){
                BetWayAgentData betWayAgentData = new BetWayAgentData();
                betWayAgentData.setUser(user);
                betWayAgentData.setAgentReferralCode(betWay.getAgentCode().split("=")[2]);
                betWayAgentDataRepository.save(betWayAgentData);
            }
        }
        productProviderUsers.addAll(productProviders);

        providerRepository.saveAll(productProviders);
    }

    public ProductProviderUser registerUsersWithBetWay(User user){


        try{
            BetWayRequest betWayRequest = new BetWayRequest();
            betWayRequest.setBrandCode("BWN");
            betWayRequest.setTrafficSourceName(user.getEmail());

            WebClient.Builder builder = WebClient.builder();

            BetWayResponse betWayResponse = builder.build()
                    .post()
                    .uri("https://api.betwaypartnersafrica.com/v1/media/trafficsource/BPA89076")
                    .header("ClientId","value-plus.apps.betwaypartnersafrica.com")
                    .header("ApiKey","5OsirI2titSQgoANSu4KW942skuxTeLhJIrF8XePrCI=")
                    .bodyValue(betWayRequest)
                    .retrieve()
                    .bodyToMono(BetWayResponse.class)
                    .block();

            ProductProviderUser productProviderUser = providerRepository.findByUserIdAndProvider(user.getId(), ProductProvider.BETWAY)
                    .orElse(new ProductProviderUser());

            productProviderUser.setUser(user);
            productProviderUser.setProvider(ProductProvider.BETWAY);
            productProviderUser.setAgentUrl(betWayResponse.getTrackingUrl());
            productProviderUser.setAgentCode(betWayResponse.getTrackingUrl());

            return productProviderUser;
        }catch (Exception e){
            System.err.println(e.getMessage());
            return null;
        }
    }

}
