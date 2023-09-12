package com.valueplus.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "pin_reset_token")
public class PinResetToken extends BasePersistentEntity {
    @Id
    private Long userId;
    private String resetToken;
}
