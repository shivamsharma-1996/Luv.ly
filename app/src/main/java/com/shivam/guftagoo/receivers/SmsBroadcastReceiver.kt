package com.shivam.guftagoo.receivers;

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class SmsBroadcastReceiver : BroadcastReceiver() {
    lateinit var smsBroadcastReceiverListener: SmsBroadcastReceiverListener

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { intent ->
            if (intent.action == SmsRetriever.SMS_RETRIEVED_ACTION) {
                val extras = intent.extras
                if (extras != null) {
                    val smsRetreiverStatus = extras.get(SmsRetriever.EXTRA_STATUS) as Status

                    when (smsRetreiverStatus.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            val messageIntent =
                                extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT);
                            smsBroadcastReceiverListener.onSuccess(messageIntent)
                        }
                        CommonStatusCodes.TIMEOUT -> {
                            smsBroadcastReceiverListener.onFailure();
                        }
                    }
                }
            }
        }
    }
}

interface SmsBroadcastReceiverListener {
    fun onSuccess(intent: Intent?)
    fun onFailure()
}
