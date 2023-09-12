package com.valueplus.domain.service.abstracts;

import com.valueplus.app.model.SocialMediaReportResponse;

import java.util.List;

public interface SocialMediaReportService {
    List<SocialMediaReportResponse> getMediaStatistics();
}
