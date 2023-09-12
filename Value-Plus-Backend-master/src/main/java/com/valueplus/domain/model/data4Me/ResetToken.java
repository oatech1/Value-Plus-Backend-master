package com.valueplus.domain.model.data4Me;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ResetToken {
    @NotBlank
    @Size(min = 6, max = 6)
    private String resetToken;
}
