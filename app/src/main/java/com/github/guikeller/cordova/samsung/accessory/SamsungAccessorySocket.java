package com.github.guikeller.cordova.samsung.accessory;

import android.util.Log;

import com.samsung.android.sdk.accessory.SASocket;

import java.nio.charset.StandardCharsets;

/**
 * The underlying protocol, used to sends and receives messages
 * @author guikeller
 */
public class SamsungAccessorySocket extends SASocket {

    private static final String TAG = SamsungAccessorySocket.class.getSimpleName();
    private static final int CHANNEL_ID = 7219;

    private static SamsungAccessoryMessageListener messageListener;

    public SamsungAccessorySocket() {
        super(SamsungAccessorySocket.class.getName());
        Log.i(TAG,"constructor");
    }

    @Override
    public void onError(int channelId, String errorMessage, int errorCode) {
        Log.e(TAG, "onError: "+channelId+" / "+errorMessage+" / "+errorCode);
    }

    @Override
    public void onReceive(int channelId, byte[] data) {
        Log.i(TAG, "onReceive :: data: "+new String(data, StandardCharsets.UTF_8));
        if (CHANNEL_ID == channelId && messageListener != null){
            String msgReceived = new String(data, StandardCharsets.UTF_8);
            messageListener.messageReceived(msgReceived);
        }
    }

    @Override
    protected void onServiceConnectionLost(int reason) {
        Log.w(TAG, "Connection Terminated :: reason: "+reason);
    }

    protected void registerMessageListener(SamsungAccessoryMessageListener listener){
        Log.i(TAG,"registerMessageListener");
        this.messageListener = listener;
    }

}
