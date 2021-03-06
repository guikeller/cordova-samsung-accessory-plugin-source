package com.github.guikeller.cordova.samsung.accessory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * CordovaPlugin entry point for the SamsungAccessory implementation
 * The plugin is the 'consumer', meaning that the 'tizen' device has to provide the 'provider'.
 */
public class CordovaSamsungAccessoryPlugin extends CordovaPlugin {

    private static final String TAG = CordovaSamsungAccessoryPlugin.class.getSimpleName();

    private SamsungAccessoryServiceConnection serviceConnection;
    private CordovaInterface cordovaInterface;
    private CallbackContext callbackContext;
    private Intent intent;


    public CordovaSamsungAccessoryPlugin(){
        super();
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.i(TAG, "initialize");
        super.initialize(cordova, webView);
        this.cordovaInterface = cordova;
    }

    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.i(TAG, "execute :: actions/args: " + action + "/" + args);
        this.cordovaInterface.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                CordovaSamsungAccessoryPluginAction cordovaPluginAction = CordovaSamsungAccessoryPluginAction.fromValue(action);
                switch (cordovaPluginAction) {
                    case INIT:
                        init(callbackContext);
                        break;
                    case SHUTDOWN:
                        shutdown(callbackContext);
                        break;
                    case FIND_PEER:
                        findPeer(callbackContext);
                        break;
                    case SEND_MESSAGE:
                        sendMessage(args, callbackContext);
                        break;
                    case REGISTER_MESSAGE_LISTENER:
                        registerMessageListener(callbackContext);
                        break;
                }
            }
        });
        return true;
    }

    protected void init(CallbackContext callbackContext){
        Log.i(TAG,"init");
        if (this.intent == null || this.serviceConnection == null){
            Activity context = this.cordovaInterface.getActivity();
            this.intent = new Intent(context, SamsungAccessoryAgent.class);
            this.intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            // Start service then bind; so onCreate is invoked on the 'Service' class
            context.startService(this.intent);
            this.serviceConnection = new SamsungAccessoryServiceConnection();
            context.bindService(this.intent, this.serviceConnection, Context.BIND_AUTO_CREATE);
            callbackContext.success(TAG + "::Success: Accessory SDK initialised");
        } else {
            callbackContext.error(TAG+"::Error: Accessory SDK already initialised");
        }
    }

    protected void shutdown(CallbackContext callbackContext) {
        Log.i(TAG,"shutdown");
        if (this.intent != null) {
            Activity context = this.cordovaInterface.getActivity();
            context.stopService(this.intent);
            callbackContext.success(TAG + "::Success: Accessory SDK Stopped");
        }else{
            callbackContext.error(TAG+"::Error: Accessory SDK does not seem to be running");
        }
    }

    protected void registerMessageListener(CallbackContext callbackContext) {
        Log.i(TAG,"registerMessageListener :: listener: "+callbackContext);
        if (this.serviceConnection != null && this.serviceConnection.getService() != null) {
            SamsungAccessoryMessageListener listener = createSamsungMessageListener(callbackContext);
            this.serviceConnection.getService().registerMessageListener(listener);
            // No result is sent back initially, the actual results are sent in the listener
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        } else {
            callbackContext.error(TAG + "::Error: Accessory SDK not ready, call init or try again");
        }
    }

    protected void sendMessage(JSONArray args, CallbackContext callbackContext) {
        try {
            Log.i(TAG, "sendMessage :: args: " + args);
            if (this.serviceConnection != null && this.serviceConnection.getService() != null) {
                String msg = args.getString(0);
                this.serviceConnection.getService().sendMessage(msg);
                callbackContext.success(TAG+"::Success: Message Sent");
            } else {
                callbackContext.error(TAG+"::Error: Service not ready, call init or try again");
            }
        } catch (Exception ex) {
            callbackContext.error(TAG+"::Error: Not able to send message: "+ex.getMessage());
        }
    }

    protected void findPeer(CallbackContext callbackContext) {
        try {
            Log.i(TAG, "findPeers");
            if (this.serviceConnection != null && this.serviceConnection.getService() != null) {
                this.serviceConnection.getService().findPeers();
                callbackContext.success(TAG+"::Success: Message Sent");
            } else {
                callbackContext.error(TAG+"::Error: Service not ready, call init or try again");
            }
            callbackContext.success(TAG+"::Success: Message Sent");
        } catch (Exception ex) {
            callbackContext.error(TAG+"::Error: Not able to send message: "+ex.getMessage());
        }
    }

    private SamsungAccessoryMessageListener createSamsungMessageListener(CallbackContext callbackContext){
        Log.i(TAG,"createSamsungMessageListener");
        this.callbackContext = callbackContext;
        SamsungAccessoryMessageListener listener = new SamsungAccessoryMessageListener() {
            @Override
            public void messageReceived(String msg) {
                Log.i(TAG,"listener :: messageReceived: "+msg);
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, msg);
                pluginResult.setKeepCallback(true);
                CordovaSamsungAccessoryPlugin.this.callbackContext.sendPluginResult(pluginResult);
            }
        };
        return listener;
    }

}
