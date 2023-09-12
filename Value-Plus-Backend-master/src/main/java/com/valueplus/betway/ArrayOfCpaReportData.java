package com.valueplus.betway;

import com.valueplus.persistence.entity.CpaReportData;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(namespace="http://schemas.datacontract.org/2004/07/Arcadia.Contracts.Api.ReportFeeds",
        name="ArrayOfCpaReportData")

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class ArrayOfCpaReportData {

    @XmlElement(name = "CpaReportData", required = true)
    private List<CpaReportData> cpaReportDataList;
}
