package com.valueplus.domain.service.concretes;

import com.valueplus.app.model.SocialMediaReportResponse;
import com.valueplus.domain.service.abstracts.SocialMediaReportService;
import com.valueplus.persistence.entity.UsersSocialMediaReport;
import com.valueplus.persistence.repository.UsersSocialMediaReportRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class SocialMediaReportServiceImpl implements SocialMediaReportService {

    private final UsersSocialMediaReportRepository socialMediaReportRepository;

    public List<SocialMediaReportResponse> getMediaStatistics() {
        List<UsersSocialMediaReport> reportList = socialMediaReportRepository.findAll();
        List<SocialMediaReportResponse> responseList = new ArrayList<>();


        for(UsersSocialMediaReport each: reportList){
            SocialMediaReportResponse reportResponse =  SocialMediaReportResponse.builder()
                    .socialMedia(each.getSocialMedia().toString())
                    .numberOfUsers(each.getNumberOfUsers().toString())
                    .build();
            responseList.add(reportResponse);
        }

        return responseList;
    }
}
