package com.valueplus.domain.model;

import com.valueplus.domain.enums.PaymentPlatform;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class PaymentRequestDTO {
    @NotBlank
    @Email
    private String email;
    @Min(value = 1)
    private BigDecimal amount;
    @NotBlank
    private String orderSkuId;

    private PaymentPlatform paymentPlatform;
}
