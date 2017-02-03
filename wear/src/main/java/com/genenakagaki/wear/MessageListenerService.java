package com.genenakagaki.wear;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by gene on 2/3/17.
 */

public class MessageListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals("/weather")) {
            final String message = new String(messageEvent.getData());
            Log.d("myTag", "Message path received on watch is: " + messageEvent.getPath());
            Log.d("myTag", "Message received on watch is: " + message);

            // Broadcast message to wearable activity for display
//            Intent messageIntent = new Intent();
//            messageIntent.setAction(Intent.ACTION_SEND);
//            messageIntent.putExtra("message", message);
//            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }
}
