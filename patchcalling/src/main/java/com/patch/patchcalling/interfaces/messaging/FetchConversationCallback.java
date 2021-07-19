package com.patch.patchcalling.interfaces.messaging;

import com.patch.patchcalling.retrofitresponse.messaging.conversations.FetchConversationResponse;

import java.util.List;

/**
 * Created by Shivam Sharma on 10-08-2019.
 */
public interface FetchConversationCallback {
    void onConversationsResponse(List<FetchConversationResponse> conversationList);
    void onFailure(int error);
}
