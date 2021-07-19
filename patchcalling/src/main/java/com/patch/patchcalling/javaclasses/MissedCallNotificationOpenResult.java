package com.patch.patchcalling.javaclasses;

public class MissedCallNotificationOpenResult {
    public MissedCallNotificationAction action;
    public CallDetails callDetails;

    public static class CallDetails{
        public String callerCuid;
        public String calleeCuid;
        public String callContext;
    }
}


