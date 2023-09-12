package com.valueplus.app.controller;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.PercentageCommissionRequest;
import com.valueplus.domain.service.abstracts.DigitalProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RequiredArgsConstructor
@Validated
@Slf4j
@RestController
@RequestMapping(path = "v1/digital-products", produces = MediaType.APPLICATION_JSON_VALUE)
public class DigitalProductCommissionController {

    private final DigitalProductService digitalProductService;

    @PostMapping("/commission/update")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> commissionUpdate(@Valid @RequestBody PercentageCommissionRequest request) throws ValuePlusException {
        return ResponseEntity.ok().body(digitalProductService.adminSetDigitalProductCommissionRate(request));
    }

    @GetMapping("/commission")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> fetchDigitalProductCommission() throws ValuePlusException {
        return ResponseEntity.ok().body(digitalProductService.fetchDigitalProductCommission());
    }
}
