package com.valueplus.domain.service.concretes;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.domain.model.DigitalProductCommissionResponse;
import com.valueplus.domain.model.MessageResponse;
import com.valueplus.domain.model.PercentageCommissionRequest;
import com.valueplus.domain.service.abstracts.DigitalProductService;
import com.valueplus.persistence.entity.DigitalProductCommission;
import com.valueplus.persistence.repository.DigitalProductCommissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RequiredArgsConstructor
@Service
public class DigitalProductServiceImpl implements DigitalProductService {

    private final DigitalProductCommissionRepository digitalProductCommissionRepository;

    public MessageResponse adminSetDigitalProductCommissionRate(PercentageCommissionRequest percentageCommissionRequest) throws ValuePlusException {

        var request = percentageCommissionRequest.getDigitalProduct();
        ProductProvider productProvider = null;

        if(request.equalsIgnoreCase(ProductProvider.BETA_CARE.name())) {
            productProvider = ProductProvider.BETA_CARE;
        }
//
//        if(request.equalsIgnoreCase(ProductProvider.DATA4ME.name())){
//            productProvider = ProductProvider.DATA4ME;
//        }

        if (request.equalsIgnoreCase(ProductProvider.BETWAY.name())) {
            productProvider = ProductProvider.BETWAY;
        }

        if (request.equalsIgnoreCase(ProductProvider.VALUEPLUS.name())) {
            productProvider = ProductProvider.VALUEPLUS;
        }

        if(productProvider == null) throw new ValuePlusException("Enter valid digital product name", BAD_REQUEST);

        var result = digitalProductCommissionRepository.findByProductProvider(productProvider);

        DigitalProductCommission digitalProductCommission;

        if(result.isEmpty()){
            digitalProductCommission = new DigitalProductCommission();
            digitalProductCommission.setProductProvider(productProvider);
            digitalProductCommission.setCommissionPercentage(percentageCommissionRequest.getCommission());
        }else digitalProductCommission = result.get();

        digitalProductCommission.setCommissionPercentage(percentageCommissionRequest.getCommission());

        digitalProductCommissionRepository.save(digitalProductCommission);

        return new MessageResponse(String.format("Successfully updated percentage commission on %s", request));
    }

    @Override
    public List<DigitalProductCommissionResponse> fetchDigitalProductCommission() throws ValuePlusException {
        List<DigitalProductCommissionResponse> digitalProductCommissionResponseList = new ArrayList<>();

        try{
            List<DigitalProductCommission> digitalProductCommission = digitalProductCommissionRepository.findAll();
            if(!ObjectUtils.isEmpty(digitalProductCommission)){
                for (DigitalProductCommission commission: digitalProductCommission) {
                    DigitalProductCommissionResponse digitalProductCommissionResponse = new DigitalProductCommissionResponse();
                    digitalProductCommissionResponse.setProductProvider(commission.getProductProvider());
                    digitalProductCommissionResponse.setCommission(commission.getCommissionPercentage());
                    digitalProductCommissionResponseList.add(digitalProductCommissionResponse);
                }
            }
        } catch (Exception err){
            log.error("DigitalProductCommissionResponse",err);
        }
        return digitalProductCommissionResponseList;
    }
}
