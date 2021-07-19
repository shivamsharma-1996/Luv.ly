package com.patch.patchcalling.interfaces.messaging;

import com.patch.patchcalling.retrofitresponse.messaging.calllogs.CallLogsResponse;

import java.util.List;

/**
 * Created by Shivam Sharma on 09-08-2019.
 */
public interface FetchCallLogsCallback {
    void onCallLogsResponse(List<CallLogsResponse> callLogsResponse);
    void onFailure(int error);
}
