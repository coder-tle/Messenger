package com.github.bettehem.messenger.tools.managers;

import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
//import android.util.Base64;

import com.github.bettehem.androidtools.Preferences;
import com.github.bettehem.androidtools.misc.Time;
import com.github.bettehem.androidtools.notification.CustomNotification;
import com.github.bettehem.messenger.MainActivity;
import com.github.bettehem.messenger.R;
import com.github.bettehem.messenger.fragments.ChatScreen;
import com.github.bettehem.messenger.objects.ChatPreparerInfo;
import com.github.bettehem.messenger.objects.ChatRequestResponseInfo;
import com.github.bettehem.messenger.tools.items.ChatItem;
import com.github.bettehem.messenger.tools.items.MessageItem;
import com.github.bettehem.messenger.tools.listeners.ChatItemListener;
import com.github.bettehem.messenger.tools.listeners.HttpPostListener;
import com.github.bettehem.messenger.tools.listeners.MessageItemListener;
import com.github.bettehem.messenger.tools.ui.CustomNotificationKt;
import com.github.bettehem.messenger.tools.users.Sender;
import com.github.bettehem.messenger.tools.users.UserProfile;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

// TODO: 3/9/18 Add more comments, so it's easier to understand what's happening
public abstract class ChatsManager {
    public static final String SPLITTER = "_kjhas3ng7vb3b3a-XYZYX-di8x888xgwbkwv0vaw3pxds22_";

    public static ArrayList<ChatItem> getChatItems(Context context) {

        //Get current chat items
        ArrayList<ChatItem> chatItems = new ArrayList<>();
        int chatAmount = Preferences.loadInt(context, "chatsAmount", "ChatDetails");
        for (int i = 0; i < chatAmount; i++) {
            String[] item = Preferences.loadStringArray(context, "chatItem_" + i, "ChatDetails");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(Long.valueOf(item[2]));
            chatItems.add(new ChatItem(item[0], item[1], new Time(calendar)));
        }
        return chatItems;
    }

    /**
     * Used to format the custom time object to a string that shows the hour and minute, date, or if the time was yesterday, it shows yesterday.
     *
     * @param time The time object that should be formatted
     * @return Returns a String that can be used for the chat items.
     */
    public static String formatTime(Time time) {
        // FIXME: 3/15/18 doesn't work as intended
        //TODO: Remove hard-coded strings;

        String formatted;

        Time currentTime = new Time(Calendar.getInstance());

        //compare given time to the current time on the device.

        //do if the current time is newer than the formatted time
        if (Integer.valueOf(currentTime.date) > Integer.valueOf(time.date) && Integer.valueOf(currentTime.year) >= Integer.valueOf(time.year)) {
            //If the formatted time was yesterday
            if (Integer.valueOf(currentTime.date) - 1 == Integer.valueOf(time.date) && (int) Integer.valueOf(currentTime.year) == Integer.valueOf(time.year)) {
                formatted = "Yesterday";

                //if formatted time is older than yesterday
            } else {
                formatted = time.date + "." + time.month + "." + time.year.substring(2);
            }

            //do if current time is same as the formatted time
        } else {
            formatted = time.hour + ":" + time.minute;
        }

        return formatted;
    }

    /**
     * Gets the topics that the user will be subscribed to
     *
     * @param context Context is used to get the needed information from SharedPreferences
     * @return Returns a String array that contains the topics that the users need to subscribe to
     * @deprecated use TopicManager for managing topics
     */
    @Deprecated
    public static String[] getGcmTopics(Context context) {

        //check if a list of topics exists, if not, save a default list of topics and return it
        if (Preferences.fileExists(context, "FCMTopics", "xml")) {
            Preferences.saveStringArray(context, "topics", new String[]{"global"}, "FCMTopics");
            return new String[]{"global"};
        } else {
            return Preferences.loadStringArray(context, "topics", "FCMTopics");
        }
    }

    public static Sender getSenderData(Context context, String senderData) {
        String userName = getUserName(context, senderData);
        boolean isSecretMessage = Boolean.valueOf(senderData.split(SPLITTER)[1]);
        return new Sender(userName, isSecretMessage);
    }

