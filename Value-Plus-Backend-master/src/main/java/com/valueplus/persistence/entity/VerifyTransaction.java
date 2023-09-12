package com.valueplus.persistence.entity;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "verify_transaction")
public class VerifyTransaction extends BasePersistentEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;

    @ManyToOne
    @JoinColumn(name = "credited_user_id")
    private User creditedUser;

    @ManyToOne
    @JoinColumn(name = "debited_user_id")
    private User debitedUser;

    private BigDecimal amount;
}
