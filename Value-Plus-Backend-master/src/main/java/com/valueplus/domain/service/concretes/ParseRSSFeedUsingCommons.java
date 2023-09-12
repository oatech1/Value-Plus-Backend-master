package com.valueplus.domain.service.concretes;


import static java.time.temporal.TemporalAdjusters.*;
import com.valueplus.app.exception.BadRequestException;
import com.valueplus.app.exception.NotFoundException;
import com.valueplus.betway.BetWayAgentData;
import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.domain.enums.TransactionType;
import com.valueplus.domain.model.bet9ja.Bet9jaResponse;
import com.valueplus.domain.model.bet9ja.Results;
import com.valueplus.domain.products.Bet9jaService;
import com.valueplus.domain.service.abstracts.WalletHistoryService;
import com.valueplus.persistence.entity.*;
import com.valueplus.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import reactor.netty.http.client.HttpClient;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParseRSSFeedUsingCommons {

        private final CpaReportDataRepository cpaReportDataRepository;

        private final BetWayAgentDataRepository betWayAgentDataRepository;

        private final ProductProviderUserRepository productProviderUserRepository;

        private final WalletRepository walletRepository;

        private final WalletHistoryService walletHistoryService;

        private final DigitalProductCommissionRepository digitalProductCommissionRepository;

        private final RevShareReportDataRepository revShareReportDataRepository;

        private final CurrencyConverterService converterService;

        private final Bet9jaService bet9jaService;

        private final Bet9jaRepository bet9jaRepository;

        private final Bet9jaCpaDataRepository bet9jaCpaDataRepository;

       // private String rateOfTheDay = "425.007658";
    private String rateOfTheDay = "425.007658";


    @Scheduled(cron = "0 0 0 * * *")
    public void convertCurrency(){
        rateOfTheDay = converterService.convertBetwayEarnings("20");
    }

    @Scheduled(cron = "0 0 */3 * * ?")
    public void getCpaReport() throws IOException, ParserConfigurationException, SAXException {

        WebClient.Builder builder = WebClient.builder();
        var betWayResponse = builder
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().followRedirect(true)
                ))
                .build()
                .get()
                .uri("http://affiliatefeeds.betwaypartnersafrica.com/api/637d3220-5bd6-4dcf-a28d-e67fadd34173/AffiliateFeed/v1/CpaReport.xml")
                .header("ClientId","value-plus.apps.betwaypartnersafrica.com")
                .header("ApiKey","5OsirI2titSQgoANSu4KW942skuxTeLhJIrF8XePrCI=")
                .header("Content-Type","application/xml")

                .retrieve()
                .bodyToMono(String.class)
                .block();


        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder1 = factory.newDocumentBuilder();
        Document document = builder1.parse(new InputSource(new StringReader(betWayResponse)));
        Element rootElement = document.getDocumentElement();

        NodeList nodeList = document.getElementsByTagName("CpaReportData");
        for (int temp = 0; temp < nodeList.getLength(); temp++) {
           Node node = nodeList.item(temp);
            System.out.println("\nCurrent element: " + node.getNodeName());
            if (node.getNodeType() == Node.ELEMENT_NODE) {


                Element element = (Element) node;
                Optional<CpaReportData> oldCpaReportData = cpaReportDataRepository.findByPlayerId(element.getElementsByTagName("PlayerId").item(0).getTextContent());
                if(oldCpaReportData.isPresent()){
                    CpaReportData cpaReportData =oldCpaReportData.get();
                    Optional<BetWayAgentData> optionalBetWayAgentData = betWayAgentDataRepository.findByAgentReferralCode(cpaReportData.getTag());
                    if(!cpaReportData.getQualifiedPlayers().equals(element.getElementsByTagName("QualifiedPlayers").item(0).getTextContent())){
                        cpaReportData.setQualifiedPlayers(element.getElementsByTagName("QualifiedPlayers").item(0).getTextContent());

                        if(optionalBetWayAgentData.isPresent()){
                            BetWayAgentData betWayAgentData = optionalBetWayAgentData.get();
                            betWayAgentData.setActiveReferrals(betWayAgentData.getActiveReferrals() + 1);
                            Optional<DigitalProductCommission> digitalProductCommissions = digitalProductCommissionRepository.findByProductProvider(ProductProvider.BETWAY);
                            if(digitalProductCommissions.isPresent()) {
                                BigDecimal commission = digitalProductCommissions.get().getCommissionPercentage();
                                betWayAgentData.setTotalEarning(betWayAgentData.getTotalEarning().add(commission));
                            }
                            betWayAgentDataRepository.save(betWayAgentData);
                            Optional<Wallet> wallet = walletRepository.findWalletByUser_Id(betWayAgentData.getUser().getId());
                            if(wallet.isPresent()){
                                Optional<DigitalProductCommission> digitalProductCommission = digitalProductCommissionRepository.findByProductProvider(ProductProvider.BETWAY);
                                if(digitalProductCommission.isPresent()){
                                    BigDecimal commission = digitalProductCommission.get().getCommissionPercentage();
                                    Wallet newBalance = wallet.get();
                                    newBalance.setAmount(newBalance.getAmount().add(commission));
                                    walletRepository.save(newBalance);
                                    walletHistoryService.createHistoryRecord(newBalance,commission, TransactionType.CREDIT,"Referral earnings from Betway");

                                }

                            }
                        }
                    }

                    cpaReportData.setLockedPlayers(element.getElementsByTagName("LockedPlayers").item(0).getTextContent());

                    cpaReportDataRepository.save(cpaReportData);
                }else if(oldCpaReportData.isEmpty()){
                    CpaReportData cpaReportData = new CpaReportData();
                    cpaReportData.setBrand(element.getElementsByTagName("Brand").item(0).getTextContent());
                    cpaReportData.setCountry(element.getElementsByTagName("Country").item(0).getTextContent());
                    cpaReportData.setCurrency(element.getElementsByTagName("Currency").item(0).getTextContent());
                    cpaReportData.setEarnings(element.getElementsByTagName("Earnings").item(0).getTextContent());
                    cpaReportData.setFirstPurchaseDate(element.getElementsByTagName("FirstPurchaseDate").item(0).getTextContent());
                    cpaReportData.setLockedPlayers(element.getElementsByTagName("LockedPlayers").item(0).getTextContent());
                    cpaReportData.setMonthToDateRegistration(element.getElementsByTagName("MonthToDateRegistrations").item(0).getTextContent());
                    cpaReportData.setPlatform(element.getElementsByTagName("Platform").item(0).getTextContent());
                    cpaReportData.setPlayerId(element.getElementsByTagName("PlayerId").item(0).getTextContent());
                    cpaReportData.setProduct(element.getElementsByTagName("Product").item(0).getTextContent());
                    cpaReportData.setPromotion(element.getElementsByTagName("Promotion").item(0).getTextContent());
                    cpaReportData.setQualifiedPlayers(element.getElementsByTagName("QualifiedPlayers").item(0).getTextContent());
                    cpaReportData.setQueryString(element.getElementsByTagName("QueryString").item(0).getTextContent());
                    cpaReportData.setReferringUrl(element.getElementsByTagName("ReferringUrl").item(0).getTextContent());
                    cpaReportData.setRegistrationDate(element.getElementsByTagName("RegistrationDate").item(0).getTextContent());
                    cpaReportData.setTag(element.getElementsByTagName("Tag").item(0).getTextContent());
                    cpaReportData.setTrafficSource(element.getElementsByTagName("TrafficSource").item(0).getTextContent());
                    cpaReportDataRepository.save(cpaReportData);

                    Optional<BetWayAgentData> optionalBetWayAgentData = betWayAgentDataRepository.findByAgentReferralCode(cpaReportData.getTag());
                    if(optionalBetWayAgentData.isPresent()) {
                        Optional<DigitalProductCommission> digitalProductCommissions = digitalProductCommissionRepository.findByProductProvider(ProductProvider.BETWAY);
                        if(digitalProductCommissions.isPresent()) {
                            BigDecimal commissions = digitalProductCommissions.get().getCommissionPercentage();

                            BetWayAgentData betWayAgentData = optionalBetWayAgentData.get();
                            betWayAgentData.setActiveReferrals(Integer.parseInt(cpaReportData.getQualifiedPlayers()) + betWayAgentData.getActiveReferrals());
                            betWayAgentData.setTotalReferrals(betWayAgentData.getTotalReferrals() + 1);
                            if (cpaReportData.getQualifiedPlayers().equals("1")) {

                                betWayAgentData.setTotalEarning(betWayAgentData.getTotalEarning().add(commissions));
                                Optional<Wallet> wallet = walletRepository.findWalletByUser_Id(betWayAgentData.getUser().getId());
                                if (wallet.isPresent()) {
                                    Optional<DigitalProductCommission> digitalProductCommission = digitalProductCommissionRepository.findByProductProvider(ProductProvider.BETWAY);
                                    if (digitalProductCommission.isPresent()) {
                                        Wallet newBalance = wallet.get();
                                        newBalance.setAmount(newBalance.getAmount().add(commissions));
                                        walletRepository.save(newBalance);
                                        walletHistoryService.createHistoryRecord(newBalance, commissions, TransactionType.CREDIT, "Referral earnings from Betway");
                                    }
                                }
                            }

                            betWayAgentDataRepository.save(betWayAgentData);
                        }

                    }
                }

            }
        }}

        @Scheduled(cron = "0 0 */3 * * ?")
        public void getRevShareReportData() throws IOException, ParserConfigurationException, SAXException {
            System.out.println("Rev share");
            WebClient.Builder builderOne = WebClient.builder();
            var betWayResponseOne = builderOne
                    .clientConnector(new ReactorClientHttpConnector(
                            HttpClient.create().followRedirect(true)
                    ))
                    .build()
                    .get()
                    .uri("http://affiliatefeeds.betwaypartnersafrica.com/api/637d3220-5bd6-4dcf-a28d-e67fadd34173/AffiliateFeed/v1/RevShareReport.xml")
                    .header("ClientId","value-plus.apps.betwaypartnersafrica.com")
                    .header("ApiKey","5OsirI2titSQgoANSu4KW942skuxTeLhJIrF8XePrCI=")
                    .header("Content-Type","application/xml")

                    .retrieve()
                    .bodyToMono(String.class)
                    .block();


            DocumentBuilderFactory factoryRev = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder2 = factoryRev.newDocumentBuilder();
            Document documentRev = builder2.parse(new InputSource(new StringReader(betWayResponseOne)));
            Element rootElementRev = documentRev.getDocumentElement();

            NodeList nodeListRev = documentRev.getElementsByTagName("RevShareReportData");
            for (int temp = 0; temp < nodeListRev.getLength(); temp++) {
                Node node = nodeListRev.item(temp);
                System.out.println("\nCurrent element: " + node.getNodeName());
                if (node.getNodeType() == Node.ELEMENT_NODE) {


                    Element element = (Element) node;
                    Optional<RevShareReportData> oldRevReportData = revShareReportDataRepository.findByPlayerId(element.getElementsByTagName("PlayerId").item(0).getTextContent());
                    if(oldRevReportData.isPresent()){
                        RevShareReportData revShareReportData =oldRevReportData.get();
                        Optional<BetWayAgentData> optionalBetWayAgentData = betWayAgentDataRepository.findByAgentReferralCode(revShareReportData.getTag());
                        if(!revShareReportData.getQualifiedPlayers().equals(element.getElementsByTagName("QualifiedPlayers").item(0).getTextContent())){
                            revShareReportData.setQualifiedPlayers(element.getElementsByTagName("QualifiedPlayers").item(0).getTextContent());}

                            if(!revShareReportData.getEarnings().equals(element.getElementsByTagName("Earnings").item(0).getTextContent())){
                                System.out.println(revShareReportData.getEarnings());
                                System.out.println(element.getElementsByTagName("Earnings").item(0).getTextContent());
                                System.out.println("not the same");
                                String newEarnings = element.getElementsByTagName("Earnings").item(0).getTextContent();
                                BigDecimal oldEarnings = BigDecimal.valueOf(Double.valueOf(revShareReportData.getEarnings()));
                                BigDecimal newEarningsD = BigDecimal.valueOf(Double.valueOf(newEarnings));
                                BigDecimal change = newEarningsD.subtract(oldEarnings);
                                revShareReportData.setEarnings(newEarningsD.toString());

                                Optional<Wallet> wallet = walletRepository.findWalletByUser_Id(optionalBetWayAgentData.get().getUser().getId());
                                if(wallet.isPresent()){
                                    Optional<DigitalProductCommission> digitalProductCommission = digitalProductCommissionRepository.findByProductProvider(ProductProvider.BETWAY);
                                    if(digitalProductCommission.isPresent()){
                                        BigDecimal earnings = change;
                                        earnings = earnings.multiply(BigDecimal.valueOf(Double.valueOf(rateOfTheDay)));
                                        System.out.println(Double.valueOf(revShareReportData.getEarnings()));
                                        Wallet newBalance = wallet.get();
                                        newBalance.setAmount(newBalance.getAmount().add(earnings));
                                        walletRepository.save(newBalance);
                                        walletHistoryService.createHistoryRecord(newBalance,change, TransactionType.CREDIT,"Loss earnings from Betway");
                                    }
                            }

                            if(optionalBetWayAgentData.isPresent()){
                                BetWayAgentData betWayAgentData = optionalBetWayAgentData.get();
                                if (!betWayAgentData.getTotalEarning().equals(revShareReportData.getEarnings())){
                                    betWayAgentData.setTotalEarning(betWayAgentData.getTotalEarning().add(change));
                                }}}

                                if(!revShareReportData.getMonthToDateRegistrations().toString().equals(element.getElementsByTagName("MonthToDateRegistrations").item(0).getTextContent())){
                                    String newReferralCount = element.getElementsByTagName("MonthToDateRegistrations").item(0).getTextContent();
                                    Integer existingCount = Integer.valueOf(revShareReportData.getMonthToDateRegistrations());
                                    Integer newCount = 0;
                                    Integer changeCount = 0;
                                    try{
                                   newCount = Integer.valueOf(newReferralCount);}
                                    catch (NumberFormatException ex){
                                        ex.printStackTrace();
                                    }
                                    if (newCount>existingCount){
                                     changeCount = newCount-existingCount;
                                    }else if (existingCount>newCount){
                                        changeCount = newCount;
                                    }
                                    revShareReportData.setMonthToDateRegistrations(revShareReportData.getMonthToDateRegistrations() + changeCount);
                                    if(optionalBetWayAgentData.isPresent()){
                                        BetWayAgentData betWayAgentData = optionalBetWayAgentData.get();
                                            if (betWayAgentData.getTotalReferrals()!=(Integer.valueOf(revShareReportData.getMonthToDateRegistrations()))){
                                            betWayAgentData.setTotalReferrals(betWayAgentData.getTotalReferrals()+changeCount);
                                        }}}
                                        revShareReportDataRepository.save(revShareReportData);

                                }

                    else if(oldRevReportData.isEmpty()){
                        RevShareReportData revShareReportData = new RevShareReportData();

                        revShareReportData.setBrand(element.getElementsByTagName("Brand").item(0).getTextContent());
                        revShareReportData.setCountry(element.getElementsByTagName("Country").item(0).getTextContent());
                        revShareReportData.setCountry(element.getElementsByTagName("Chargebacks").item(0).getTextContent());
                        revShareReportData.setCurrency(element.getElementsByTagName("Currency").item(0).getTextContent());
                        revShareReportData.setEarnings(element.getElementsByTagName("Earnings").item(0).getTextContent());
                        revShareReportData.setEarningsAfterTax(element.getElementsByTagName("EarningsAfterTax").item(0).getTextContent());
                        revShareReportData.setGamingRevenue(element.getElementsByTagName("GamingRevenue").item(0).getTextContent());
                        revShareReportData.setFirstPurchaseDate(element.getElementsByTagName("FirstPurchaseDate").item(0).getTextContent());
                        revShareReportData.setLockedPlayers(element.getElementsByTagName("LockedPlayers").item(0).getTextContent());
                        revShareReportData.setMonthToDateRegistrations(element.getElementsByTagName("MonthToDateRegistrations").item(0).getTextContent());
                        revShareReportData.setPlatform(element.getElementsByTagName("Platform").item(0).getTextContent());
                        revShareReportData.setPlayerId(element.getElementsByTagName("PlayerId").item(0).getTextContent());
                        revShareReportData.setProduct(element.getElementsByTagName("Product").item(0).getTextContent());
                        revShareReportData.setPromotion(element.getElementsByTagName("Promotion").item(0).getTextContent());
                        revShareReportData.setPromotion(element.getElementsByTagName("Purchases").item(0).getTextContent());
                        revShareReportData.setQualifiedPlayers(element.getElementsByTagName("QualifiedPlayers").item(0).getTextContent());
                        revShareReportData.setQueryString(element.getElementsByTagName("QueryString").item(0).getTextContent());
                        revShareReportData.setReferringUrl(element.getElementsByTagName("ReferringUrl").item(0).getTextContent());
                        revShareReportData.setRegistrationDate(element.getElementsByTagName("RegistrationDate").item(0).getTextContent());
                        revShareReportData.setTag(element.getElementsByTagName("Tag").item(0).getTextContent());
                        revShareReportData.setTrafficSource(element.getElementsByTagName("TrafficSource").item(0).getTextContent());
                        revShareReportDataRepository.save(revShareReportData);

                        Optional<BetWayAgentData> optionalBetWayAgentData = betWayAgentDataRepository.findByAgentReferralCode(revShareReportData.getTag());
                        if(optionalBetWayAgentData.isPresent()) {
                            Optional<DigitalProductCommission> digitalProductCommissions = digitalProductCommissionRepository.findByProductProvider(ProductProvider.BETWAY);
                            if(digitalProductCommissions.isPresent()) {
                             //   BigDecimal commissions = digitalProductCommissions.get().getCommissionPercentage();

                                BetWayAgentData betWayAgentData = optionalBetWayAgentData.get();
                                betWayAgentData.setActiveReferrals(Integer.parseInt(revShareReportData.getQualifiedPlayers()) + betWayAgentData.getActiveReferrals());
                                betWayAgentData.setTotalReferrals(betWayAgentData.getTotalReferrals() + 1);
                                if (Double.valueOf(revShareReportData.getEarnings())>0) {
                                    BigDecimal newAmount = BigDecimal.valueOf(Double.valueOf(revShareReportData.getEarnings()));
                                    newAmount = newAmount.multiply(BigDecimal.valueOf(Double.valueOf(rateOfTheDay)));
                                    betWayAgentData.setTotalEarning(betWayAgentData.getTotalEarning().add(newAmount));
                                    Optional<Wallet> wallet = walletRepository.findWalletByUser_Id(betWayAgentData.getUser().getId());
                                    if (wallet.isPresent()) {
                                       // Optional<DigitalProductCommission> digitalProductCommission = digitalProductCommissionRepository.findByProductProvider(ProductProvider.BETWAY);

                                            Wallet newBalance = wallet.get();
                                            newBalance.setAmount(newBalance.getAmount().add(newAmount));
                                            walletRepository.save(newBalance);
                                            walletHistoryService.createHistoryRecord(newBalance, newAmount, TransactionType.CREDIT, "Loss earnings from Betway");

                                    }
                                }

                                betWayAgentDataRepository.save(betWayAgentData);
                            }

                        }
                    }

                }
            }


        }

    public ResponseEntity getBetWayReport(User user){
       BetWayAgentData betWayAgentData = betWayAgentDataRepository.findByUser(user).orElseThrow(
               () -> new BadRequestException("user has not registerd for Betway promotion")
       );

        return new ResponseEntity(betWayAgentData,HttpStatus.OK);

    }

    public void registerAgentOnTable(){
        List<ProductProviderUser> productProviderUsers = productProviderUserRepository.findByProvider(ProductProvider.BETWAY);
        for (ProductProviderUser user:productProviderUsers) {
            Optional<BetWayAgentData> betWayAgentData = betWayAgentDataRepository.findByAgentReferralCode(user.getAgentCode().split("=")[2]);
            if(betWayAgentData.isEmpty()){
                BetWayAgentData betWayAgentData1 = new BetWayAgentData();
                betWayAgentData1.setUser(user.getUser());
                betWayAgentData1.setAgentReferralCode(user.getAgentCode().split("=")[2]);
                betWayAgentDataRepository.save(betWayAgentData1);
            }
        }
    }

