package com.patch.patchcalling.javaclasses;

public class MissedCallNotificationAction {
    /*public interface ActionType {
        // Notification was tapped on.
        public static int Opened = 10001;
        public static int ActionTapped = 10002;
    }*/

    // The type of the notification action
    //public ActionType actionType;

    // The ID associated with the button tapped. null when the actionType is NotificationTapped or InAppAlertClosed
    public String actionID;

    public String actionLabel;
}
