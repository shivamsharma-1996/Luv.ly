package com.patch.patchcalling.models;

import com.patch.patchcalling.interfaces.OutgoingCallResponse;

import org.json.JSONObject;

/**
 * Created by sanyamjain on 23/03/19.
 */

public class IosAPF {
    String calleeCC = "";
    String calleePhone = "";
    String cuid = "";
    String context = "";
    JSONObject callOptions = new JSONObject();
    OutgoingCallResponse outgoingCallResponse = null;

    public IosAPF(String calleeCC, String calleePhone, String cuid, String context, JSONObject callOptions, OutgoingCallResponse outgoingCallResponse) {
        this.calleeCC = calleeCC;
        this.calleePhone = calleePhone;
        this.cuid = cuid;
        this.context = context;
        this.callOptions = callOptions;
        this.outgoingCallResponse = outgoingCallResponse;
    }

    public String getCalleeCC() {
        return calleeCC;
    }

    public String getCalleePhone() {
        return calleePhone;
    }

    public String getCuid() {
        return cuid;
    }

    public String getContext() {
        return context;
    }

    public JSONObject getCallOptions() {
        return callOptions;
    }

    public OutgoingCallResponse getOutgoingCallResponse() {
        return outgoingCallResponse;
    }
}