//    @Scheduled(cron = "0 */5 * ? * *")
//    public void getBet9jaReport() {
//        System.out.println("bet9ja report");
//
//
//        LocalDate initial = LocalDate.now();
//        LocalDate start = initial.with(firstDayOfMonth());
//        LocalDate end = initial.with(lastDayOfMonth());
//        Bet9jaResponse bet9jaResponse = bet9jaService.getAgentReportData(start.toString(), end.toString());
//
//        ArrayList<Results> results = bet9jaResponse.getResults();
//        System.out.println("results.size() = " + results.size());
//
//        List<Bet9jaCpaData> bet9jaCpaData = bet9jaCpaDataRepository.findAll();
//        System.out.println("bet9jaCpaData.size() = " + bet9jaCpaData.size());
//        for (Bet9jaCpaData cpaData : bet9jaCpaData) {
//            int totalRegistered = 0;
//            int activeRegistered = 0;
//            int differenceTotal = 0;
//            int differenceActive = 0;
//            System.out.println("cpaData.getUser().getFirstname() = " + cpaData.getUser().getFirstname());
//            System.out.println("cpaData.getAgentReferralCode() = " + cpaData.getAgentReferralCode());
//            System.out.println("results.get(0) = " + results.get(0));
//            System.out.println("results.get(1) = " + results.get(1));
//            List<Results> agentData = results.stream().filter(r -> r.getS1().equals(cpaData.getAgentReferralCode())).collect(Collectors.toList());
//            System.out.println("agentData.size() = " + agentData.size());
//            Optional<Bet9jaAgentData> bet9jaAgentData = bet9jaRepository.findByAgentReferralCode(cpaData.getAgentReferralCode());
//            Bet9jaAgentData agentData1 = bet9jaAgentData.get();
//            if (!agentData.isEmpty()) {
//                for (Results iAgents : agentData) {
//                    totalRegistered++;
//                    System.out.println("totalRegistered = " + totalRegistered);
//                    if (iAgents.getFirstBetDate() != null && iAgents.getFirstDepositDate() != null) {
//                        activeRegistered++;
//                        System.out.println("activeRegistered = " + activeRegistered);
//                    }
//                }
//                if (cpaData.getTotalReferrals() != totalRegistered) {
//                    System.out.println("cpaData = " + cpaData.getTotalReferrals());
//                    System.out.println("totalRegistered = " + totalRegistered);
//                    differenceTotal = totalRegistered - cpaData.getTotalReferrals();
//                    cpaData.setTotalReferrals(totalRegistered);
//                    agentData1.setTotalReferrals(agentData1.getTotalReferrals() + differenceTotal);
//                    bet9jaCpaDataRepository.save(cpaData);
//                    bet9jaRepository.save(agentData1);
//                } if (cpaData.getActiveReferrals() != activeRegistered) {
//                    System.out.println("cpaData = " + cpaData.getActiveReferrals());
//                    System.out.println("activeRegistered = " + activeRegistered);
//                    differenceActive = activeRegistered - cpaData.getActiveReferrals();
//                    cpaData.setActiveReferrals(activeRegistered);
//                    agentData1.setActiveReferrals(agentData1.getActiveReferrals() + differenceActive);
//                    bet9jaCpaDataRepository.save(cpaData);
//                    bet9jaRepository.save(agentData1);
//                }
//                if (differenceActive > 0) {
//                    System.out.println("differenceActive = " + differenceActive);
//                    System.out.println("agentData1.getAgentReferralCode() = " + agentData1.getAgentReferralCode());
//                    Optional<Wallet> wallet = walletRepository.findWalletByUser_Id(cpaData.getUser().getId());
//                    if (wallet.isPresent()) {
//                        Optional<DigitalProductCommission> digitalProductCommission = digitalProductCommissionRepository.findByProductProvider(ProductProvider.BET9JA);
//                        if (digitalProductCommission.isPresent()) {
//                            BigDecimal commissions = digitalProductCommission.get().getCommissionPercentage();
//                            BigDecimal earnings = commissions.multiply(BigDecimal.valueOf(differenceActive));
//                            Wallet newBalance = wallet.get();
//                            newBalance.setAmount(newBalance.getAmount().add(earnings));
//                            walletRepository.save(newBalance);
//                            walletHistoryService.createHistoryRecord(newBalance, earnings, TransactionType.CREDIT, "Referral earnings from Bet9ja");
//                        }
//                    }
//                }
//            }
//        }
//    }





    @Async
    public void loadCpaReportOnStartUp() throws IOException, ParserConfigurationException, SAXException {
        this.getCpaReport();
    }



}