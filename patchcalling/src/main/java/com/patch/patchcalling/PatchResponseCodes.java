package com.patch.patchcalling;


import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.patch.patchcalling.PatchResponseCodes.FetchConversationCallback.OnResponse.SUCCESS_FETCHING_CONVERSATION;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingMessageCallback.OnFailure.ERR_CUID_ALREADY_CONNECTED_ELSEWHERE;

/**
 * Created by Shivam Sharma on 27-07-2019.
 */

public interface PatchResponseCodes {

    int ERR_NETWORK_NOT_AVAILABLE = 100;
    int ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM = 101;
    int SUCCESS_SDK_CONNECTED = 102;
    int ERR_CONNECTION_FAILED =103;
    int ERR_MESSAGING_MUST_BE_INITIALIZED = 104;
    int FAILURE_NO_ACTIVE_PATCH_SESSION = 105;

    //Handle these response-codes for PatchInitResponse callback, while SDK is initializing
    interface PatchInitCallback {

        @IntDef({OnSuccess.SUCCESS_PATCH_SDK_INITIALIZED})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnSuccess {
            int SUCCESS_PATCH_SDK_INITIALIZED = 1001;
        }

        @IntDef({OnFailure.ERR_PATCH_SDK_NOT_INITIALIZED, OnFailure.ERR_INCORRECT_PARAMS_IN_SDK_INITIALIZATION,
                OnFailure.ERR_PATCH_SERVER_UNREACHABLE, OnFailure.ERR_INVALID_CUID, OnFailure.ERR_INVALID_PHONE_NUMBER,
                OnFailure.ERR_INVALID_CC, OnFailure.ERR_NAME_LENGTH_EXCEEDED_BY_25, OnFailure.ERR_EITHER_CC_AND_PHONE_OR_CUID_NEEDED,
                OnFailure.ERR_INVALID_ACTIVITY_CONTEXT, OnFailure.ERR_BOTH_ACCOUNTID_AND_APIKEY_REQUIRED, OnFailure.ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP = 2000;
            int ERR_PATCH_SDK_NOT_INITIALIZED = 2001;
            int ERR_INCORRECT_PARAMS_IN_SDK_INITIALIZATION = 2002;
            int ERR_PATCH_SERVER_UNREACHABLE = 2003;
            int ERR_INVALID_CUID = 2004;
            int ERR_INVALID_PHONE_NUMBER = 2005;
            int ERR_INVALID_CC = 2006;
            int ERR_NAME_LENGTH_EXCEEDED_BY_25 = 2007;
            int ERR_EITHER_CC_AND_PHONE_OR_CUID_NEEDED = 2008;
            int ERR_INVALID_ACTIVITY_CONTEXT = 2009;
            int ERR_BOTH_ACCOUNTID_AND_APIKEY_REQUIRED = 2010;
            int ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE = 2011;
            int ERR_INVALID_STICKY_PARAMS = 2012;
            int ERR_PATCH_STICKY_SERVICE_FORCE_STOPPED = 2013;
            int ERR_INVALID_SESSION_TO_START_STICKY_SERVICE = 2014;
            int ERR_AUTH_FAILURE_TO_START_STICKY_SERVICE = 2015;
            int ERR_INVALID_LENGH_CUID = 2016;
            int ERR_CUID_CAN_NOT_HAVE_SPECIAL_CHARS_BETWEEN_NUMBERS = 2017;
            int ERR_INVALID_FCM_TOKEN = 2018;
            int ERR_INVALID_ACCOUNTID_OR_APIKEY = 2019;
        }
    }

    //Handle these response-codes for IncomingCallResponse callback, while receiving a call
    interface IncomingCallCallback {
        @IntDef({CallStatus.CALL_OVER, CallStatus.CALL_ANSWERED, CallStatus.CALL_DECLINED, CallStatus.CALL_MISSED})
        @Retention(RetentionPolicy.SOURCE)
        @interface CallStatus {
            int CALL_OVER = 3007;
            int CALL_ANSWERED = 3008;
            int CALL_DECLINED = 3009;
            int CALL_MISSED = 30010;
        }

        @IntDef({OnSuccess.SUCCESS_CALL_INCOMING})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnSuccess {
            int SUCCESS_CALL_INCOMING = 4002;
        }

        @IntDef({})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {

        }
    }

    //Handle these response-codes for OutgoingCallResponse callback, while placing call from SDK
    interface OutgoingCallCallback {
        @IntDef({CallStatus.CALL_OVER, CallStatus.CALL_ANSWERED, CallStatus.CALL_DECLINED, CallStatus.CALL_MISSED})
        @Retention(RetentionPolicy.SOURCE)
        @interface CallStatus {
            int CALL_OVER = 3001;
            int CALL_ANSWERED = 3002;
            int CALL_DECLINED = 3003;
            int CALL_MISSED = 3004;
            int CALLEE_BUSY_ON_ANOTHER_CALL = 3005;
        }

