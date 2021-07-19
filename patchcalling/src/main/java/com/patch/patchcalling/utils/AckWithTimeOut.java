package com.patch.patchcalling.utils;

import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Ack;

public class AckWithTimeOut implements Ack {

private Timer timer;
private long timeOut = 0;
private boolean called = false;

public AckWithTimeOut() {
}

public AckWithTimeOut(long timeout_after) {
    if (timeout_after <= 0)
        return;
    this.timeOut = timeout_after;
    startTimer();
}

public void startTimer() {
    timer = new Timer();
    timer.schedule(new TimerTask() {
        @Override
        public void run() {
            callback("No Ack");
        }
    }, timeOut);
}

public void resetTimer() {
    if (timer != null) {
        timer.cancel();
        startTimer();
    }
}

public void cancelTimer() {
    if (timer != null)
        timer.cancel();
}

void callback(Object... args) {
    if (called) return;
    called = true;
    cancelTimer();
    call(args);
}

@Override
public void call(Object... args) {

}
}