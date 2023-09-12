package com.valueplus.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.valueplus.domain.enums.State;
import com.valueplus.domain.model.AgentCreate;
import com.valueplus.domain.model.AgentDto;
import com.valueplus.domain.model.UserCreate;
import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.valueplus.domain.util.FunctionUtil.emptyIfNullStream;
import static javax.persistence.FetchType.EAGER;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BasePersistentEntity implements UserDetails, ToModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String firstname;
    @Column(nullable = false)
    private String lastname;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private String email;
    private String phone;
    private String address;
    @Enumerated(EnumType.STRING)
//    @Column(columnDefinition = "varchar(255) default ''")
    private State state ;
//    @Column(columnDefinition = "varchar(255) default ''")
    private String city ="";
    @Setter
    private String agentCode;
    private String referralCode;
    private boolean emailVerified;
    private String transactionPin;
    @Column(columnDefinition = "boolean default false")
    private boolean kycVerification ;
    @Column(columnDefinition = " int default '0'")
    private int retries =0;

    @OneToOne
    @JoinColumn(name = "super_agent_id")
    private User superAgent;

    @OneToOne
    @JoinColumn(name = "agent_id")
    private User agent;

    @OneToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToMany(fetch = EAGER)
    @JoinTable(
            name = "user_authority",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "authority_id", referencedColumnName = "id"
            )
    )
    private Collection<Authority> authorities;

    private boolean enabled = false;
    private boolean deleted = false;
    @Transient
    private transient boolean isTransactionTokenSet;
    @Column(name = "activated")
    private boolean activated ;

    @JsonIgnore
    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(mappedBy = "user")
    private List<ProductProviderUser> productProviders;

    public static UserBuilder from(AgentCreate agentCreate) {
        return builder()
                .email(agentCreate.getEmail().toLowerCase())
                .firstname(agentCreate.getFirstname())
                .lastname(agentCreate.getLastname())
                .phone(agentCreate.getPhone())
                .address(agentCreate.getAddress())
                .city(agentCreate.getCity())
                .state(State.valueOf(agentCreate.getState().toUpperCase()));
    }

    public static UserBuilder from(UserCreate agentCreate) {
        return builder()
                .email(agentCreate.getEmail().toLowerCase())
                .firstname(agentCreate.getFirstname())
                .lastname(agentCreate.getLastname())
                .phone(agentCreate.getPhone())
                .address(agentCreate.getAddress())
                .city(agentCreate.getCity())
                .state(State.valueOf(agentCreate.getState().toUpperCase()));
    }
    public interface AgentData {
        String getEmail();
        String getFirstname();
        String getLastname();
        String getPhone();
        String getAddress();
    }

    public interface subAgentData{
        String getEmail();
        String getFirstname();
        String getLastname();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Role role = getRole();
        if (role != null) {
            String roleName = "ROLE_" + role.getName();
            authorities.add(new SimpleGrantedAuthority(roleName));
        }

        emptyIfNullStream(this.authorities)
                .map(a -> a.getAuthority().toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return authorities;
    }

    public Collection<Authority> getUserAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public UserBuilder toBuilder() {
        return builder()
                .id(id)
                .firstname(firstname)
                .lastname(lastname)
                .email(email)
                .password(password)
                .phone(phone)
                .address(address)
                .agentCode(agentCode)
                .emailVerified(emailVerified)
                .enabled(enabled)
                .deleted(deleted)
                .role(role)
                .kycVerification(kycVerification);
    }

    public boolean isTransactionTokenSet() {
        return !isNullOrEmpty(transactionPin);
    }

    @Override
    public Object toModel() {
        return AgentDto.valueOf(this);
    }
}
