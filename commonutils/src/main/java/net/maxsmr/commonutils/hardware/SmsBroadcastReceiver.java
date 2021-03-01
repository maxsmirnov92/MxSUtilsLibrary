package net.maxsmr.commonutils.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static net.maxsmr.commonutils.SdkVersionsKt.isAtLeastMarshmallow;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_SMS_RECEIVED = SmsBroadcastReceiver.class.getSimpleName() + ".ACTION_SMS_RECEIVED";
    public static final String EXTRA_SMS_DATA = SmsBroadcastReceiver.class.getSimpleName() + ".EXTRA_SMS_DATA";

    private final Set<String> senders = new LinkedHashSet<>();

    public SmsBroadcastReceiver() {
    }

    public SmsBroadcastReceiver(Collection<String> senders) {
        this.senders.addAll(senders);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equalsIgnoreCase(intent.getAction())) {
            SmsMessage smsMessage = get(intent);
            if (isFromSpecifiedSender(smsMessage)) {
                notify(context, smsMessage);
            }
        }
    }

    private boolean isFromSpecifiedSender(@Nullable SmsMessage smsMessage) {
        return smsMessage != null && (senders.isEmpty() || senders.contains(smsMessage.getOriginatingAddress()));
    }

    private void notify(Context context, SmsMessage sms) {
        Intent broadcast = new Intent();
        broadcast.setAction(ACTION_SMS_RECEIVED);
        broadcast.putExtra(EXTRA_SMS_DATA, SmsData.fromMessage(sms));
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast);
    }


    @Nullable
    private SmsMessage get(Intent intent) {
        SmsMessage result = null;
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[])bundle.get("pdus");
            if (pdus != null) {
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    if (isAtLeastMarshmallow()) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], intent.getStringExtra("format"));
                    } else {
                        //noinspection deprecation
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    }
                }
                if (messages.length >= 1) {
                    result = messages[0];
                }
            }
        }
        return result;
    }

    public static class SmsData implements Serializable {

        public String serviceCenterAddress;

        public String originatingAddress;

        public String body;

        public String emailBody;

        @Nullable
        public static SmsData fromMessage(@Nullable SmsMessage message) {
            if (message != null) {
                SmsData data = new SmsData();
                data.serviceCenterAddress = message.getServiceCenterAddress();
                data.originatingAddress = message.getOriginatingAddress();
                data.body = message.getDisplayMessageBody();
                data.emailBody = message.getEmailBody();
                return data;
            }
            return null;
        }

        @Override
        @NotNull
        public String toString() {
            return "SmsData{" +
                    "serviceCenterAddress='" + serviceCenterAddress + '\'' +
                    ", originatingAddress='" + originatingAddress + '\'' +
                    ", body='" + body + '\'' +
                    ", emailBody='" + emailBody + '\'' +
                    '}';
        }
    }
}
