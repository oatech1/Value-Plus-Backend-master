package com.valueplus.app.config;

import com.valueplus.betway.BetWayAgentData;
import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.domain.enums.TransactionType;
import com.valueplus.domain.service.abstracts.TransferService;
import com.valueplus.domain.service.abstracts.WalletHistoryService;
import com.valueplus.domain.service.concretes.AgentMonthlyReportService;
import com.valueplus.domain.service.concretes.DefaultSystemSetting;
import com.valueplus.domain.service.concretes.TokenService;
import com.valueplus.persistence.entity.CpaReportData;
import com.valueplus.persistence.entity.DigitalProductCommission;
import com.valueplus.persistence.entity.Wallet;
import com.valueplus.persistence.repository.BetWayAgentDataRepository;
import com.valueplus.persistence.repository.CpaReportDataRepository;
import com.valueplus.persistence.repository.DigitalProductCommissionRepository;
import com.valueplus.persistence.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobScheduler {

    private final TransferService transferService;
    private final TokenService tokenService;
    private final AgentMonthlyReportService reportService;
    private final DefaultSystemSetting defaultSystemSetting;
    private final CpaReportDataRepository cpaReportDataRepository;
    private final BetWayAgentDataRepository betWayAgentDataRepository;
    private final WalletRepository walletRepository;
    private final WalletHistoryService walletHistoryService;
    private final DigitalProductCommissionRepository digitalProductCommissionRepository;


    @Scheduled(cron = "${vp.verify-transactions.status.cron:0 0 0 * * ?}")
    public void scheduleVerifyPendingTransactions() {
        transferService.verifyPendingTransactions();
    }

    @Scheduled(cron = "${vp.token-delete.cron:0 0 0 * * ?}")
    public void deleteInvalidToken() {
        tokenService.deleteExpiredTokens();
    }

    @Scheduled(cron = "${vp.agent.report.cron:0 0 3 1 1/1 ?}")
    public void downloadAgentReport() {
        try {
            reportService.loadMonthlyReport();
        } catch (Exception e) {
            log.error("Error loading report", e);
        }
    }

    @Scheduled(cron = "${commission.effective-cron:0 0 0 * * ?}")
    public void updateScheduledCommission() {
        defaultSystemSetting.effectCommission();
    }

    @Scheduled(cron = "0 0 3 1 * ?")
    public void getCpaReport() throws IOException, ParserConfigurationException, SAXException {
        String year = null;
        String month = String.valueOf(LocalDateTime.now().getMonth().minus(1).getValue());

        if(LocalDateTime.now().getYear() != LocalDateTime.now().minusDays(1).getYear()){
            year = String.valueOf(LocalDateTime.now().minusDays(1).getYear());
        }else
        {
            year= String.valueOf(LocalDateTime.now().getYear());
        }


        WebClient.Builder builder = WebClient.builder();
        var betWayResponse = builder
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().followRedirect(true)
                ))
                .build()
                .get()
                .uri("http://affiliatefeeds.betwaypartnersafrica.com/api/637d3220-5bd6-4dcf-a28d-e67fadd34173/AffiliateFeed/v1/CpaReport.xml?year="+year+"&month="+month)
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
        Optional<DigitalProductCommission> digitalProductCommissions = digitalProductCommissionRepository.findByProductProvider(ProductProvider.BETWAY);

            BigDecimal commission = digitalProductCommissions.get().getCommissionPercentage();


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
                            betWayAgentData.setTotalEarning(betWayAgentData.getTotalEarning().add(commission));
                            betWayAgentDataRepository.save(betWayAgentData);
                            Optional<Wallet> wallet = walletRepository.findWalletByUser_Id(betWayAgentData.getUser().getId());
                            if(wallet.isPresent()){
                                Wallet newBalance = wallet.get();
                                newBalance.setAmount(newBalance.getAmount().add(commission));
                                walletRepository.save(newBalance);
                                walletHistoryService.createHistoryRecord(newBalance,commission, TransactionType.CREDIT,"Referral earnings from Betway");
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
                    if(optionalBetWayAgentData.isPresent()){
                        BetWayAgentData betWayAgentData = optionalBetWayAgentData.get();
                        betWayAgentData.setActiveReferrals(Integer.parseInt(cpaReportData.getQualifiedPlayers())+betWayAgentData.getActiveReferrals());
                        betWayAgentData.setTotalReferrals(betWayAgentData.getTotalReferrals()+1);
                        if(cpaReportData.getQualifiedPlayers().equals("1")){
                            betWayAgentData.setTotalEarning(betWayAgentData.getTotalEarning().add(commission));
                            Optional<Wallet> wallet = walletRepository.findWalletByUser_Id(betWayAgentData.getUser().getId());
                            if(wallet.isPresent()){
                                Wallet newBalance = wallet.get();
                                newBalance.setAmount(newBalance.getAmount().add(commission));
                                walletRepository.save(newBalance);
                                walletHistoryService.createHistoryRecord(newBalance,commission, TransactionType.CREDIT,"Referral earnings from Betway");
                            }
                        }
                        betWayAgentDataRepository.save(betWayAgentData);

                    }
                }

            }
        }


    }

}
