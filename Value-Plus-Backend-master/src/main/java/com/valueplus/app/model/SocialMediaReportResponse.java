package com.valueplus.app.model;

import lombok.*;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocialMediaReportResponse {
    @Enumerated(EnumType.STRING)
    private String socialMedia;
    private String numberOfUsers;
}
