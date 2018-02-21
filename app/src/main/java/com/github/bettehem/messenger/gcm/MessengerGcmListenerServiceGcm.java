package com.github.bettehem.messenger.gcm;

import android.os.Handler;
import com.github.bettehem.messenger.tools.background.ReceivedMessage;
import com.github.bettehem.messenger.tools.background.RequestResponse;
import com.github.bettehem.messenger.tools.listeners.GcmReceivedListener;
import com.github.bettehem.messenger.tools.managers.ChatsManager;
import com.github.bettehem.messenger.tools.users.Sender;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import static com.github.bettehem.messenger.tools.ui.CustomNotificationKt.notification;

public class MessengerGcmListenerServiceGcm extends FirebaseMessagingService implements GcmReceivedListener {

    @Override
    public void onMessageReceived(RemoteMessage message){
        final Map data = message.getData();

        String type = (String) data.get("type");
        Handler mainHandler = new Handler(getApplication().getMainLooper());
        Runnable myRunnable;

        if (type != null){

            switch (type.toLowerCase()){

                case "notification":
                    notification(getApplicationContext(), (String)  data.get("title"), (String) data.get("message"), false);
                    break;

                case "chatrequest":
                    final String requestSender = getSender((String) data.get("sender"));
                    myRunnable = () -> ChatsManager.handleChatRequest(getApplicationContext(), requestSender, (String) data.get("key"), (String) data.get("iv"));
                    mainHandler.post(myRunnable);
                    break;

                case "message":
                    ReceivedMessage receivedMessage = new ReceivedMessage(getApplicationContext(), true, (String) data.get("sender") + ChatsManager.SPLITTER + data.get("isSecretMessage"), (String) data.get("message"));
                    receivedMessage.setMessageListener(this);
                    receivedMessage.getMessage();
                    break;

                case "requestresponse":
                    String responseSender = getSender((String) data.get("sender"));
                    boolean requestAccepted = Boolean.valueOf((String) data.get("requestAccepted"));
                    String password = (String) data.get("password");
                    RequestResponse requestResponse = new RequestResponse(getApplicationContext(), requestAccepted, responseSender, password);
                    requestResponse.handleResponse();
                    break;

                case "startchat":
                    final String chatStartSender = getSender((String) data.get("sender"));
                    boolean correctPassword = Boolean.valueOf((String) data.get("correctPassword"));
                    if (correctPassword){
                        myRunnable = () -> ChatsManager.startNormalChat(getApplication(), chatStartSender, null);
                        mainHandler.post(myRunnable);

                    }else {
                        //TODO: Tell user that their password was incorrect
                    }
                    break;

            }
        }
    }

    private String getSender(String sender){
        return sender.contains(ChatsManager.SPLITTER) ? sender.replace(ChatsManager.SPLITTER, " ") : sender;
    }



    @Override
    public void onMessageReceived(Sender senderData, String message) {
        notification(getApplicationContext(), "Messenger - " + senderData.userName, message, senderData.isSecretMessage);
    }
}
