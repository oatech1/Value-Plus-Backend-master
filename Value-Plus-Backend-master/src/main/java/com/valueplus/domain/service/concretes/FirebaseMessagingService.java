package com.valueplus.domain.service.concretes;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.valueplus.app.exception.BadRequestException;
import com.valueplus.app.model.Note;
import org.springframework.stereotype.Service;

@Service
public class FirebaseMessagingService {

    private final FirebaseMessaging firebaseMessaging;

    public FirebaseMessagingService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }


    public void sendNotification(Note note, String token) throws FirebaseMessagingException {
        String vain = null;
        try{
//            System.out.println("Eneterd send notidfication");
//            Notification notification = Notification
//                    .builder()
//                    .setTitle(note.getSubject())
//                    .setBody(note.getContent())
//                    .build();
//            System.out.println("Got to 2");
//            Message message = Message
//                    .builder()
//                    .setToken(token)
//                    .setNotification(notification)
//                    .build();
//            System.out.println("Got to 3");
//           firebaseMessaging.send(message);
//            System.out.println("Got to 4");
        }catch (Exception err){
            System.out.println(err.getMessage());
            throw new BadRequestException(err.getMessage());
        }
    }

}