        @IntDef({OnSuccess.CALL_PLACED})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnSuccess {
            int CALL_PLACED = 4001;
        }

        @IntDef({OnFailure.ERR_MICROPHONE_PERMISSION_NOT_GRANTED, OnFailure.FAILURE_INTERNET_LOST_AT_RECEIVER_END, OnFailure.ERR_CONTACT_NOT_REACHABLE,
                OnFailure.ERR_NUMBER_NOT_EXISTS, OnFailure.ERR_BAD_NETWORK, OnFailure.ERR_MISSING_CLI, OnFailure.ERR_MISSING_CC_OR_PHONE_IN_CLI,
                OnFailure.ERR_INVALID_PHONE_NUMBER_LENGTH, OnFailure.ERR_INVALID_CC_LENGTH, OnFailure.ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI,
                OnFailure.ERR_TAGS_COUNT_EXCEEDED_BY_10, OnFailure.ERR_TAG_LENGTH_EXCEEDED_BY_32, OnFailure.ERR_INVALID_FORMAT_OF_WEBHOOK,
                OnFailure.ERR_VAR_LENGTH_EXCEEDED_BY_128, OnFailure.ERR_SOMETHING_WENT_WRONG, OnFailure.FAILURE_CAN_NOT_CALL_SELF,
                OnFailure.ERR_CALL_CONTEXT_REQUIRED, OnFailure.ERR_CALL_CONTEXT_LENGTH_EXCEEDED_BY_64, OnFailure.ERR_INVALID_ACTIVITY_CONTEXT,
                OnFailure.ERR_CALL_OPTIONS_REQUIRED})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int ERR_MICROPHONE_PERMISSION_NOT_GRANTED = 5001;
            int FAILURE_INTERNET_LOST_AT_RECEIVER_END = 5002;
            int ERR_CONTACT_NOT_REACHABLE = 5003;
            int ERR_NUMBER_NOT_EXISTS = 5004;
            int ERR_BAD_NETWORK = 5005;
            int ERR_MISSING_CLI = 5006;
            int ERR_MISSING_CC_OR_PHONE_IN_CLI = 5007;
            int ERR_INVALID_CC_LENGTH = 5008;
            int ERR_INVALID_PHONE_NUMBER_LENGTH = 5009;
            int ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI = 5010;
            int ERR_TAGS_COUNT_EXCEEDED_BY_10 = 5011;
            int ERR_TAG_LENGTH_EXCEEDED_BY_32 = 5012;
            int ERR_INVALID_FORMAT_OF_WEBHOOK = 5013;
            int ERR_INVALID_CALLEE_CUID = 5014;
            int ERR_VAR_LENGTH_EXCEEDED_BY_128 = 5015;
            int ERR_SOMETHING_WENT_WRONG = 5016;
            int ERR_CALLEE_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN = 5017;
            int ERR_CALLER_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN = 5018;
            int ERR_UNAUTHORIZED_CLI = 5019;
            int ERR_EMPTY_VERIFIED_CLI_LIST = 5020;
            int ERR_CC_PHONE_MISSING_FOR_CUID_IN_PSTN_CALL = 5021;
            int ERR_INVALID_CALL_TOKEN = 5022;
            int ERR_CUID_ALREADY_CONNECTED_ELSEWHERE = 5023;
            int ERR_BOTH_CC_AND_PHONE_REQUIRED = 5024;
            int ERR_MISSING_CC_PHONE_TO_MAKE_PSTN_CALL = 5025;
            int FAILURE_CAN_NOT_CALL_SELF = 5026;
            int ERR_CALL_CONTEXT_REQUIRED = 5027;
            int ERR_CALL_CONTEXT_LENGTH_EXCEEDED_BY_64 = 5028;
            int ERR_INVALID_ACTIVITY_CONTEXT = 5029;
            int ERR_CALL_OPTIONS_REQUIRED = 5030;
            int ERR_WHILE_MAKING_VOIP_CALL = 5031;
        }
    }

    //Handle these response-codes for NotificationResponse callback, while sending notification from SDK
    interface NotificationCallback {
        interface OnResponse {
        }

