package com.github.bettehem.messenger.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import com.github.bettehem.androidtools.Preferences;
import com.github.bettehem.messenger.MainActivity;
import com.github.bettehem.messenger.R;
import com.github.bettehem.messenger.tools.listeners.ChatItemListener;
import com.github.bettehem.messenger.tools.managers.ChatsManager;

public class ChatScreen extends Fragment implements View.OnClickListener {

    private static final int CHAT_VIEW = 0;
    private static final int PENDING_VIEW = 1;
    private static final int REQUEST_VIEW = 2;



    private View view;
    private String username;
    private ViewFlipper chatViews;

    private AppCompatEditText passwordEditText;
    private AppCompatButton acceptRequestButton;
    private AppCompatButton rejectRequestButton;

    private AppCompatImageButton emojiSelectionButton;
    private AppCompatEditText messageEditText;
    private AppCompatButton sendMessageButton;
    private RecyclerView messageRecycler;

    private ChatItemListener listener;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.chat_screen, container, false);

        //save the current fragment
        MainActivity.currentFragment = this;
        Preferences.saveString(getActivity(), "currentFragment", "ChatScreen");

        //set the current username
        username = Preferences.loadString(getActivity(), "username", "CurrentChat");

        setup();

        checkStatus();

        return view;
    }

    private void setup(){
        toolbars();
        viewFlippers();
        editTexts();
        buttons();
        recyclers();
    }

    private void toolbars(){
        Preferences.saveString(getActivity(), "defaultToolbarText", MainActivity.toolbar.getTitle().toString());
        MainActivity.toolbar.setTitle(username);
    }

    private void viewFlippers(){
        chatViews = (ViewFlipper) view.findViewById(R.id.chatScreenViewFlipper);
        chatViews.setDisplayedChild(CHAT_VIEW);
    }

    private void buttons(){
        acceptRequestButton = (AppCompatButton) view.findViewById(R.id.acceptChatRequestButton);
        rejectRequestButton = (AppCompatButton) view.findViewById(R.id.rejectChatRequestButton);
        emojiSelectionButton = (AppCompatImageButton) view.findViewById(R.id.chatScreenEmojiSelectButton);
        sendMessageButton = (AppCompatButton) view.findViewById(R.id.chatScreenSendMessageButton);

        acceptRequestButton.setOnClickListener(this);
        rejectRequestButton.setOnClickListener(this);
        emojiSelectionButton.setOnClickListener(this);
        sendMessageButton.setOnClickListener(this);
    }

    private void editTexts(){
        passwordEditText = (AppCompatEditText) view.findViewById(R.id.chatScreenPasswordEditText);
        messageEditText = (AppCompatEditText) view.findViewById(R.id.chatScreenMessageEditText);
    }

    private void recyclers(){
        messageRecycler = (RecyclerView) view.findViewById(R.id.chatScreenMessageRecycler);

    }





    private void checkStatus(){
        String status = Preferences.loadString(getActivity(), "chatStatus", username);
        switch (status){
            case "normal":
                //show normal chat
                chatViews.setDisplayedChild(CHAT_VIEW);
                break;

            case "pending":
                //show pending text
                chatViews.setDisplayedChild(PENDING_VIEW);
                AppCompatTextView textView = (AppCompatTextView) view.findViewById(R.id.pendingTextView);
                textView.setText("Waiting for " + username + " to answer your chat request...");
                break;

            case "chatRequest":
                //show chat request screen
                chatViews.setDisplayedChild(REQUEST_VIEW);
                break;
        }
    }

    public void setChatItemListener(ChatItemListener chatItemListener){
        listener = chatItemListener;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.acceptChatRequestButton:
                //TODO: Add check for empty password
                ChatsManager.responseToRequest(getActivity(), true, username, passwordEditText.getText().toString(), listener);
                break;

            case R.id.rejectChatRequestButton:
                ChatsManager.responseToRequest(getActivity(), false, username, "", listener);
                break;

            case R.id.chatScreenSendMessageButton:
                ChatsManager.sendMessage(getActivity(), username, messageEditText.getText().toString());
                break;
        }
    }
}
