package com.valueplus.persistence.entity;

import com.valueplus.domain.model.AccountModel;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "account")
public class Account extends BasePersistentEntity implements ToModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String accountNumber;
    private String accountName;
    private String bankCode;
    private String bankName;
    @OneToOne
    @JoinColumn(name = "user_id")
//    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Override
    public AccountModel toModel() {
        return AccountModel.builder()
                .id(this.id)
                .accountName(this.accountName)
                .accountNumber(this.accountNumber)
                .bankCode(this.bankCode)
                .userId(this.user.getId())
                .bankName(this.bankName)
                .build();
    }
}
