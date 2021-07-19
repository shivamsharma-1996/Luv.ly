package com.patch.patchcalling.interfaces.messaging;

import com.patch.patchcalling.retrofitresponse.messaging.messages.ChatMessages;

import java.util.List;

/**
 * Created by Shivam Sharma on 10-08-2019.
 */
public interface FetchChatMessageCallback {
    void onChatMessagesResponse(List<ChatMessages> chatMessagesList);
    void onFailure(int error);
}
