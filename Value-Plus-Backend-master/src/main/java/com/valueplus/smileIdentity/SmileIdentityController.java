package com.valueplus.smileIdentity;


import com.valueplus.domain.model.MessageResponse;
import com.valueplus.persistence.entity.KycVerification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "v1/smile", produces = MediaType.APPLICATION_JSON_VALUE)
public class SmileIdentityController {

 private final SmileIdentityServiceImpl smileIdentityService;
    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.OK)
    public void prepUpload(@RequestBody SmileDTO smileDTO) throws Exception {
       smileIdentityService.makeUploadRequestLive(smileDTO);
    }

//    @PostMapping("/tupload")
//    @ResponseStatus(HttpStatus.OK)
//    public SmileIdentityResponse upload() throws Exception {
//        return smileIdentityService.makeUploadRequest();
//    }

    @GetMapping("status")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponse checkStatus() {
        return smileIdentityService.kycStatus();
    }

    @PostMapping("/callback")
   @ResponseStatus(HttpStatus.OK)
   public void callback(@RequestBody SmileCallback smileCallback){
         smileIdentityService.callBack(smileCallback);
    }


}
