package com.valueplus.domain.service.abstracts;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.DigitalProductCommissionResponse;
import com.valueplus.domain.model.MessageResponse;
import com.valueplus.domain.model.PercentageCommissionRequest;
import com.valueplus.persistence.entity.DigitalProductCommission;

import java.util.List;

public interface DigitalProductService {
    MessageResponse adminSetDigitalProductCommissionRate(PercentageCommissionRequest percentageCommissionRequest) throws ValuePlusException;

    List<DigitalProductCommissionResponse> fetchDigitalProductCommission() throws ValuePlusException;
}
