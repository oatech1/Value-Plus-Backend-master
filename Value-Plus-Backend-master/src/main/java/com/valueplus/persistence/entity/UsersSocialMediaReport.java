package com.valueplus.persistence.entity;

import com.valueplus.domain.enums.SocialMedia;
import lombok.*;

import javax.persistence.*;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "social_media_report")
public class UsersSocialMediaReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private SocialMedia socialMedia;
    private Long numberOfUsers;
}
