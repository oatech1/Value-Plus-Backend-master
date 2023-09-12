package com.valueplus.domain.model;

import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.domain.products.ProductProviderUrlService;
import com.valueplus.persistence.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.valueplus.domain.util.FunctionUtil.emptyIfNullStream;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;

@NoArgsConstructor
@Getter
@Slf4j
public class AgentDto extends UserDto {

    private Long id;
    private static final String BASE_LINK = "https://play.google.com/store/apps/details?id=je.data4me.jara&referrer=utm_campaign%3D";
    private String agentCode;
    private String link;
    private String photo;
    private boolean emailVerified;
    private String superAgentCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean kycVerified;
    private boolean activated;

    @Setter
    private Map<ProductProvider, String> referralData;

    @Builder
    public AgentDto(Long id,
                    String firstname,
                    String lastname,
                    String email,
                    String phone,
                    String address,
                    String roleType,
                    String agentCode,
                    String link,
                    String photo,
                    boolean emailVerified,
                    boolean kycVerified,
                    boolean activated,
                    String referralCode,
                    String superAgentCode,
                    boolean enabled,
                    Map<ProductProvider, String> referralData, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, firstname, lastname, email, phone, address, roleType, referralCode, false, emptySet(), enabled);
        this.agentCode = agentCode;
        this.link = link;
        this.photo = photo;
        this.superAgentCode = superAgentCode;
        this.emailVerified = emailVerified;
        this.activated = activated;
        this.kycVerified = kycVerified;
        this.referralData = referralData;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.id = id;
    }

    public static AgentDto valueOf(User user) {
        return valueOf(user, null, new HashMap<>());
    }

    public static AgentDto valueOf(User user, Map<ProductProvider, ProductProviderUrlService> providerUrlServiceMap) {
        return valueOf(user, null, providerUrlServiceMap);
    }

    public static AgentDto valueOf(User user, String photo, Map<ProductProvider, ProductProviderUrlService> providerUrlServiceMap) {
        AgentDtoBuilder builder = builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .phone(user.getPhone())
                .address(user.getAddress())
                .emailVerified(user.isEmailVerified())
                .roleType(user.getRole().getName())
                .referralCode(user.getReferralCode())
                .superAgentCode(ofNullable(user.getSuperAgent()).map(User::getReferralCode).orElse(null))
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .photo(photo)
                .kycVerified(user.isKycVerification())
                        .activated(user.isActivated());

        ofNullable(user.getAgentCode())
                .ifPresent(agentCode -> builder.agentCode(agentCode).link(BASE_LINK.concat(agentCode)));

        Map<ProductProvider, String> referralData = new HashMap<>();

        if (!providerUrlServiceMap.isEmpty()) {
            System.out.println(user.getProductProviders());
            emptyIfNullStream(user.getProductProviders())
                    .forEach(s -> {
                        System.out.println(s.getProvider().name());
                        log.info("before agentUrl");
//                        if (s.getProvider().name().compareTo("DATA4ME")!=0) {
                            log.info("passed inside functions");
                            String agentUrl = providerUrlServiceMap.get(s.getProvider()).getReferralUrl(s.getAgentUrl(), s.getAgentUrl());
                            log.info("before agentUrl" + agentUrl);
                            referralData.put(s.getProvider(), agentUrl);
                        });
        }


        var agentDto = builder.build();
        agentDto.setTransactionTokenSet(user.isTransactionTokenSet());
        agentDto.setAuthorities(extractAuthorities(user));
        agentDto.setReferralData(referralData);
        return agentDto;
    }
}