        @IntDef({OnFailure.ERR_NOTIFICATION_FAILED, OnFailure.ERR_INCORRECT_NOTIFICATION_PARAMS_IN_SDK_INITIALIZATION,
                OnFailure.ERR_SOMETHING_WENT_WRONG})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int ERR_NOTIFICATION_FAILED = 6001;
            int ERR_NOTIFICATION_INITIALIZATION_REQUIRED = 6002;
            int ERR_INCORRECT_NOTIFICATION_PARAMS_IN_SDK_INITIALIZATION = 6003;
            int ERR_SOMETHING_WENT_WRONG = 6004;
            int ERR_CUID_ALREADY_CONNECTED_ELSEWHERE = 6005;
        }
    }

    //Handle these response-codes for MessageInitResponse callback, while initializing messaging
    interface MessageInitCallback {
        @IntDef({OnResponse.SUCCESS_MESSAGING_INITIALIZED})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnResponse {
            int SUCCESS_MESSAGING_INITIALIZED = 70010;
        }

        @IntDef({OnFailure.ERR_MISSING_MERCHANT, OnFailure.ERR_MISSING_ENABLE_CALL, OnFailure.ERR_MISSING_RECEIVER_CUID,
                OnFailure.ERR_INVALID_RECEIVER_CUID, OnFailure.ERR_MISSING_ACCOUNTID_OR_APIKEY, OnFailure.ERR_MESSAGING_NOT_INITIALIZED})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int ERR_MISSING_MERCHANT = 70020;
            int ERR_MISSING_ENABLE_CALL = 70021;
            int ERR_MISSING_RECEIVER_CUID = 70022;
            int ERR_INVALID_RECEIVER_CUID = 70023;
            int ERR_MISSING_ACCOUNTID_OR_APIKEY = 70024;
            int ERR_MESSAGING_NOT_INITIALIZED = 70025;
        }
    }

    interface OutgoingMessageCallback {
        @IntDef({OnResponse.SUCCESS_MESSAGING_SENT})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnResponse {
            int SUCCESS_MESSAGING_SENT = 80010;
        }

        @IntDef({OnFailure.ERR_MESSAGE_FAILED, OnFailure.ERR_MISSING_CUID, OnFailure.ERR_MISSING_MESSAGE, OnFailure.ERR_WRONG_THREAD_EXCEPTION_TO_UPDATE_UI, ERR_CUID_ALREADY_CONNECTED_ELSEWHERE})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int ERR_MESSAGE_FAILED = 80020;
            int ERR_MISSING_CUID = 80021;
            int ERR_MISSING_MESSAGE = 80022;
            int ERR_WRONG_THREAD_EXCEPTION_TO_UPDATE_UI = 80023;
            int ERR_CUID_ALREADY_CONNECTED_ELSEWHERE = 80024;
        }
    }

    interface FetchConversationCallback {
        @IntDef({SUCCESS_FETCHING_CONVERSATION})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnResponse {
            int SUCCESS_FETCHING_CONVERSATION = 90000;
        }
        @IntDef({OnFailure.ERR_FETCHING_CONVERSATION})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int ERR_FETCHING_CONVERSATION = 90010;
        }
    }

    interface TagsCallback {
        @IntDef({OnFailure.FAILURE_SESSION_EXPIRED_RESTART_THE_APP, OnFailure.FAILURE_WHIILE_ADDING_TAG, OnFailure.FAILURE_WHIILE_REMOVING_TAG})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int FAILURE_SESSION_EXPIRED_RESTART_THE_APP = 7001;
            int FAILURE_WHIILE_ADDING_TAG = 7002;
            int FAILURE_WHIILE_REMOVING_TAG = 7003;
        }
    }

    interface FetchChatMessageCallback {
        @IntDef({OnResponse.SUCCESS_FETCHING_CHAT_MESSAGES})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnResponse {
            int SUCCESS_FETCHING_CHAT_MESSAGES = 90015;
        }
        @IntDef({OnFailure.ERR_FETCHING_CHAT_MESSAGES})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int ERR_FETCHING_CHAT_MESSAGES = 90020;
        }
    }

    interface FetchUnreadMessageCallback {
        @IntDef({OnResponse.SUCCESS_FETCHING_UNREAD_COUNT})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnResponse {
            int SUCCESS_FETCHING_UNREAD_COUNT = 90025;
        }
        @IntDef({OnFailure.ERR_FETCHING_UNREAD_COUNT})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int ERR_FETCHING_UNREAD_COUNT = 90030;
        }
    }

    interface MarkMessageSeenCallback {
        @IntDef({OnSuccess.SUCCESS_MESSAGES_MARKED_SEEN})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnSuccess {
            int SUCCESS_MESSAGES_MARKED_SEEN = 90040;
        }

        @IntDef({OnFailure.ERR_MARK_MESSAGES_SEEN})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int ERR_MARK_MESSAGES_SEEN = 90050;
        }
    }

    interface FetchCallLogsCallback {
        @IntDef({OnSuccess.SUCCESS_FETCHING_CALL_LOG})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnSuccess {
            int SUCCESS_FETCHING_CALL_LOG = 90055;
        }
        @IntDef({OnFailure.ERR_FETCHING_CALL_LOG})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int ERR_FETCHING_CALL_LOG = 90060;
        }
    }

    interface PatchFcmResponse {

        @IntDef({OnSuccess.SUCCESS_FCM_TOKEN_UPDATED})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnSuccess {
            int SUCCESS_FCM_TOKEN_UPDATED = 11001;
        }

        @IntDef({OnFailure.ERR_FCM_TOKEN_REQUIRED})
        @Retention(RetentionPolicy.SOURCE)
        @interface OnFailure {
            int ERR_FCM_TOKEN_REQUIRED = 21001;
            int ERR_FAILED_TO_UPDATE_FCM_TOKEN = 21002;
            int ERR_INTERNAL_SERVER_ERROR = 21004;
        }
    }

}
