package com.valueplus.app.controller;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.valueplus.app.model.Note;
import com.valueplus.domain.service.concretes.FirebaseMessagingService;
import com.valueplus.firebase.firebase.FCMService;
import com.valueplus.firebase.model.PushNotificationRequest;
import com.valueplus.persistence.entity.FirebaseToken;
import com.valueplus.persistence.repository.FireBaseTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@RestController

@DependsOn("fCMInitializer")
public class FireBaseTest {
    @Autowired
    FirebaseMessagingService firebaseMessagingService;

    @Autowired
    FireBaseTokenRepository fireBaseTokenRepository;

    @Autowired
    FCMService fcmService;

    @GetMapping("/v1/firebasetesting/{userId}")
    public String testFireBase(@PathVariable Long userId) throws FirebaseMessagingException, ExecutionException, InterruptedException {

        Optional<FirebaseToken> firebaseToken = fireBaseTokenRepository.findByUserId(userId);

        PushNotificationRequest request = new PushNotificationRequest();

       request.setToken("cQfpLBl-o80VEhbHzkH-Kr:APA91bHEW6fNNu9ES35eQS9QdadyRV7wNA5DK9xHfwBqmiwaL370Bb6yO_JTVE0gAA0pfon6xB6xhT1yDgMnfkPvGn19ToVU0cWtYaE-xsNI5yh1oz69tYt9rGy7-cy89Jo-_QrTorjR");
       request.setTitle("My PushNotification");
       request.setMessage("Hello World!");

       try {
           fcmService.sendMessageToToken(request);
       }   catch (Exception err){
           System.out.println(err.getMessage());
       }


            return "Sent Successfully";

    }

}
