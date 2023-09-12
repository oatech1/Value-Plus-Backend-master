package com.valueplus.persistence.repository;

import com.valueplus.domain.enums.SocialMedia;
import com.valueplus.persistence.entity.UsersSocialMediaReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersSocialMediaReportRepository extends JpaRepository<UsersSocialMediaReport, Long> {

    UsersSocialMediaReport findBySocialMedia(SocialMedia socialMedia);
}