    public static void sendHttpPost(JSONObject data, HttpPostListener listener){
        Thread thread = new Thread(){
            @Override
            public void run() {
                try {
                    HttpPost post = new HttpPost("https://fcm.googleapis.com/fcm/send");
                    StringEntity se = new StringEntity(data.toString());
                    se.setContentType(new BasicHeader("Content-Type", "application/json; UTF-8"));
                    post.setEntity(se);
                    post.setHeader("Authorization", "key=" + "AIzaSyD8C9exPq2SWMkJUcGc8ZNT8MA9b18rF4I");
                    HttpClient client = new DefaultHttpClient();
                    HttpResponse response = client.execute(post);
                    listener.onPostResponse(response);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public static void sendMessage(final Context context, final String username, final String message, MessageItemListener messageItemListener){
        //set receiver
        String receiver = EncryptionManager.createHash(Preferences.loadString(context, "encryptedUsername", username));

        //encrypt message
        String key = EncryptionManager.createHash(Preferences.loadString(context, "localEncryptedUsername", username));
        String iv = Preferences.loadString(context, "iv", username);
        String scrambledMessage = EncryptionManager.scramble(message);
        //index 0 is the iv, index 1 is the encrypted data
        ArrayList<String> encryptedData = EncryptionManager.encrypt(iv, key, scrambledMessage);

        String messageId = EncryptionManager.createHash(String.valueOf(Calendar.getInstance().getTimeInMillis()));

        JSONObject jsonObject = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            jsonObject.put("to", "/topics/" + receiver);
            data.put("type", "message");
            data.put("sender", Preferences.loadString(context, "name", ProfileManager.FILENAME));
            data.put("message", encryptedData.get(1));
            data.put("messageId", messageId);
            // TODO: 9/30/17 add support for secret messages
            data.put("isSecretMessage", "false");
            jsonObject.put("data", data);
            jsonObject.put("TTL", String.valueOf(60 * 60 * 7));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendHttpPost(jsonObject, response -> {
            if (!response.getStatusLine().getReasonPhrase().contentEquals("OK")){
                Snackbar.make(MainActivity.mainRelativeLayout, "Status: " + response.getStatusLine().getReasonPhrase(), Snackbar.LENGTH_SHORT).show();
            }
        });


        //save the sent message
        saveMessage(context, username, new MessageItem(message, messageId, new Time(Calendar.getInstance()), true));
        messageItemListener.onMessageListUpdated(context);
    }

    public static String getMessage(Context context, Sender senderData, String rawMessage) {
        //decrypt message

        String key = EncryptionManager.createHash(Preferences.loadString(context, "encryptedUsername", senderData.userName));
        //String unscrambled = EncryptionManager.unscramble(rawMessage);
        String decrypted = EncryptionManager.decrypt(Preferences.loadString(context, "iv", senderData.userName), key, rawMessage);
        return EncryptionManager.unscramble(decrypted);
    }

    public static ArrayList<MessageItem> getMessageItems(Context context, String username) {
        ArrayList<MessageItem> items = new ArrayList<>();
        int messageAmount = Preferences.loadInt(context, "messageAmount", username);

        for (int i = 0; i < messageAmount; i++){
            String[] rawMessage = Preferences.loadStringArray(context, "message_" + i, username);
            String message = rawMessage[0];
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(Long.valueOf(rawMessage[1]));
            Time time = new Time(calendar);
            boolean isOwnMessage = Boolean.valueOf(rawMessage[2]);
            String messageId = String.valueOf(Calendar.getInstance().getTimeInMillis());
            if (rawMessage.length >= 4){
                messageId = rawMessage[3];
            }
            boolean messageDelivered = false;
            if (rawMessage.length >= 5){
                messageDelivered = Boolean.valueOf(rawMessage[4]);
            }
            items.add(new MessageItem(message, messageId, time, isOwnMessage, messageDelivered));
        }
        return items;
    }

    public static void saveMessage(Context context, String username, MessageItem messageItem){
        //get current items
        ArrayList<MessageItem> items = getMessageItems(context, username);

        //add new MessageItem to list
        //if the item already exists, modify it instead
        boolean itemExists = false;
        for (int i = 0; i < items.size(); i++){
            if (items.get(i).mMessageId != null && items.get(i).mMessageId.contentEquals(messageItem.mMessageId)){
                MessageItem item = items.get(i);
                item.mMessageId = messageItem.mMessageId;
                item.mMessage = messageItem.mMessage;
                item.mMessageDelivered = messageItem.mMessageDelivered;
                item.mIsOwnMessage = messageItem.mIsOwnMessage;
                item.mTime = messageItem.mTime;
                items.set(i, item);
                itemExists = true;
            }
        }
        if (!itemExists){
            items.add(messageItem);
        }

        //save new MessageAmount
        Preferences.saveInt(context, "messageAmount", items.size(), username);

        //save items
        for (int i = 0; i < items.size(); i++){
            String message = items.get(i).mMessage;
            String messageId = items.get(i).mMessageId;
            String time = String.valueOf(items.get(i).mTime.getTimeInMillis());
            String isOwnMessage = String.valueOf(items.get(i).mIsOwnMessage);
            String messageDelivered = String.valueOf(items.get(i).mMessageDelivered);
            Preferences.saveStringArray(context, "message_" + i, new String[]{message, time, isOwnMessage, messageId, messageDelivered}, username);
        }
    }

    public static boolean usernameExists(Context context, String username) {
        boolean usernameExists = false;

        ArrayList<ChatItem> chatItems = getChatItems(context);
        for (ChatItem c : chatItems) {
            if (c.name.contentEquals(username)) {
                usernameExists = true;
            }
        }

        return usernameExists;
    }

    /**
     * WARNING!
     * Running this method is not recommended in the main thread!
     *
     * @param username the name of the person to start a chat with
     * @param password the password of the chat to be started
     */
    @WorkerThread
    public static ChatPreparerInfo prepareChat(Context context, FragmentManager fragmentManager, String username, String password) {

        //generate key from password
        String key = EncryptionManager.createHash(password);

        //encrypt local username
        String scrambledUsername = EncryptionManager.scramble(Preferences.loadString(context, "name", ProfileManager.FILENAME));
        ArrayList<String> encryptedUsername = EncryptionManager.encrypt(key, scrambledUsername);
        //String readyUsername = EncryptionManager.scramble(encryptedUsername);


        //save local username
        Preferences.saveString(context, "localEncryptedUsername", encryptedUsername.get(1), username);
        //save iv
        Preferences.saveString(context, "iv", encryptedUsername.get(0), username);


        //encrypt username
        scrambledUsername = EncryptionManager.scramble(username);
        encryptedUsername = EncryptionManager.encrypt(encryptedUsername.get(0), key, scrambledUsername);
        //readyUsername = EncryptionManager.scramble(encryptedUsername);


        //save username
        Preferences.saveString(context, "encryptedUsername", encryptedUsername.get(1), username);

        // TODO: 8/11/17 remove hard-coded strings
        //save a chat item
        if (usernameExists(context, username)) {
            editChatItem(context, username, "Pending...", new Time(Calendar.getInstance()));
        } else {
            saveChatItem(context, getChatItems(context).size(), username, "Pending...", new Time(Calendar.getInstance()));
        }

        sendRequest(context, encryptedUsername.get(0), username);

        //encrypt your own username
        UserProfile profile = ProfileManager.getProfile(context);
        String encryptedProfilename = EncryptionManager.encrypt(encryptedUsername.get(0), key, EncryptionManager.scramble(profile.name)).get(1);

        return new ChatPreparerInfo(username, "pending", encryptedProfilename, R.id.mainFrameLayout, fragmentManager);
    }

    public static void openChatScreen(Context context, String name, String chatStatus, int fragmentId, FragmentManager fragmentManager, ChatItemListener listener) {
        //Save settings for current chat
        Preferences.saveString(context, "username", name, "CurrentChat");

        //Save chat status
        setChatStatus(context, chatStatus, name);

        //open chat screen
        ChatScreen chatScreen = MainActivity.chatScreen;
        chatScreen.setChatItemListener(listener);
        fragmentManager.beginTransaction().replace(fragmentId, chatScreen).commit();
        chatScreen.checkStatus(context);

        //change viewFlipper to show fragments
        MainActivity.mainViewFlipper.setDisplayedChild(MainActivity.MAIN_FRAGMENT);
    }


    /**
     * Sends a chat request to the wanted person
     *
     * @param context  Used to get needed information from SharedPreferences.
     * @param username The chat request is sent to the given username.
     */
    private static void sendRequest(final Context context, final String iv, final String username) {
        String receiver = username.replace(" ", SPLITTER);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("to", "/topics/" + receiver);
            JSONObject data = new JSONObject();
            data.put("type", "chatRequest");
            data.put("sender", Preferences.loadString(context, "name", ProfileManager.FILENAME));
            data.put("key", Preferences.loadString(context, "localEncryptedUsername", username));
            data.put("iv", iv);
            jsonObject.put("data", data);
        }catch (JSONException e){
            e.printStackTrace();
        }

        sendHttpPost(jsonObject, response -> {
            Snackbar.make(MainActivity.mainRelativeLayout, "Status: " + response.getStatusLine().getReasonPhrase(), Snackbar.LENGTH_SHORT).show();
        });
    }


    public static void handleChatRequest(Context context, final String sender, final String key, final String iv) {
        if (Preferences.loadString(context, "currentFragment").contentEquals("ChatScreen") && Preferences.loadString(context, "username", "CurrentChat").contentEquals(sender)) {
            //User has the chat open
            // TODO: 8/11/17 finish this

        } else {
            //User doesn't have the chat open, so make a notification
            //save status
            Preferences.saveString(context, "chatStatus", "chatRequest", sender);

            if (usernameExists(context, sender)) {
                //edit the existing chat item
                editChatItem(context, sender, "New Chat Request", new Time(Calendar.getInstance()));
            } else {
                //Save a chat item for this chat
                saveChatItem(context, getChatItems(context).size(), sender, "New Chat Request", new Time(Calendar.getInstance()));
            }

            //save the username hash
            Preferences.saveString(context, "encryptedUsername", key, sender);

            //save iv
            Preferences.saveString(context, "iv", iv, sender);

            if (Preferences.loadBoolean(context, "appVisible")) {
                //if the app is visible, update the chatItems
                if (MainActivity.chatsRecyclerAdapter != null){
                    MainActivity.chatsRecyclerAdapter.setChatItems(getChatItems(context));
                }
            } else {
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("type", "chatRequest");
                intent.putExtra("username", sender);
                //TODO: Remove hard-coded strings
                CustomNotificationKt.notification(context,"Messenger Chat Request", "New chat request from " + sender, false, intent);
            }
        }
    }

    public static void responseToRequest(final Context context, final boolean acceptRequest, final String username, final String password, @Nullable ChatItemListener listener) {
        //generate key from password
        String key = EncryptionManager.createHash(password);
        //get iv
        String iv = Preferences.loadString(context, "iv", username);

        //encrypt local username
        String scrambledUsername = EncryptionManager.scramble(Preferences.loadString(context, "name", ProfileManager.FILENAME));
        ArrayList<String> encryptedUsername = EncryptionManager.encrypt(iv, key, scrambledUsername);
        //String readyUsername = EncryptionManager.scramble(encryptedUsername);

        //save encrypted username
        Preferences.saveString(context, "localEncryptedUsername", encryptedUsername.get(1), username);



        //encrypt username
        scrambledUsername = EncryptionManager.scramble(username);
        encryptedUsername = EncryptionManager.encrypt(iv, key, scrambledUsername);
        //readyUsername = EncryptionManager.scramble(encryptedUsername);

        //save username
        Preferences.saveString(context, "encryptedUsername", encryptedUsername.get(1), username);



        if (listener != null && acceptRequest) {
            listener.onRequestAccepted(username, key);
        }


        //Send response to the chat request
        String receiver = username;
        String readyUsername = encryptedUsername.get(1);
        JSONObject jsonObject = new JSONObject();
        try{
            jsonObject.put("to", "/topics/" + receiver);

            JSONObject data = new JSONObject();
            data.put("type", "requestResponse");
            data.put("sender", Preferences.loadString(context, "name", ProfileManager.FILENAME));
            data.put("requestAccepted", String.valueOf(acceptRequest));
            data.put("password", readyUsername);
            jsonObject.put("data", data);
            jsonObject.put("TTL", "30");
        }catch (JSONException e){
            e.printStackTrace();
        }
        sendHttpPost(jsonObject, response -> {
            Snackbar.make(MainActivity.mainRelativeLayout, "Request status: " + response.getStatusLine().getReasonPhrase(), Snackbar.LENGTH_SHORT).show();
        });
    }

    public static ChatRequestResponseInfo handleChatRequestResponse(Context context, boolean requestAccepted, String username, String password) {
        if (requestAccepted) {
            //get local encrypted username
            String localEncryptedUsername = Preferences.loadString(context, "localEncryptedUsername", username);

            if (localEncryptedUsername.contentEquals(password)) {
                return new ChatRequestResponseInfo(requestAccepted, true, username);
            } else {
                return new ChatRequestResponseInfo(requestAccepted, false, username);
            }

        } else {
            return new ChatRequestResponseInfo(requestAccepted, false, username);
        }
    }

    public static void startChat(final Context context, final boolean correctPassword, final String username, int fragmentId, FragmentManager fragmentManager, ChatItemListener listener) {

        //Send info to other user on starting the chat
        String receiver = EncryptionManager.createHash(Preferences.loadString(context, "encryptedUsername", username));
        String ivHash = EncryptionManager.createHash(Preferences.loadString(context, "iv", username));
        JSONObject jsonObject = new JSONObject();
        try{
            jsonObject.put("to", "/topics/" + receiver);
            JSONObject data = new JSONObject();
            data.put("type", "startChat");
            data.put("correctPassword", String.valueOf(correctPassword));
            data.put("hash", ivHash);
            data.put("sender", Preferences.loadString(context, "name", ProfileManager.FILENAME));
            jsonObject.put("data", data);
            jsonObject.put("TTL", String.valueOf(15));
        }catch (JSONException e){
            e.printStackTrace();
        }

        sendHttpPost(jsonObject, response -> {
            Snackbar.make(MainActivity.mainRelativeLayout, "Starting chat with " + username, Snackbar.LENGTH_SHORT).show();
        });

        if (correctPassword) {
            //Edit current chatItem
            //TODO: Remove hard-coded strings
            editChatItem(context, username, "Chat Started", new Time(Calendar.getInstance()));

            //open the chat screen if user is not in chat list
            if (!Preferences.loadString(context, "currentFragment").contentEquals("")) {
                openChatScreen(context, username, "normal", fragmentId, fragmentManager, listener);
            }
        } else {
            //TODO: Ask user if they want to delete the chat [ALERT_DIALOG]
        }
    }

    public static void startNormalChat(Context context, String username, ChatItemListener listener) {
        //TODO: Remove hard-coded strings
        //Edit the chatItem
        editChatItem(context, username, "Chat Started", new Time(Calendar.getInstance()));

        //TODO: set the chat status

        //update chat item's list
        MainActivity.chatsRecyclerAdapter.setChatItems(getChatItems(context));

        MainActivity.toolbar.setSubtitle("New Chat");
        MainActivity.newChatButton.hide();
        openChatScreen(context, username, "normal", R.id.mainFrameLayout, MainActivity.fragmentManager, listener);
    }

    public static void editChatItem(Context context, String username, String newMessage, Time newTime) {
        //Get current chat items
        ArrayList<ChatItem> chatItems = new ArrayList<>();
        int chatAmount = Preferences.loadInt(context, "chatsAmount", "ChatDetails");
        for (int i = 0; i < chatAmount; i++) {
            String[] item = Preferences.loadStringArray(context, "chatItem_" + i, "ChatDetails");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(Long.valueOf(item[2]));
            chatItems.add(new ChatItem(item[0], item[1], new Time(calendar)));
        }

        //Edit the items
        for (int i = 0; i < chatItems.size(); i++) {
            if (chatItems.get(i).name.contentEquals(username)) {
                chatItems.set(i, new ChatItem(username, newMessage, newTime));
            }
        }

        //delete the old items
        Preferences.deleteAllValues(context, "ChatDetails");

        //sort items so that latest item is on top
        Collections.sort(chatItems);
        //Collections.reverse(chatItems);

        //save the items
        for (ChatItem c : chatItems) {
            saveChatItem(context, chatItems.size(), c.name, c.message, c.time);
        }

        //update list if app is visible
        if (Preferences.loadBoolean(context, "appVisible")){
            MainActivity.chatItemListener.onChatItemListUpdated(getChatItems(context));
        }
    }


    public static void sendDeliveryReport(Context context, String username, String messageId){
        String receiver = EncryptionManager.createHash(Preferences.loadString(context, "encryptedUsername", username));
        JSONObject jsonObject = new JSONObject();
        try{
            jsonObject.put("to", "/topics/" + receiver);
            JSONObject data = new JSONObject();
            data.put("type", "deliveryReport");
            data.put("sender", Preferences.loadString(context, "name", ProfileManager.FILENAME));
            data.put("messageId", messageId);
            jsonObject.put("data", data);
            jsonObject.put("TTL", String.valueOf(10));
        }catch (JSONException e){
            e.printStackTrace();
        }

        sendHttpPost(jsonObject, response -> {
            //Snackbar.make(MainActivity.mainRelativeLayout, "Status: " + response.getStatusLine().getReasonPhrase(), Snackbar.LENGTH_SHORT).show();
        });
    }

    public static MessageItem saveDeliveryReport(Context context, String username, String messageId){
        ArrayList<MessageItem> messageItems = getMessageItems(context, username);

        MessageItem deliveredItem = new MessageItem("null", messageId, new Time(Calendar.getInstance()), true);

        for (MessageItem item : messageItems){
            if (item.mMessageId.contentEquals(messageId)){
                item.setMessageDelivered(true);
                saveMessage(context, username, item);
                deliveredItem = item;
            }
        }
        return deliveredItem;
    }

    //Private methods
    //----------------------------------------------------------------------------------------------

    private static Time getMessageTime(String messageTime) {
        if (messageTime.contentEquals("")) {
            return new Time(Calendar.getInstance());
        } else {
            String[] messageTimeArray = messageTime.split(SPLITTER);
            return new Time(Integer.valueOf(messageTimeArray[0]), Integer.valueOf(messageTimeArray[1]), Integer.valueOf(messageTimeArray[2]), Integer.valueOf(messageTimeArray[3]), Integer.valueOf(messageTimeArray[4]), Integer.valueOf(messageTimeArray[5]));
        }
    }

    private static String getUserName(Context context, String senderData) {
        String user_raw = senderData.split(SPLITTER)[0];
        //String user_unscrambled = EncryptionManager.unscramble(user_raw);
        //String userName = EncryptionManager.unscramble(EncryptionManager.decrypt(Preferences.loadString(context, user_raw, "ChatsConfig"), user_unscrambled));

        return user_raw;
    }

    private static void setChatStatus(Context context, String status, String username) {
        Preferences.saveString(context, "chatStatus", status, username);
    }

    private static void saveChatItem(Context context, int chatItemsAmount, String username, String message, Time time) {

        //Get current chat items
        ArrayList<ChatItem> chatItems = new ArrayList<>();
        int chatAmount = chatItemsAmount;
        for (int i = 0; i < chatAmount; i++) {
            String[] item = Preferences.loadStringArray(context, "chatItem_" + i, "ChatDetails");
            if (item.length == 3){
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(Long.valueOf(item[2]));
                chatItems.add(new ChatItem(item[0], item[1], new Time(calendar)));
            }
        }


        //Add new item to chatItems
        chatItems.add(new ChatItem(username, message, time));


        //save amount of chats
        Preferences.saveInt(context, "chatsAmount", chatItems.size(), "ChatDetails");

        //Save chat items
        for (int i = 0; i < chatItems.size(); i++) {
            String[] item = new String[]{chatItems.get(i).name, chatItems.get(i).message, String.valueOf(chatItems.get(i).time.getTimeInMillis())};
            Preferences.saveStringArray(context, "chatItem_" + i, item, "ChatDetails");
        }
    }
}
